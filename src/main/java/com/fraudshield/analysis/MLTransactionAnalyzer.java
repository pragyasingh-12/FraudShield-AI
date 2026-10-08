package com.fraudshield.analysis;

import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.model.Transaction;
import java.util.Collections;
import java.util.Locale;

/**
 * Machine-learning detector: converts the transaction into the 7 features the Weka J48 model was
 * trained on and returns the model's fraud probability as a 0-100 score.
 */
public class MLTransactionAnalyzer extends TransactionAnalyzer {
    public static final String NAME = "Weka J48 anomaly classifier";
    private final MLModelService modelService;

    public MLTransactionAnalyzer(MLModelService modelService) {
        super(NAME);
        this.modelService = modelService;
    }

    @Override
    protected DetectionResult doAnalyze(AnalysisContext ctx) throws FraudAnalysisException {
        if (!modelService.isReady()) {
            throw new FraudAnalysisException("ML model unavailable: " + modelService.getReport().getStatus());
        }
        double p = modelService.predictFraudProbability(features(ctx));
        int score = (int) Math.round(p * 100);
        String summary = String.format(Locale.ROOT, "Weka J48 model estimates a %.0f%% probability that this pattern is fraudulent", p * 100);
        return new DetectionResult(score, Collections.emptyList(), NAME, summary);
    }

    /** Feature vector - order must equal MLModelService.FEATURES. */
    static double[] features(AnalysisContext ctx) {
        Transaction t = ctx.getTransaction();
        return new double[]{
                t.getAmount(),
                ctx.amountRatio(),
                ctx.getTxnsLastHour(),
                t.getHour(),
                ctx.isNewDevice() ? 1 : 0,
                ctx.isLocationChanged() ? 1 : 0,
                ctx.behaviorDeviation()
        };
    }
}
