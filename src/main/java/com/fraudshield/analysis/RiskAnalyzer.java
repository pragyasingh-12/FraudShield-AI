package com.fraudshield.analysis;

import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.model.RiskResult;

/** Combines one or more detectors into the final explainable RiskResult. */
public interface RiskAnalyzer {
    RiskResult assess(AnalysisContext context) throws FraudAnalysisException;
}
