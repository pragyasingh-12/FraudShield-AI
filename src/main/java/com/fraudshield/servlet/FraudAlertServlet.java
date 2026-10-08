package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.AlertStatus;
import com.fraudshield.model.RiskLevel;
import com.fraudshield.util.ValidationUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /alerts - list of fraud alerts with filters; POST records the analyst's decision. */
@WebServlet(name = "FraudAlertServlet", urlPatterns = "/alerts")
public class FraudAlertServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("alerts", services().alerts().list(
                    RiskLevel.parse(req.getParameter("level")), AlertStatus.parse(req.getParameter("status"))));
            req.setAttribute("alertStatuses", AlertStatus.values());
            render(req, resp, "fraud-alerts", "Fraud alerts", "alerts");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "Alerts could not be loaded because of a database problem.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int id = ValidationUtil.parseInt(req.getParameter("alertId"), -1);
            AlertStatus status = AlertStatus.parse(req.getParameter("status"));
            if (id < 1 || status == null) {
                flash(req, "error", "Choose an alert status.");
            } else {
                services().alerts().updateStatus(id, status);
                flash(req, "success", "Alert updated to " + status.getLabel() + ".");
            }
            redirect(req, resp, "/alerts");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The alert could not be updated because of a database problem.");
        }
    }
}
