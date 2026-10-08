package com.fraudshield.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Summary of a customer's normal behaviour, built from their APPROVED history.
 * The persisted columns mirror the behavior_profiles table; the collections are
 * rebuilt in memory on every analysis (Set / Map usage).
 */
public class BehaviorProfile {
    public static final int MIN_HISTORY = 5;

    private int id;
    private int userId;
    private double avgAmount;
    private double stdDevAmount;
    private double maxAmount;
    private int usualStartHour = 8;
    private int usualEndHour = 22;
    private String usualLocation = "";
    private double transactionFrequency;   // average transactions per active day
    private int totalTransactions;
    private LocalDateTime updatedAt;

    // not persisted - derived from history for each analysis
    private final Set<String> knownLocations = new HashSet<>();
    private final Set<String> knownReceivers = new HashSet<>();
    private final Map<String, Integer> typeCounts = new HashMap<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public double getAvgAmount() { return avgAmount; }
    public void setAvgAmount(double avgAmount) { this.avgAmount = avgAmount; }
    public double getStdDevAmount() { return stdDevAmount; }
    public void setStdDevAmount(double stdDevAmount) { this.stdDevAmount = stdDevAmount; }
    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }
    public int getUsualStartHour() { return usualStartHour; }
    public void setUsualStartHour(int usualStartHour) { this.usualStartHour = usualStartHour; }
    public int getUsualEndHour() { return usualEndHour; }
    public void setUsualEndHour(int usualEndHour) { this.usualEndHour = usualEndHour; }
    public String getUsualLocation() { return usualLocation; }
    public void setUsualLocation(String usualLocation) { this.usualLocation = usualLocation == null ? "" : usualLocation; }
    public double getTransactionFrequency() { return transactionFrequency; }
    public void setTransactionFrequency(double transactionFrequency) { this.transactionFrequency = transactionFrequency; }
    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<String> getKnownLocations() { return knownLocations; }
    public Set<String> getKnownReceivers() { return knownReceivers; }
    public Map<String, Integer> getTypeCounts() { return typeCounts; }

    /** True when there is enough approved history to make behavioural comparisons meaningful. */
    public boolean hasEnoughHistory() {
        return totalTransactions >= MIN_HISTORY;
    }

    public String getUpdatedAtFormatted() {
        return com.fraudshield.util.DateUtil.format(updatedAt);
    }
}
