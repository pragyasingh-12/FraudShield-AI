package com.fraudshield.servlet;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** GET /dashboard - summary cards, recent alerts and recent transactions (from the cached snapshot). */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard", ""})
public class DashboardServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("stats", services().dashboard().getStats());
            req.setAttribute("modelReport", services().ml().getReport());
            req.setAttribute("analysedCount", services().detection().getAnalysedCount());
            render(req, resp, "dashboard", "Dashboard", "dashboard");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The dashboard could not be loaded because of a database problem.");
        }
    }
}
