package com.fraudshield.service;

import com.fraudshield.dao.FraudAlertDAO;
import com.fraudshield.dao.TransactionDAO;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.AlertStatus;
import com.fraudshield.model.DashboardStats;
import com.fraudshield.util.AppConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dashboard numbers. A background scheduled task recomputes them every N seconds and every finished
 * analysis triggers an immediate refresh; the page reads the cached snapshot (volatile field).
 */
public class DashboardService {
    private static final Logger LOG = Logger.getLogger(DashboardService.class.getName());
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final FraudAlertDAO alertDAO = new FraudAlertDAO();
    private volatile DashboardStats cached;

    public void startBackgroundRefresh(AppExecutors executors) {
        int seconds = Math.max(5, AppConfig.getInt("analytics.refresh.seconds", 30));
        executors.scheduler().scheduleWithFixedDelay(() -> {
            try {
                refresh();
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Background analytics refresh failed: {0}", ex.getMessage());
            }
        }, seconds, seconds, TimeUnit.SECONDS);
    }

    public DashboardStats getStats() throws DatabaseOperationException {
        DashboardStats s = cached;
        return s != null ? s : refresh();
    }

    /** Recomputes the snapshot. synchronized so two threads never compute and publish at the same time. */
    public synchronized DashboardStats refresh() throws DatabaseOperationException {
        Map<String, Integer> counts = transactionDAO.getRiskCounts();
        DashboardStats s = new DashboardStats();
        s.setTotalTransactions(counts.getOrDefault("TOTAL", 0));
        s.setPendingTransactions(counts.getOrDefault("PENDING", 0));
        s.setHighRiskTransactions(counts.getOrDefault("HIGH", 0));
        s.setCriticalTransactions(counts.getOrDefault("CRITICAL", 0));
        s.setSuspiciousTransactions(counts.getOrDefault("MEDIUM", 0) + s.getHighRiskTransactions() + s.getCriticalTransactions());
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (String k : new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"}) levels.put(k, counts.getOrDefault(k, 0));
        s.setLevelCounts(levels);
        s.setAverageRiskScore(transactionDAO.getAverageRiskScore());
        s.setOpenAlerts(alertDAO.countByStatus(AlertStatus.OPEN));
        s.setRecentAlerts(alertDAO.findRecent(5));
        s.setRecentTransactions(transactionDAO.findRecent(8));
        cached = s;
        return s;
    }
}
