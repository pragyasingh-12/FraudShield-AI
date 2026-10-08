package com.fraudshield.service;

import com.fraudshield.dao.UserDAO;
import com.fraudshield.exception.AuthenticationException;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.User;
import com.fraudshield.util.AppConfig;
import com.fraudshield.util.PasswordUtil;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Login logic: password check with salted hashes and a simple lock-out after repeated failures. */
public class AuthenticationService {
    private final UserDAO userDAO = new UserDAO();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockedUntil = new ConcurrentHashMap<>();
    private final int maxAttempts = AppConfig.getInt("auth.max.failed.attempts", 5);
    private final long lockMillis = AppConfig.getInt("auth.lock.minutes", 5) * 60_000L;

    public User login(String email, String password) throws AuthenticationException, DatabaseOperationException {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Enter your e-mail and password.");
        }
        String key = email.trim().toLowerCase();
        Long until = lockedUntil.get(key);
        if (until != null) {
            if (System.currentTimeMillis() < until) {
                throw new AuthenticationException("Too many failed attempts. Try again in a few minutes.");
            }
            lockedUntil.remove(key);
            failedAttempts.remove(key);
        }

        Optional<User> found = userDAO.findByEmail(key);
        if (found.isEmpty() || !PasswordUtil.verify(password, found.get().getPasswordHash())) {
            int n = failedAttempts.merge(key, 1, Integer::sum);
            if (n >= maxAttempts) lockedUntil.put(key, System.currentTimeMillis() + lockMillis);
            throw new AuthenticationException("Invalid e-mail or password.");   // same message: no user enumeration
        }
        User user = found.get();
        if (!user.isStaff()) {
            throw new AuthenticationException("This account is a monitored customer account and cannot sign in to the console.");
        }
        failedAttempts.remove(key);
        return user;
    }
}
