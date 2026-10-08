package com.fraudshield.service;

import com.fraudshield.dao.DeviceDAO;
import com.fraudshield.dao.TransactionDAO;
import com.fraudshield.dao.UserDAO;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.exception.InvalidTransactionException;
import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.model.Device;
import com.fraudshield.model.Transaction;
import com.fraudshield.model.User;
import com.fraudshield.util.PasswordUtil;
import java.util.List;
import java.util.Optional;

/** User and device management. */
public class UserService {
    private final UserDAO userDAO = new UserDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final BehaviorProfileService profileService;

    public UserService(BehaviorProfileService profileService) {
        this.profileService = profileService;
    }

    public List<User> listAll() throws DatabaseOperationException { return userDAO.findAll(); }

    public List<User> listCustomers() throws DatabaseOperationException { return userDAO.findByRole(User.ROLE_CUSTOMER); }

    public Optional<User> getById(int id) throws DatabaseOperationException { return userDAO.findById(id); }

    public List<Device> getDevices(int userId) throws DatabaseOperationException { return deviceDAO.findByUserId(userId); }

    public Optional<BehaviorProfile> getProfile(int userId) throws DatabaseOperationException {
        return profileService.getSnapshot(userId);
    }

    public List<Transaction> getRecentTransactions(int userId) throws DatabaseOperationException {
        return transactionDAO.findByUserId(userId, 10);
    }

    public int register(String name, String email, String phone, String role, String password)
            throws InvalidTransactionException, DatabaseOperationException {
        if (password == null || password.length() < 8) {
            throw new InvalidTransactionException("Password must be at least 8 characters.");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^[0-9+ -]{7,15}$")) {
            throw new InvalidTransactionException("Phone number looks invalid.");
        }
        try {
            User u = new User(name, email, PasswordUtil.hash(password), phone, role);   // setters validate
            if (userDAO.findByEmail(u.getEmail()).isPresent()) {
                throw new InvalidTransactionException("A user with this e-mail already exists.");
            }
            return userDAO.create(u);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTransactionException(ex.getMessage());
        }
    }

    public void addDevice(int userId, String deviceId, String type) throws InvalidTransactionException, DatabaseOperationException {
        if (deviceId == null || !deviceId.matches("^[A-Za-z0-9_-]{3,64}$")) {
            throw new InvalidTransactionException("Device ID must be 3-64 letters, digits, '-' or '_'.");
        }
        if (userDAO.findById(userId).isEmpty()) throw new InvalidTransactionException("Unknown user.");
        deviceDAO.create(new Device(userId, deviceId, type));
    }

    public boolean delete(int userId) throws DatabaseOperationException {
        return userDAO.delete(userId);
    }
}
