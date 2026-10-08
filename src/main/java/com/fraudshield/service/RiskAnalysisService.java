package com.fraudshield.service;

import com.fraudshield.analysis.MLModelService;
import com.fraudshield.dao.RiskAnalysisDAO;
import com.fraudshield.dao.TransactionDAO;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.RiskResult;
import com.fraudshield.model.Transaction;
import java.util.Map;
import java.util.Optional;

/** Read side of the risk analysis: stored explainable results, factor averages, ML model report. */
public class RiskAnalysisService {
    private final RiskAnalysisDAO riskDAO = new RiskAnalysisDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final MLModelService mlService;

    public RiskAnalysisService(MLModelService mlService) {
        this.mlService = mlService;
    }

    public Optional<Transaction> findTransaction(String idOrReference) throws DatabaseOperationException {
        int id = Transaction.idFromReference(idOrReference);
        return id < 1 ? Optional.empty() : transactionDAO.findById(id);
    }

    public Optional<RiskResult> getResult(int transactionId) throws DatabaseOperationException {
        Optional<RiskResult> r = riskDAO.findByTransactionId(transactionId);
        r.ifPresent(x -> x.setTransactionId(transactionId));
        return r;
    }

    public Map<String, Double> averageFactorScores() throws DatabaseOperationException {
        return riskDAO.averageFactorScores();
    }

    public MLModelService.ModelReport getModelReport() {
        return mlService.getReport();
    }
}
