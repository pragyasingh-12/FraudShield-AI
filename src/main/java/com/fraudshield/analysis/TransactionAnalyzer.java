package com.fraudshield.analysis;

import com.fraudshield.exception.FraudAnalysisException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class (abstraction + inheritance). It fixes the analysis procedure
 * (Template Method): validate -> doAnalyze() -> count. Subclasses supply only doAnalyze().
 */
public abstract class TransactionAnalyzer implements FraudDetector {
    private final String methodName;
    private final AtomicLong analysisCount = new AtomicLong();   // updated by many threads, so atomic

    protected TransactionAnalyzer(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public final DetectionResult analyze(AnalysisContext context) throws FraudAnalysisException {
        if (context == null || context.getTransaction() == null) {
            throw new FraudAnalysisException("Nothing to analyse: transaction is missing");
        }
        DetectionResult result = doAnalyze(context);
        analysisCount.incrementAndGet();
        return result;
    }

    protected abstract DetectionResult doAnalyze(AnalysisContext context) throws FraudAnalysisException;

    @Override
    public String getMethodName() {
        return methodName;
    }

    public long getAnalysisCount() {
        return analysisCount.get();
    }
}
