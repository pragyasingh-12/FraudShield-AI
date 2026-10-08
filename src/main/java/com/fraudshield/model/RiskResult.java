package com.fraudshield.model;

import com.fraudshield.util.DateUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Full, explainable outcome of analysing one transaction: final score, level,
 * the individual risk factors, a human readable explanation, the method used and a timestamp.
 */
public class RiskResult {
    private int transactionId;
    private int finalScore;
    private RiskLevel riskLevel;
    private final List<RiskFactor> factors = new ArrayList<>();
    private String explanation = "";
    private String analysisMethod = "";
    private LocalDateTime analyzedAt = LocalDateTime.now();
    private int ruleScore;
    private Integer mlScore;          // null when the ML model was unavailable
    private String mlSummary = "";

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) {
        this.finalScore = Math.max(0, Math.min(100, finalScore));
        this.riskLevel = RiskLevel.fromScore(this.finalScore);
    }

    public RiskLevel getRiskLevel() { return riskLevel; }

    public List<RiskFactor> getFactors() { return Collections.unmodifiableList(factors); }
    public void addFactor(RiskFactor factor) { factors.add(factor); }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getAnalysisMethod() { return analysisMethod; }
    public void setAnalysisMethod(String analysisMethod) { this.analysisMethod = analysisMethod; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    public String getAnalyzedAtFormatted() { return DateUtil.format(analyzedAt); }

    public int getRuleScore() { return ruleScore; }
    public void setRuleScore(int ruleScore) { this.ruleScore = ruleScore; }

    public Integer getMlScore() { return mlScore; }
    public void setMlScore(Integer mlScore) { this.mlScore = mlScore; }

    public String getMlSummary() { return mlSummary; }
    public void setMlSummary(String mlSummary) { this.mlSummary = mlSummary; }

    /** Only the factors that actually raised suspicion (the "detection factors" list). */
    public List<RiskFactor> getTriggeredFactors() {
        List<RiskFactor> out = new ArrayList<>();
        for (RiskFactor f : factors) {
            if (f.isTriggered()) out.add(f);
        }
        return out;
    }

    /** Score (0-100) of the factor with the given code, or 0 if not present. */
    public int scoreOf(String code) {
        for (RiskFactor f : factors) {
            if (f.getCode().equals(code)) return f.getScore();
        }
        return 0;
    }

    public boolean isMlUsed() {
        return mlScore != null;
    }
}
