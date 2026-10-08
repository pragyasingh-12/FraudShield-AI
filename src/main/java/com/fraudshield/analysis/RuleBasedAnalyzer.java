package com.fraudshield.analysis;

import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.model.RiskFactor;
import com.fraudshield.model.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rule-based + behavioural detector. Produces six factors (amount, frequency, time, device,
 * location, behaviour), each scored 0-100 and weighted by RiskWeights. Fully transparent:
 * every number in the final score can be traced to one rule here.
 */
public class RuleBasedAnalyzer extends TransactionAnalyzer {
    public static final String NAME = "Rule-based + behavioural analysis";

    public RuleBasedAnalyzer() {
        super(NAME);
    }

    @Override
    protected DetectionResult doAnalyze(AnalysisContext ctx) {
        List<RiskFactor> factors = new ArrayList<>();
        factors.add(amountFactor(ctx));
        factors.add(frequencyFactor(ctx));
        factors.add(timeFactor(ctx));
        factors.add(deviceFactor(ctx));
        factors.add(locationFactor(ctx));
        factors.add(behaviorFactor(ctx));

        double total = 0;
        for (RiskFactor f : factors) total += f.getContribution();
        int score = (int) Math.round(total);
        String summary = ctx.hasHistory() ? "Compared against " + ctx.getProfile().getTotalTransactions()
                + " approved past transactions" : "Limited history - conservative defaults used";
        return new DetectionResult(score, factors, NAME, summary);
    }

    private RiskFactor amountFactor(AnalysisContext ctx) {
        double amount = ctx.getTransaction().getAmount();
        int score;
        String text;
        if (ctx.hasHistory()) {
            double avg = ctx.getProfile().getAvgAmount();
            double ratio = ctx.amountRatio();
            if (ratio <= 2) score = 0;
            else if (ratio <= 3) score = 30;
            else if (ratio <= 5) score = 60;
            else if (ratio <= 10) score = 85;
            else score = 100;
            if (amount >= 100000) score = Math.max(score, 70);   // UPI per-transaction ceiling is about Rs 1,00,000
            text = score >= RiskFactor.TRIGGER_THRESHOLD
                    ? String.format(Locale.ROOT, "Transaction amount (Rs %,.0f) is %.1fx the customer's normal average (Rs %,.0f)", amount, ratio, avg)
                    : String.format(Locale.ROOT, "Amount (Rs %,.0f) is within the customer's normal range (average Rs %,.0f)", amount, avg);
        } else {
            score = amount >= 100000 ? 100 : amount >= 50000 ? 70 : amount >= 20000 ? 40 : 0;
            text = String.format(Locale.ROOT, "Amount Rs %,.0f judged by absolute limits (not enough history)", amount);
        }
        return new RiskFactor("AMOUNT", "Amount anomaly", score, RiskWeights.AMOUNT, text);
    }

    private RiskFactor frequencyFactor(AnalysisContext ctx) {
        int recent = ctx.getTxnsLast10Min();
        int velocity = recent == 0 ? 0 : recent == 1 ? 40 : recent == 2 ? 70 : 100;
        int daily = 0;
        if (ctx.hasHistory()) {
            double avgDaily = Math.max(1.0, ctx.getProfile().getTransactionFrequency());
            double ratio = (ctx.getTxnsToday() + 1) / avgDaily;
            daily = ratio > 4 ? 80 : ratio > 3 ? 60 : ratio > 2 ? 30 : 0;
        }
        int score = Math.max(velocity, daily);
        String text;
        if (velocity >= daily && velocity >= RiskFactor.TRIGGER_THRESHOLD) {
            text = "Multiple transactions detected within a short interval (" + recent + " in the previous 10 minutes)";
        } else if (daily >= RiskFactor.TRIGGER_THRESHOLD) {
            text = "Abnormal transaction frequency: " + (ctx.getTxnsToday() + 1) + " payments today versus a usual "
                    + String.format(Locale.ROOT, "%.1f", ctx.getProfile().getTransactionFrequency()) + " per active day";
        } else {
            text = "Transaction frequency is normal";
        }
        return new RiskFactor("FREQUENCY", "Frequency anomaly", score, RiskWeights.FREQUENCY, text);
    }

    private RiskFactor timeFactor(AnalysisContext ctx) {
        int hour = ctx.getTransaction().getHour();
        boolean night = hour >= 0 && hour < 5;
        int score;
        String text;
        if (ctx.hasHistory()) {
            BehaviorProfile p = ctx.getProfile();
            int start = p.getUsualStartHour();
            int end = p.getUsualEndHour();
            int distance = hour < start ? start - hour : Math.max(0, hour - end);
            score = distance == 0 ? 0 : Math.min(100, 30 + 15 * distance);
            if (night && !(hour >= start && hour <= end)) score = Math.max(score, 80);
            text = score >= RiskFactor.TRIGGER_THRESHOLD
                    ? String.format("Transaction occurred at an unusual time (%02d:00; customer normally pays between %02d:00 and %02d:00)", hour, start, end)
                    : String.format("Time (%02d:00) matches the customer's usual hours (%02d:00-%02d:00)", hour, start, end);
        } else {
            score = night ? 70 : 0;
            text = night ? String.format("Late-night transaction (%02d:00)", hour) : "Transaction time is normal";
        }
        return new RiskFactor("TIME", "Time anomaly", score, RiskWeights.TIME, text);
    }

    private RiskFactor deviceFactor(AnalysisContext ctx) {
        boolean isNew = ctx.isNewDevice();
        boolean suspicious = ctx.isSuspiciousDevice();
        int score = isNew && suspicious ? 100 : suspicious ? 90 : isNew ? 70 : 0;
        String text;
        if (isNew && suspicious) text = "New device detected, and it was used in a previously blocked transaction";
        else if (suspicious) text = "Device was involved in a previously blocked transaction";
        else if (isNew) text = "New device detected (never used by this customer before)";
        else text = "Known, trusted device";
        return new RiskFactor("DEVICE", "Device anomaly", score, RiskWeights.DEVICE, text);
    }

    private RiskFactor locationFactor(AnalysisContext ctx) {
        Transaction t = ctx.getTransaction();
        int score = 0;
        String text;
        if (!ctx.hasHistory()) {
            text = "Location not judged (not enough history)";
        } else if (!ctx.isLocationChanged()) {
            text = "Location matches the customer's usual location (" + ctx.getProfile().getUsualLocation() + ")";
        } else if (ctx.isLocationEverSeen()) {
            score = 25;
            text = "Location (" + t.getLocation() + ") differs from the usual one but has been used before";
        } else {
            score = 100;
            text = "Location differs from historical behaviour: " + t.getLocation() + " (usual: "
                    + ctx.getProfile().getUsualLocation() + ")";
        }
        return new RiskFactor("LOCATION", "Location anomaly", score, RiskWeights.LOCATION, text);
    }

    private RiskFactor behaviorFactor(AnalysisContext ctx) {
        int score = ctx.behaviorDeviation();
        List<String> parts = new ArrayList<>();
        if (ctx.hasHistory()) {
            BehaviorProfile p = ctx.getProfile();
            Transaction t = ctx.getTransaction();
            if (!p.getKnownReceivers().contains(AnalysisContext.norm(t.getReceiver()))) parts.add("new/unknown beneficiary");
            if (p.getMaxAmount() > 0 && t.getAmount() > 1.5 * p.getMaxAmount()) parts.add("larger than any previous payment");
            if (p.getTypeCounts().getOrDefault(t.getTransactionType(), 0) * 10 < p.getTotalTransactions()) {
                parts.add("rarely used payment type (" + t.getTransactionType() + ")");
            }
        }
        String text = parts.isEmpty() ? "Behaviour consistent with transaction history"
                : "Deviation from historical behaviour: " + String.join(", ", parts);
        return new RiskFactor("BEHAVIOR", "Behaviour deviation", score, RiskWeights.BEHAVIOR, text);
    }
}
