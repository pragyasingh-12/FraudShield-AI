package com.fraudshield.model;

import java.time.LocalDate;

/** Search / filter criteria for the transaction history page (all fields optional). */
public class TransactionFilter {
    private String query;
    private RiskLevel riskLevel;
    private TransactionStatus status;
    private String type;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Double minAmount;
    private Double maxAmount;
    private int page = 1;
    private int pageSize = 15;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public Double getMinAmount() { return minAmount; }
    public void setMinAmount(Double minAmount) { this.minAmount = minAmount; }
    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = Math.max(1, Math.min(100, pageSize)); }
    public int getOffset() { return (page - 1) * pageSize; }
}
