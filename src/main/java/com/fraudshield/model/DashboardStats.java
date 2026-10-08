package com.fraudshield.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only snapshot of the numbers shown on the dashboard (computed in the background and cached). */
public class DashboardStats {
    private int totalTransactions;
    private int suspiciousTransactions;   // MEDIUM and above
    private int highRiskTransactions;     // HIGH
    private int criticalTransactions;     // CRITICAL
    private int pendingTransactions;
    private int openAlerts;
    private double averageRiskScore;
    private Map<String, Integer> levelCounts = new LinkedHashMap<>();
    private List<FraudAlert> recentAlerts;
    private List<Transaction> recentTransactions;
    private long computedAtMillis = System.currentTimeMillis();

    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int v) { this.totalTransactions = v; }
    public int getSuspiciousTransactions() { return suspiciousTransactions; }
    public void setSuspiciousTransactions(int v) { this.suspiciousTransactions = v; }
    public int getHighRiskTransactions() { return highRiskTransactions; }
    public void setHighRiskTransactions(int v) { this.highRiskTransactions = v; }
    public int getCriticalTransactions() { return criticalTransactions; }
    public void setCriticalTransactions(int v) { this.criticalTransactions = v; }
    public int getPendingTransactions() { return pendingTransactions; }
    public void setPendingTransactions(int v) { this.pendingTransactions = v; }
    public int getOpenAlerts() { return openAlerts; }
    public void setOpenAlerts(int v) { this.openAlerts = v; }
    public double getAverageRiskScore() { return averageRiskScore; }
    public void setAverageRiskScore(double v) { this.averageRiskScore = v; }
    public Map<String, Integer> getLevelCounts() { return levelCounts; }
    public void setLevelCounts(Map<String, Integer> v) { this.levelCounts = v; }
    public List<FraudAlert> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<FraudAlert> v) { this.recentAlerts = v; }
    public List<Transaction> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<Transaction> v) { this.recentTransactions = v; }
    public long getComputedAtMillis() { return computedAtMillis; }
}
