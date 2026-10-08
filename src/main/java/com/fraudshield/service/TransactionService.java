package com.fraudshield.service;

import com.fraudshield.dao.TransactionDAO;
import com.fraudshield.dao.UserDAO;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.exception.InvalidTransactionException;
import com.fraudshield.model.PageResult;
import com.fraudshield.model.RiskResult;
import com.fraudshield.model.Transaction;
import com.fraudshield.model.TransactionFilter;
import com.fraudshield.model.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/** Validation, storage and submission of transactions. */
public class TransactionService {
    public static final List<String> TYPES = Collections.unmodifiableList(Arrays.asList("UPI", "CARD", "NETBANKING", "WALLET"));
    public static final double MAX_AMOUNT = 1_000_000;
    private static final Pattern UPI_ID = Pattern.compile("^[A-Za-z0-9._-]{2,50}@[A-Za-z0-9]{2,30}$");
    private static final Pattern DEVICE_ID = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final FraudDetectionService detectionService;
    private final AppExecutors executors;

    public TransactionService(FraudDetectionService detectionService, AppExecutors executors) {
        this.detectionService = detectionService;
        this.executors = executors;
    }

    /** Throws InvalidTransactionException describing the first problem found. */
    public void validate(Transaction t) throws InvalidTransactionException, DatabaseOperationException {
        if (t.getAmount() <= 0) throw new InvalidTransactionException("Amount must be greater than zero.");
        if (t.getAmount() > MAX_AMOUNT) throw new InvalidTransactionException("Amount exceeds the simulator limit of Rs 10,00,000.");
        if (!TYPES.contains(t.getTransactionType())) throw new InvalidTransactionException("Unknown transaction type.");
        String receiver = t.getReceiver() == null ? "" : t.getReceiver().trim();
        if (receiver.isEmpty()) throw new InvalidTransactionException("Receiver is required.");
        if (receiver.length() > 100) throw new InvalidTransactionException("Receiver is too long (max 100 characters).");
        if ("UPI".equals(t.getTransactionType()) && !UPI_ID.matcher(receiver).matches()) {
            throw new InvalidTransactionException("UPI receiver must look like name@bank (e.g. shop@okaxis).");
        }
        if (t.getDeviceId() == null || !DEVICE_ID.matcher(t.getDeviceId()).matches()) {
            throw new InvalidTransactionException("Device ID must be 3-64 letters, digits, '-' or '_'.");
        }
        if (t.getLocation() == null || t.getLocation().isBlank() || t.getLocation().length() > 60) {
            throw new InvalidTransactionException("Location is required (max 60 characters).");
        }
        if (t.getTransactionTime() == null) throw new InvalidTransactionException("Transaction date/time is invalid.");
        if (t.getTransactionTime().isAfter(LocalDateTime.now().plusDays(1))) {
            throw new InvalidTransactionException("Transaction time cannot be in the future.");
        }
        Optional<User> owner = userDAO.findById(t.getUserId());
        if (owner.isEmpty() || !User.ROLE_CUSTOMER.equals(owner.get().getRole())) {
            throw new InvalidTransactionException("Select a valid customer account.");
        }
        t.setReceiver(receiver);
        t.setLocation(t.getLocation().trim());
    }

    /** Validates, stores as PENDING and queues the analysis on a worker thread. Returns immediately. */
    public Transaction submit(Transaction t) throws InvalidTransactionException, DatabaseOperationException {
        validate(t);
        transactionDAO.create(t);
        detectionService.analyzeAsync(t.getId());
        return t;
    }

    /** Dry run used by the Transaction Analysis page - validates and scores without saving. */
    public RiskResult analyzeOnly(Transaction t)
            throws InvalidTransactionException, DatabaseOperationException, FraudAnalysisException {
        validate(t);
        return detectionService.simulate(t);
    }

    /**
     * Concurrent processing demo: stores {@code count} rapid payments (20 seconds apart, same device and
     * receiver) in order, then analyses them in parallel on the worker pool and waits for all of them.
     */
    public List<RiskResult> submitBurst(Transaction template, int count)
            throws InvalidTransactionException, DatabaseOperationException {
        int n = Math.max(2, Math.min(count, 8));
        validate(template);
        List<Transaction> stored = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Transaction t = new Transaction(template.getUserId(), template.getAmount(), template.getTransactionType(),
                    template.getReceiver(), template.getTransactionTime().plusSeconds(20L * i),
                    template.getDeviceId(), template.getLocation());
            transactionDAO.create(t);
            stored.add(t);
        }
        List<Callable<RiskResult>> tasks = new ArrayList<>();
        for (Transaction t : stored) {
            final int id = t.getId();
            tasks.add(() -> detectionService.analyze(id));
        }
        List<RiskResult> results = new ArrayList<>();
        try {
            for (Future<RiskResult> f : executors.analysisPool().invokeAll(tasks)) {
                results.add(f.get());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof DatabaseOperationException) throw (DatabaseOperationException) cause;
            throw new DatabaseOperationException("Burst analysis failed: " + cause.getMessage(), cause);
        }
        return results;
    }

    public Optional<Transaction> getById(int id) throws DatabaseOperationException {
        return transactionDAO.findById(id);
    }

    public PageResult<Transaction> search(TransactionFilter filter) throws DatabaseOperationException {
        int total = transactionDAO.countSearch(filter);
        List<Transaction> items = transactionDAO.search(filter);
        return new PageResult<>(items, total, filter.getPage(), filter.getPageSize());
    }

    public boolean delete(int id) throws DatabaseOperationException {
        return transactionDAO.delete(id);
    }
}
