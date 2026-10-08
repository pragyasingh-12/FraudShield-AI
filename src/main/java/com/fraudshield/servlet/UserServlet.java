package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.exception.InvalidTransactionException;
import com.fraudshield.model.User;
import com.fraudshield.util.ValidationUtil;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /users - user list, customer detail (?id=), and admin actions (add user, delete user, register device). */
@WebServlet(name = "UserServlet", urlPatterns = "/users")
public class UserServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int id = ValidationUtil.parseInt(req.getParameter("id"), -1);
            if (id > 0) {
                Optional<User> u = services().users().getById(id);
                if (u.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    req.setAttribute("errorMessage", "User not found.");
                    render(req, resp, "error", "User not found", "");
                    return;
                }
                req.setAttribute("subject", u.get());
                req.setAttribute("devices", services().users().getDevices(id));
                req.setAttribute("profile", services().users().getProfile(id).orElse(null));
                req.setAttribute("recent", services().users().getRecentTransactions(id));
                render(req, resp, "user-detail", u.get().getName(), "users");
                return;
            }
            req.setAttribute("users", services().users().listAll());
            render(req, resp, "users", "Users", "users");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "Users could not be loaded because of a database problem.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAdmin(req, resp)) return;
        String action = ValidationUtil.trim(req.getParameter("action"));
        String back = "/users";
        try {
            switch (action) {
                case "add" -> {
                    services().users().register(ValidationUtil.clean(req.getParameter("name"), 100),
                            ValidationUtil.trim(req.getParameter("email")), ValidationUtil.trim(req.getParameter("phone")),
                            ValidationUtil.trim(req.getParameter("role")), req.getParameter("password"));
                    flash(req, "success", "User created.");
                }
                case "delete" -> {
                    int id = ValidationUtil.parseInt(req.getParameter("id"), -1);
                    if (id == currentUser(req).getId()) {
                        flash(req, "error", "You cannot delete the account you are signed in with.");
                    } else if (services().users().delete(id)) {
                        services().dashboard().refresh();
                        flash(req, "success", "User and all related records deleted.");
                    } else {
                        flash(req, "error", "User not found.");
                    }
                }
                case "addDevice" -> {
                    int id = ValidationUtil.parseInt(req.getParameter("id"), -1);
                    services().users().addDevice(id, ValidationUtil.trim(req.getParameter("deviceId")),
                            ValidationUtil.trim(req.getParameter("deviceType")));
                    flash(req, "success", "Device registered as trusted.");
                    back = "/users?id=" + id;
                }
                default -> flash(req, "error", "Unknown action.");
            }
            redirect(req, resp, back);
        } catch (InvalidTransactionException ex) {
            flash(req, "error", ex.getMessage());
            redirect(req, resp, back);
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The change could not be saved because of a database problem.");
        }
    }
}
