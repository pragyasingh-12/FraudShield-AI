package com.fraudshield.service;

import com.fraudshield.dao.BehaviorProfileDAO;
import com.fraudshield.dao.TransactionDAO;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.model.Transaction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Builds a customer's behavioural baseline from their APPROVED transaction history. */
public class BehaviorProfileService {
    private static final int HISTORY_LIMIT = 300;
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final BehaviorProfileDAO profileDAO = new BehaviorProfileDAO();

    /** Builds the profile from approved transactions with id &lt; beforeId (in memory; not saved). */
    public BehaviorProfile build(int userId, int beforeId) throws DatabaseOperationException {
        List<Transaction> history = transactionDAO.findApprovedHistory(userId, beforeId, HISTORY_LIMIT);
        BehaviorProfile p = new BehaviorProfile();
        p.setUserId(userId);
        int n = history.size();
        p.setTotalTransactions(n);
        if (n == 0) return p;

        double sum = 0, max = 0;
        List<Integer> hours = new ArrayList<>();
        Map<String, Integer> locationCounts = new HashMap<>();   // Map<String,Integer>: how often each location was used
        Set<LocalDate> activeDays = new HashSet<>();             // Set: distinct days with activity
        for (Transaction t : history) {
            sum += t.getAmount();
            max = Math.max(max, t.getAmount());
            hours.add(t.getHour());
            locationCounts.merge(t.getLocation(), 1, Integer::sum);
            activeDays.add(t.getTransactionTime().toLocalDate());
            p.getKnownLocations().add(norm(t.getLocation()));
            p.getKnownReceivers().add(norm(t.getReceiver()));
            p.getTypeCounts().merge(t.getTransactionType(), 1, Integer::sum);
        }
        double avg = sum / n;
        double variance = 0;
        for (Transaction t : history) variance += Math.pow(t.getAmount() - avg, 2);

        Collections.sort(hours);
        p.setAvgAmount(avg);
        p.setStdDevAmount(Math.sqrt(variance / n));
        p.setMaxAmount(max);
        p.setUsualStartHour(hours.get((int) Math.floor(0.05 * (n - 1))));     // 5th percentile hour
        p.setUsualEndHour(hours.get((int) Math.ceil(0.95 * (n - 1))));        // 95th percentile hour
        p.setUsualLocation(Collections.max(locationCounts.entrySet(), Map.Entry.comparingByValue()).getKey());
        p.setTransactionFrequency(n / (double) Math.max(1, activeDays.size()));
        return p;
    }

    /** Persists a fresh snapshot of the profile (called after a transaction is approved). */
    public void refreshSnapshot(int userId) throws DatabaseOperationException {
        BehaviorProfile p = build(userId, Integer.MAX_VALUE);
        if (p.getTotalTransactions() > 0) profileDAO.save(p);
    }

    public Optional<BehaviorProfile> getSnapshot(int userId) throws DatabaseOperationException {
        return profileDAO.findByUserId(userId);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
