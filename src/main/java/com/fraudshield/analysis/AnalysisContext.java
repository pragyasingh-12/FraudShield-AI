package com.fraudshield.analysis;

import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.model.Transaction;
import java.util.Locale;
import java.util.Set;

/**
 * Everything a detector needs to judge one transaction: the transaction, the customer's
 * behaviour profile, known/suspicious devices and recent-activity counters.
 * Also exposes the derived features shared by the rule engine and the ML model.
 */
public class AnalysisContext {
    private final Transaction transaction;
    private final BehaviorProfile profile;
    private final Set<String> knownDevices;
    private final Set<String> suspiciousDevices;
    private final int txnsLast10Min;
    private final int txnsLastHour;
    private final int txnsToday;

    public AnalysisContext(Transaction transaction, BehaviorProfile profile, Set<String> knownDevices,
                           Set<String> suspiciousDevices, int txnsLast10Min, int txnsLastHour, int txnsToday) {
        this.transaction = transaction;
        this.profile = profile;
        this.knownDevices = knownDevices;
        this.suspiciousDevices = suspiciousDevices;
        this.txnsLast10Min = txnsLast10Min;
        this.txnsLastHour = txnsLastHour;
        this.txnsToday = txnsToday;
    }

    public Transaction getTransaction() { return transaction; }
    public BehaviorProfile getProfile() { return profile; }
    public Set<String> getKnownDevices() { return knownDevices; }
    public Set<String> getSuspiciousDevices() { return suspiciousDevices; }
    public int getTxnsLast10Min() { return txnsLast10Min; }
    public int getTxnsLastHour() { return txnsLastHour; }
    public int getTxnsToday() { return txnsToday; }

    public static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasHistory() {
        return profile != null && profile.hasEnoughHistory();
    }

    public boolean isNewDevice() {
        return !knownDevices.contains(transaction.getDeviceId());
    }

    public boolean isSuspiciousDevice() {
        return suspiciousDevices.contains(transaction.getDeviceId());
    }

    /** True when the location differs from the customer's usual location (only judged with history). */
    public boolean isLocationChanged() {
        return hasHistory() && !norm(transaction.getLocation()).equals(norm(profile.getUsualLocation()));
    }

    public boolean isLocationEverSeen() {
        return profile != null && profile.getKnownLocations().contains(norm(transaction.getLocation()));
    }

    /** amount / customer's average amount; 1.0 when there is not enough history. */
    public double amountRatio() {
        if (!hasHistory() || profile.getAvgAmount() <= 0) return 1.0;
        return transaction.getAmount() / profile.getAvgAmount();
    }

    /**
     * Behaviour deviation 0-100: new receiver (+50), amount well above the largest amount seen before (+30),
     * rarely-used payment type (+20). Used as the BEHAVIOR factor and as an ML feature.
     */
    public int behaviorDeviation() {
        if (!hasHistory()) return 0;
        int score = 0;
        if (!profile.getKnownReceivers().contains(norm(transaction.getReceiver()))) score += 50;
        if (profile.getMaxAmount() > 0 && transaction.getAmount() > 1.5 * profile.getMaxAmount()) score += 30;
        int typeCount = profile.getTypeCounts().getOrDefault(transaction.getTransactionType(), 0);
        if (typeCount * 10 < profile.getTotalTransactions()) score += 20;
        return Math.min(100, score);
    }
}
