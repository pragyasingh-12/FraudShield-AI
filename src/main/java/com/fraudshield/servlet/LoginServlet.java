package com.fraudshield.servlet;

import com.fraudshield.exception.AuthenticationException;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.User;
import java.io.IOException;
import java.security.SecureRandom;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** GET /login shows the form; POST /login authenticates and starts a fresh session. */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("user") != null) {
            redirect(req, resp, "/dashboard");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        try {
            User user = services().auth().login(email, password);
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();                       // prevents session fixation
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", newToken());
            redirect(req, resp, "/dashboard");
        } catch (AuthenticationException ex) {
            req.setAttribute("error", ex.getMessage());
            req.setAttribute("email", email);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        } catch (DatabaseOperationException ex) {
            log.warning("Login failed - database problem: " + ex.getMessage());
            req.setAttribute("error", "The database is not reachable. Check application.properties and that MySQL is running.");
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        }
    }

    private static String newToken() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
