package com.fraudshield.servlet;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.exception.InvalidTransactionException;
import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.model.Device;
import com.fraudshield.model.RiskResult;
import com.fraudshield.model.Transaction;
import com.fraudshield.model.User;
import com.fraudshield.service.TransactionService;
import com.fraudshield.util.ValidationUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /transactions/add - the Add Transaction form (GET), saving + async analysis (POST), rapid-burst demo. */
@WebServlet(name = "TransactionServlet", urlPatterns = "/transactions/add")
public class TransactionServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            prepareForm(req);
            req.setAttribute("nowValue", INPUT_FORMAT.format(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
            render(req, resp, "add-transaction", "Add transaction", "add");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The form could not be loaded because of a database problem.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Transaction t = parse(req);
            if ("burst".equals(req.getParameter("action"))) {
                int count = ValidationUtil.parseInt(req.getParameter("count"), 5);
                List<RiskResult> results = services().transactions().submitBurst(t, count);
                int worst = 0;
                for (RiskResult r : results) worst = Math.max(worst, r.getFinalScore());
                flash(req, "success", results.size() + " rapid transactions were stored and analysed in parallel on the worker pool. Highest risk score: " + worst + "/100.");
                redirect(req, resp, "/history");
                return;
            }
            Transaction saved = services().transactions().submit(t);
            redirect(req, resp, "/risk?id=" + saved.getId() + "&new=1");
        } catch (InvalidTransactionException ex) {
            try {
                prepareForm(req);
            } catch (DatabaseOperationException dbEx) {
                fail(req, resp, dbEx, "The form could not be loaded because of a database problem.");
                return;
            }
            req.setAttribute("error", ex.getMessage());
            req.setAttribute("nowValue", ValidationUtil.trim(req.getParameter("time")));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "add-transaction", "Add transaction", "add");
        } catch (DatabaseOperationException ex) {
            fail(req, resp, ex, "The transaction could not be saved because of a database problem.");
        }
    }

    /** Reads and parses request parameters; business validation happens in TransactionService. */
    static Transaction parse(HttpServletRequest req) throws InvalidTransactionException {
        Double amount = ValidationUtil.parseDouble(req.getParameter("amount"));
        if (amount == null) throw new InvalidTransactionException("Amount must be a valid number.");
        int userId = ValidationUtil.parseInt(req.getParameter("userId"), -1);
        LocalDateTime time = ValidationUtil.parseDateTime(req.getParameter("time"));
        if (time == null) throw new InvalidTransactionException("Transaction date/time is invalid.");
        return new Transaction(userId, amount,
                ValidationUtil.trim(req.getParameter("type")).toUpperCase(),
                ValidationUtil.clean(req.getParameter("receiver"), 120),
                time,
                ValidationUtil.clean(req.getParameter("deviceId"), 80),
                ValidationUtil.clean(req.getParameter("location"), 80));
    }

    /** Puts the customer list and per-customer hints (usual device / location) on the request. */
    static void prepareForm(HttpServletRequest req) throws DatabaseOperationException {
        var registry = com.fraudshield.service.ServiceRegistry.get();
        List<User> customers = registry.users().listCustomers();
        Map<Integer, String> usualDevice = new HashMap<>();
        Map<Integer, String> usualLocation = new HashMap<>();
        Map<Integer, String> avgAmount = new HashMap<>();
        for (User u : customers) {
            List<Device> devices = registry.users().getDevices(u.getId());
            usualDevice.put(u.getId(), devices.isEmpty() ? "" : devices.get(0).getDeviceId());
            Optional<BehaviorProfile> p = registry.users().getProfile(u.getId());
            usualLocation.put(u.getId(), p.map(BehaviorProfile::getUsualLocation).orElse(""));
            avgAmount.put(u.getId(), String.valueOf(Math.round(p.map(BehaviorProfile::getAvgAmount).orElse(2000.0))));
        }
        req.setAttribute("customers", customers);
        req.setAttribute("usualDevice", usualDevice);
        req.setAttribute("usualLocation", usualLocation);
        req.setAttribute("avgAmount", avgAmount);
        req.setAttribute("types", TransactionService.TYPES);
    }
}
