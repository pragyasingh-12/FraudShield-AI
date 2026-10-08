package com.fraudshield.service;

import com.fraudshield.analysis.AnalysisContext;
import com.fraudshield.analysis.RiskAnalyzer;
import com.fraudshield.dao.DeviceDAO;
import com.fraudshield.dao.FraudAlertDAO;
import com.fraudshield.dao.RiskAnalysisDAO;
import com.fraudshield.dao.TransactionDAO;
import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.model.AlertStatus;
import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.model.Device;
import com.fraudshield.model.FraudAlert;
import com.fraudshield.model.RiskFactor;
import com.fraudshield.model.RiskLevel;
import com.fraudshield.model.RiskResult;
import com.fraudshield.model.Transaction;
import com.fraudshield.model.TransactionStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates the fraud-detection workflow for one transaction:
 * build context (behaviour profile, devices, velocity) -> risk analyzer -> save analysis, status and alert
 * in ONE JDBC transaction -> update device trust and behaviour profile -> refresh dashboard.
 *
 * Concurrency design:
 *  - analysis runs on a thread pool (analyzeAsync);
 *  - analyses of the SAME customer are serialised with a per-user lock, because each result depends on the
 *    customer's earlier transactions (velocity, profile); different customers run in parallel;
 *  - suspiciousDevices is a concurrent set shared by all worker threads.
 */
public class FraudDetectionService {
    private static final Logger LOG = Logger.getLogger(FraudDetectionService.class.getName());

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final FraudAlertDAO alertDAO = new FraudAlertDAO();
    private final RiskAnalysisDAO riskDAO = new RiskAnalysisDAO();
    private final BehaviorProfileService profileService;
    private final RiskAnalyzer riskAnalyzer;
    private final AppExecutors executors;
    private final DashboardService dashboardService;

    private final Map<Integer, Object> userLocks = new ConcurrentHashMap<>();
    private final Set<String> suspiciousDevices = ConcurrentHashMap.newKeySet();
    private final AtomicInteger analysedCount = new AtomicInteger();

    public FraudDetectionService(BehaviorProfileService profileService, RiskAnalyzer riskAnalyzer,
                                 AppExecutors executors, DashboardService dashboardService) {
        this.profileService = profileService;
        this.riskAnalyzer = riskAnalyzer;
        this.executors = executors;
        this.dashboardService = dashboardService;
    }

    /** Loads the set of devices involved in blocked transactions (call once at start-up). */
    public void loadSuspiciousDevices() throws DatabaseOperationException {
        suspiciousDevices.addAll(deviceDAO.findSuspiciousDeviceIds());
    }

    public int getAnalysedCount() { return analysedCount.get(); }

    public Set<String> getSuspiciousDevices() { return new HashSet<>(suspiciousDevices); }

    // ------------------------------------------------------------ analysis

    /** Queues the analysis on the worker pool; the transaction stays PENDING until it finishes. */
    public Future<RiskResult> analyzeAsync(final int transactionId) {
        Callable<RiskResult> task = () -> {
            try {
                return analyze(transactionId);
            } catch (DatabaseOperationException | FraudAnalysisException | RuntimeException ex) {
                // Nobody calls Future.get() for fire-and-forget analyses, so log here or the failure is invisible.
                LOG.log(Level.SEVERE, "Asynchronous analysis of transaction " + transactionId
                        + " failed; it stays PENDING and is retried at the next start-up", ex);
                throw ex;
            }
        };
        return executors.analysisPool().submit(task);
    }

    /** Analyses and persists the result of a stored transaction (blocking). Idempotent. */
    public RiskResult analyze(int transactionId) throws DatabaseOperationException, FraudAnalysisException {
        Transaction probe = transactionDAO.findById(transactionId)
                .orElseThrow(() -> new FraudAnalysisException("Transaction not found: " + transactionId));
        Object lock = userLocks.computeIfAbsent(probe.getUserId(), k -> new Object());
        synchronized (lock) {
            Transaction t = transactionDAO.findById(transactionId)
                    .orElseThrow(() -> new FraudAnalysisException("Transaction not found: " + transactionId));
            if (t.getStatus() != TransactionStatus.PENDING) {
                Optional<RiskResult> existing = riskDAO.findByTransactionId(transactionId);
                if (existing.isPresent()) return existing.get();
            }
            AnalysisContext ctx = buildContext(t, t.getId());
            RiskResult result = riskAnalyzer.assess(ctx);
            result.setTransactionId(t.getId());
            persist(t, result);
            afterPersist(t, result);
            analysedCount.incrementAndGet();
            return result;
        }
    }

    /** What-if analysis: nothing is written to the database. */
    public RiskResult simulate(Transaction t) throws DatabaseOperationException, FraudAnalysisException {
        AnalysisContext ctx = buildContext(t, Integer.MAX_VALUE);
        return riskAnalyzer.assess(ctx);
    }

    /** Analyses everything still PENDING in time order (start-up recovery and demo data). */
    public int analyzePending() throws DatabaseOperationException {
        int done = 0;
        for (Transaction t : transactionDAO.findPending()) {
            try {
                analyze(t.getId());
                done++;
            } catch (FraudAnalysisException ex) {
                LOG.log(Level.WARNING, "Skipped {0}: {1}", new Object[]{t.getReference(), ex.getMessage()});
            }
        }
        return done;
    }

    private AnalysisContext buildContext(Transaction t, int beforeId) throws DatabaseOperationException {
        BehaviorProfile profile = profileService.build(t.getUserId(), beforeId);
        Set<String> known = deviceDAO.findDeviceIdsByUser(t.getUserId());
        LocalDateTime when = t.getTransactionTime();
        int last10 = transactionDAO.countInWindow(t.getUserId(), beforeId, when.minusMinutes(10), when);
        int lastHour = transactionDAO.countInWindow(t.getUserId(), beforeId, when.minusHours(1), when);
        int today = transactionDAO.countInWindow(t.getUserId(), beforeId, when.toLocalDate().atStartOfDay(), when);
        return new AnalysisContext(t, profile, known, new HashSet<>(suspiciousDevices), last10, lastHour, today);
    }

    /** Saves analysis + status + alert atomically (all or nothing). */
    private void persist(Transaction t, RiskResult result) throws DatabaseOperationException {
        TransactionStatus status = statusFor(result.getRiskLevel());
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                riskDAO.save(c, result);
                transactionDAO.updateStatusAndScore(c, t.getId(), status, result.getFinalScore());
                if (result.getRiskLevel() != RiskLevel.LOW && !alertExists(t.getId())) {
                    FraudAlert alert = new FraudAlert();
                    alert.setTransactionId(t.getId());
                    alert.setRiskLevel(result.getRiskLevel());
                    alert.setReason(alertReason(result));
                    alertDAO.create(c, alert);
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not store analysis for " + t.getReference() + ": " + ex.getMessage(), ex);
        }
        t.setStatus(status);
        t.setRiskScore(result.getFinalScore());
    }

    private boolean alertExists(int transactionId) throws DatabaseOperationException {
        return alertDAO.findByTransactionId(transactionId).isPresent();
    }

    private void afterPersist(Transaction t, RiskResult result) {
        try {
            if (t.getStatus() == TransactionStatus.APPROVED) {
                deviceDAO.create(new Device(t.getUserId(), t.getDeviceId(), "MOBILE"));   // device becomes trusted
                profileService.refreshSnapshot(t.getUserId());
            } else if (t.getStatus() == TransactionStatus.BLOCKED) {
                suspiciousDevices.add(t.getDeviceId());
            }
            dashboardService.refresh();
        } catch (DatabaseOperationException ex) {
            LOG.log(Level.WARNING, "Post-analysis update failed for {0}: {1}", new Object[]{t.getReference(), ex.getMessage()});
        }
    }

    static TransactionStatus statusFor(RiskLevel level) {
        return switch (level) {
            case LOW, MEDIUM -> TransactionStatus.APPROVED;
            case HIGH -> TransactionStatus.REVIEW;
            case CRITICAL -> TransactionStatus.BLOCKED;
        };
    }

    private String alertReason(RiskResult r) {
        List<String> parts = new ArrayList<>();
        for (RiskFactor f : r.getTriggeredFactors()) parts.add(f.getDescription());
        if (r.isMlUsed() && r.getMlScore() >= 60) parts.add(r.getMlSummary());
        return parts.isEmpty() ? "Combined low-level signals raised the score" : String.join("; ", parts);
    }

    // ---------------------------------------------------------- alert handling

    /**
     * Analyst decision. FALSE_POSITIVE releases the transaction (APPROVED); CONFIRMED_FRAUD blocks it and
     * marks its device suspicious.
     */
    public void resolveAlert(int alertId, AlertStatus newStatus) throws DatabaseOperationException {
        FraudAlert alert = alertDAO.findById(alertId)
                .orElseThrow(() -> new DatabaseOperationException("Alert not found: " + alertId, null));
        alertDAO.updateStatus(alertId, newStatus);
        Optional<Transaction> txn = transactionDAO.findById(alert.getTransactionId());
        if (txn.isPresent()) {
            if (newStatus == AlertStatus.FALSE_POSITIVE) {
                transactionDAO.updateStatus(txn.get().getId(), TransactionStatus.APPROVED);
            } else if (newStatus == AlertStatus.CONFIRMED_FRAUD) {
                transactionDAO.updateStatus(txn.get().getId(), TransactionStatus.BLOCKED);
                suspiciousDevices.add(txn.get().getDeviceId());
            }
        }
        dashboardService.refresh();
    }
}
