package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.exception.FraudAnalysisException;
import com.fraudshield.exception.InvalidTransactionException;
import com.fraudshield.model.RiskResult;
import com.fraudshield.model.Transaction;
import com.fraudshield.util.ValidationUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /analyze - "what-if" analysis: scores a transaction with the full engine but stores nothing. */
@WebServlet(name = "TransactionAnalysisServlet", urlPatterns = "/analyze")
public class TransactionAnalysisServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            TransactionServlet.prepareForm(req);
            req.setAttribute("nowValue", INPUT_FORMAT.format(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
            render(req, resp, "analyze", "Transaction analysis", "analyze");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The page could not be loaded because of a database problem.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            TransactionServlet.prepareForm(req);
            req.setAttribute("nowValue", ValidationUtil.trim(req.getParameter("time")));
            try {
                Transaction t = TransactionServlet.parse(req);
                RiskResult result = services().transactions().analyzeOnly(t);
                req.setAttribute("txn", t);
                req.setAttribute("result", result);
                req.setAttribute("dryRun", Boolean.TRUE);
            } catch (InvalidTransactionException ex) {
                req.setAttribute("error", ex.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (FraudAnalysisException ex) {
                req.setAttribute("error", "Analysis failed: " + ex.getMessage());
            }
            render(req, resp, "analyze", "Transaction analysis", "analyze");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The analysis could not run because of a database problem.");
        }
    }
}
