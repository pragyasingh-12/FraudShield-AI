package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.RiskLevel;
import com.fraudshield.model.TransactionFilter;
import com.fraudshield.model.TransactionStatus;
import com.fraudshield.service.TransactionService;
import com.fraudshield.util.ValidationUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /history - searchable, filterable, paged transaction list. POST deletes (admin only). */
@WebServlet(name = "TransactionHistoryServlet", urlPatterns = "/history")
public class TransactionHistoryServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            TransactionFilter f = new TransactionFilter();
            f.setQuery(ValidationUtil.clean(req.getParameter("q"), 80));
            f.setRiskLevel(RiskLevel.parse(req.getParameter("level")));
            f.setStatus(TransactionStatus.parse(req.getParameter("status")));
            String type = ValidationUtil.trim(req.getParameter("type"));
            f.setType(TransactionService.TYPES.contains(type) ? type : null);
            f.setFromDate(parseDate(req.getParameter("from")));
            f.setToDate(parseDate(req.getParameter("to")));
            f.setMinAmount(ValidationUtil.parseDouble(req.getParameter("min")));
            f.setMaxAmount(ValidationUtil.parseDouble(req.getParameter("max")));
            f.setPage(ValidationUtil.parseInt(req.getParameter("page"), 1));

            req.setAttribute("pageResult", services().transactions().search(f));
            req.setAttribute("types", TransactionService.TYPES);
            // the filter string is re-used by the pager links
            req.setAttribute("filterQuery", buildQueryString(req));
            render(req, resp, "transaction-history", "Transaction history", "history");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The history could not be loaded because of a database problem.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAdmin(req, resp)) return;
        try {
            int id = ValidationUtil.parseInt(req.getParameter("id"), -1);
            if (services().transactions().delete(id)) {
                services().dashboard().refresh();
                flash(req, "success", "Transaction deleted.");
            } else {
                flash(req, "error", "Transaction not found.");
            }
            redirect(req, resp, "/history");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The transaction could not be deleted because of a database problem.");
        }
    }

    private LocalDate parseDate(String s) {
        try {
            return ValidationUtil.isBlank(s) ? null : LocalDate.parse(s.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String buildQueryString(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        for (String k : new String[]{"q", "level", "status", "type", "from", "to", "min", "max"}) {
            String v = req.getParameter(k);
            if (!ValidationUtil.isBlank(v)) {
                sb.append('&').append(k).append('=').append(java.net.URLEncoder.encode(v.trim(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
