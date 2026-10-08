package com.fraudshield.model;

import com.fraudshield.util.DateUtil;
import java.time.LocalDateTime;

/** An alert raised for a MEDIUM, HIGH or CRITICAL transaction. */
public class FraudAlert {
    private int id;
    private int transactionId;
    private RiskLevel riskLevel;
    private String reason;
    private AlertStatus alertStatus = AlertStatus.OPEN;
    private LocalDateTime createdAt;

    // display fields filled by JOIN queries
    private String userName;
    private double amount;
    private String receiver;
    private Integer riskScore;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AlertStatus getAlertStatus() { return alertStatus; }
    public void setAlertStatus(AlertStatus alertStatus) { this.alertStatus = alertStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getTransactionReference() {
        return "TXN" + (Transaction.REFERENCE_OFFSET + transactionId);
    }

    public String getCreatedAtFormatted() {
        return DateUtil.format(createdAt);
    }
}
