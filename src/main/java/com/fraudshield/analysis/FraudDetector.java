package com.fraudshield.analysis;

import com.fraudshield.exception.FraudAnalysisException;

/**
 * Contract for anything that can score a transaction (rule engine, ML model, ...).
 * Callers hold a FraudDetector reference and do not care which implementation runs (polymorphism).
 */
public interface FraudDetector {
    DetectionResult analyze(AnalysisContext context) throws FraudAnalysisException;

    String getMethodName();
}
