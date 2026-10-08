package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.dao.TransactionDAO;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /analytics - distribution charts, factor averages and the Weka model evaluation report. */
@WebServlet(name = "AnalyticsServlet", urlPatterns = "/analytics")
public class AnalyticsServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final transient TransactionDAO transactionDAO = new TransactionDAO();   // read-only statistics queries

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("stats", services().dashboard().getStats());
            req.setAttribute("factorAverages", services().risk().averageFactorScores());
            req.setAttribute("byHour", transactionDAO.countByHour());
            req.setAttribute("byType", transactionDAO.statsByType());
            req.setAttribute("byLocation", transactionDAO.statsByLocation());
            req.setAttribute("modelReport", services().risk().getModelReport());
            render(req, resp, "analytics", "Analytics", "analytics");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "Analytics could not be loaded because of a database problem.");
        }
    }
}
