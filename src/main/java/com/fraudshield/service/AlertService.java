package com.fraudshield.service;

import com.fraudshield.dao.FraudAlertDAO;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.AlertStatus;
import com.fraudshield.model.FraudAlert;
import com.fraudshield.model.RiskLevel;
import java.util.List;

/** Reading and handling of fraud alerts. */
public class AlertService {
    private final FraudAlertDAO alertDAO = new FraudAlertDAO();
    private final FraudDetectionService detectionService;

    public AlertService(FraudDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    public List<FraudAlert> list(RiskLevel level, AlertStatus status) throws DatabaseOperationException {
        return alertDAO.findFiltered(level, status);
    }

    public int countOpen() throws DatabaseOperationException {
        return alertDAO.countByStatus(AlertStatus.OPEN);
    }

    /** Analyst decision on an alert; also adjusts the transaction status accordingly. */
    public void updateStatus(int alertId, AlertStatus status) throws DatabaseOperationException {
        detectionService.resolveAlert(alertId, status);
    }
}
