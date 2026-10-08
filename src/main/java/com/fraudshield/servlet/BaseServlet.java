package com.fraudshield.servlet;

import com.fraudshield.model.User;
import com.fraudshield.service.ServiceRegistry;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Shared helpers for all servlets: view rendering, flash messages, error handling. Keeps servlets thin. */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected final transient Logger log = Logger.getLogger(getClass().getName());

    protected ServiceRegistry services() {
        return ServiceRegistry.get();
    }

    protected User currentUser(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("user");
    }

    /** Forwards to /WEB-INF/views/{view}.jsp (JSPs are not directly reachable by URL). */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view, String title, String nav)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.setAttribute("activeNav", nav);
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /** One-shot message shown after a redirect (type = success | error | info). */
    protected void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flash", message);
        req.getSession().setAttribute("flashType", type);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    /** Logs the problem and shows the friendly error page (never a stack trace). */
    protected void fail(HttpServletRequest req, HttpServletResponse resp, Exception ex, String userMessage)
            throws ServletException, IOException {
        log.log(Level.SEVERE, userMessage, ex);
        req.setAttribute("errorMessage", userMessage);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        render(req, resp, "error", "Something went wrong", "");
    }

    protected boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (currentUser(req).isAdmin()) return true;
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access required.");
        return false;
    }
}
