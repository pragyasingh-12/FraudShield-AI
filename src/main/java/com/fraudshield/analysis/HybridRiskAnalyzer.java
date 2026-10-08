package com.fraudshield.analysis;

import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.model.RiskFactor;
import com.fraudshield.model.RiskResult;
import com.fraudshield.util.AppConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Blends the rule/behaviour score and the ML probability:
 * final = 0.75 * ruleScore + 0.25 * mlScore (weights configurable). If the ML model is not
 * available the rule score is used alone and the result says so.
 */
public class HybridRiskAnalyzer implements RiskAnalyzer {
    private final FraudDetector ruleDetector;      // interface references -> polymorphism
    private final FraudDetector mlDetector;
    private final double ruleWeight;
    private final double mlWeight;

    public HybridRiskAnalyzer(FraudDetector ruleDetector, FraudDetector mlDetector) {
        this.ruleDetector = ruleDetector;
        this.mlDetector = mlDetector;
        double rw = AppConfig.getDouble("risk.rule.weight", 0.75);
        double mw = AppConfig.getDouble("risk.ml.weight", 0.25);
        double sum = rw + mw;
        this.ruleWeight = sum <= 0 ? 0.75 : rw / sum;
        this.mlWeight = sum <= 0 ? 0.25 : mw / sum;
    }

    @Override
    public RiskResult assess(AnalysisContext ctx) throws FraudAnalysisException {
        DetectionResult rule = ruleDetector.analyze(ctx);

        DetectionResult ml = null;
        try {
            ml = mlDetector.analyze(ctx);
        } catch (FraudAnalysisException ex) {
            // ML is optional: fall back to rules only, but remember why
        }

        RiskResult result = new RiskResult();
        result.setTransactionId(ctx.getTransaction().getId());
        for (RiskFactor f : rule.getFactors()) result.addFactor(f);
        result.setRuleScore(rule.getScore());

        int finalScore;
        if (ml != null) {
            result.setMlScore(ml.getScore());
            result.setMlSummary(ml.getSummary());
            finalScore = (int) Math.round(ruleWeight * rule.getScore() + mlWeight * ml.getScore());
            result.setAnalysisMethod("Hybrid: " + ruleDetector.getMethodName() + " (" + pct(ruleWeight) + ") + "
                    + mlDetector.getMethodName() + " (" + pct(mlWeight) + ")");
        } else {
            finalScore = rule.getScore();
            result.setMlSummary("ML model unavailable - rule-based score used");
            result.setAnalysisMethod(ruleDetector.getMethodName() + " (ML unavailable)");
        }
        result.setFinalScore(finalScore);
        result.setExplanation(buildExplanation(ctx, result));
        return result;
    }

    private String pct(double w) {
        return String.format(Locale.ROOT, "%.0f%%", w * 100);
    }

    /** Human readable explanation: headline + one line per triggered factor, biggest contributor first. */
    static String buildExplanation(AnalysisContext ctx, RiskResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Risk score ").append(r.getFinalScore()).append("/100 (").append(r.getRiskLevel()).append("). ");
        sb.append("Rule score ").append(r.getRuleScore());
        if (r.isMlUsed()) sb.append(", ML probability ").append(r.getMlScore()).append("%");
        sb.append(".\n");
        List<RiskFactor> triggered = new ArrayList<>(r.getTriggeredFactors());
        triggered.sort(Comparator.comparingDouble(RiskFactor::getContribution).reversed());
        if (triggered.isEmpty()) {
            sb.append("No risk factor was triggered - the transaction matches the customer's normal behaviour.");
        } else {
            for (RiskFactor f : triggered) {
                sb.append("\u2713 ").append(f.getDescription())
                        .append(String.format(Locale.ROOT, " (+%.1f points)", f.getContribution())).append('\n');
            }
        }
        return sb.toString().trim();
    }
}
