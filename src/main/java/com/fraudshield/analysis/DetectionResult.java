package com.fraudshield.analysis;

import com.fraudshield.model.RiskFactor;
import java.util.ArrayList;
import java.util.List;

/** Output of a single FraudDetector: a 0-100 score, its factors (if any) and a short summary. */
public class DetectionResult {
    private final int score;
    private final List<RiskFactor> factors;
    private final String methodName;
    private final String summary;

    public DetectionResult(int score, List<RiskFactor> factors, String methodName, String summary) {
        this.score = Math.max(0, Math.min(100, score));
        this.factors = new ArrayList<>(factors);
        this.methodName = methodName;
        this.summary = summary;
    }

    public int getScore() { return score; }
    public List<RiskFactor> getFactors() { return factors; }
    public String getMethodName() { return methodName; }
    public String getSummary() { return summary; }
}
