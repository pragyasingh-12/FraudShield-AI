package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.RiskResult;
import com.fraudshield.model.Transaction;
import com.fraudshield.model.TransactionStatus;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /risk?id=1024 or /risk?ref=TXN2024 - detailed explainable analysis of one transaction. */
@WebServlet(name = "RiskAnalysisServlet", urlPatterns = "/risk")
public class RiskAnalysisServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String key = req.getParameter("ref") != null ? req.getParameter("ref") : req.getParameter("id");
        try {
            Optional<Transaction> txn = services().risk().findTransaction(key);
            if (txn.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.setAttribute("errorMessage", "Transaction not found. Check the transaction ID and try again.");
                render(req, resp, "error", "Transaction not found", "");
                return;
            }
            Transaction t = txn.get();
            Optional<RiskResult> result = services().risk().getResult(t.getId());
            req.setAttribute("txn", t);
            req.setAttribute("result", result.orElse(null));
            // PENDING = the worker thread has not finished yet: let the page refresh itself
            req.setAttribute("autoRefresh", t.getStatus() == TransactionStatus.PENDING && result.isEmpty());
            req.setAttribute("justSubmitted", "1".equals(req.getParameter("new")));
            render(req, resp, "risk-analysis", "Risk analysis " + t.getReference(), "history");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The analysis could not be loaded because of a database problem.");
        }
    }
}
