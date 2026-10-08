package com.fraudshield.model;

import com.fraudshield.util.DateUtil;
import java.time.LocalDateTime;

/**
 * A simulated UPI / digital payment. The public reference shown to users
 * (e.g. TXN1024) is derived from the database id: reference = "TXN" + (1000 + id).
 */
public class Transaction {
    public static final int REFERENCE_OFFSET = 1000;

    private int id;
    private int userId;
    private String userName;          // filled by JOIN queries, for display only
    private double amount;
    private String transactionType = "UPI";
    private String receiver;
    private LocalDateTime transactionTime;
    private String deviceId;
    private String location;
    private TransactionStatus status = TransactionStatus.PENDING;
    private Integer riskScore;        // null until the analysis has finished
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(int userId, double amount, String transactionType, String receiver,
                       LocalDateTime transactionTime, String deviceId, String location) {
        this.userId = userId;
        this.amount = amount;
        if (transactionType != null && !transactionType.isBlank()) this.transactionType = transactionType;
        this.receiver = receiver;
        this.transactionTime = transactionTime;
        this.deviceId = deviceId;
        this.location = location;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Public reference such as TXN1024. */
    public String getReference() {
        return "TXN" + (REFERENCE_OFFSET + id);
    }

    /** Converts "TXN1024" (or a plain number) back to a database id; returns -1 if unparsable. */
    public static int idFromReference(String ref) {
        if (ref == null) return -1;
        String s = ref.trim().toUpperCase();
        try {
            if (s.startsWith("TXN")) return Integer.parseInt(s.substring(3)) - REFERENCE_OFFSET;
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public int getHour() {
        return transactionTime == null ? 0 : transactionTime.getHour();
    }

    /** Null while the transaction is still PENDING analysis. */
    public RiskLevel getRiskLevel() {
        return riskScore == null ? null : RiskLevel.fromScore(riskScore);
    }

    public String getTransactionTimeFormatted() {
        return DateUtil.format(transactionTime);
    }

    @Override
    public String toString() {
        return "Transaction{" + getReference() + ", amount=" + amount + ", status=" + status + "}";
    }
}
