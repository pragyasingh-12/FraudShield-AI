package com.fraudshield.dao;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.Device;
import com.fraudshield.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** JDBC access for the devices table (devices a customer has used before). */
public class DeviceDAO implements DAOOperations<Device> {
    private static final String COLS = "id, user_id, device_id, device_type, first_seen";

    @Override
    public int create(Device d) throws DatabaseOperationException {
        // INSERT IGNORE: re-registering the same (user, device) pair is harmless
        String sql = "INSERT IGNORE INTO devices (user_id, device_id, device_type) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getUserId());
            ps.setString(2, d.getDeviceId());
            ps.setString(3, d.getDeviceType());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not register device: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Device> findById(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM devices WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read device: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Device> findAll() throws DatabaseOperationException {
        return query("SELECT " + COLS + " FROM devices ORDER BY id", null);
    }

    public List<Device> findByUserId(int userId) throws DatabaseOperationException {
        return query("SELECT " + COLS + " FROM devices WHERE user_id = ? ORDER BY first_seen", userId);
    }

    private List<Device> query(String sql, Integer param) throws DatabaseOperationException {
        List<Device> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not list devices: " + ex.getMessage(), ex);
        }
    }

    /** Device ids already registered for this customer, as a Set for O(1) lookups. */
    public Set<String> findDeviceIdsByUser(int userId) throws DatabaseOperationException {
        Set<String> ids = new HashSet<>();
        for (Device d : findByUserId(userId)) ids.add(d.getDeviceId());
        return ids;
    }

    /** Device ids that were used in at least one BLOCKED transaction (suspicious devices). */
    public Set<String> findSuspiciousDeviceIds() throws DatabaseOperationException {
        Set<String> ids = new HashSet<>();
        String sql = "SELECT DISTINCT device_id FROM transactions WHERE status = 'BLOCKED'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getString(1));
            return ids;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not load suspicious devices: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean update(Device d) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE devices SET device_type=? WHERE id=?")) {
            ps.setString(1, d.getDeviceType());
            ps.setInt(2, d.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not update device: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM devices WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not delete device: " + ex.getMessage(), ex);
        }
    }

    private Device map(ResultSet rs) throws SQLException {
        Device d = new Device(rs.getInt("user_id"), rs.getString("device_id"), rs.getString("device_type"));
        d.setId(rs.getInt("id"));
        d.setFirstSeen(DateUtil.fromTimestamp(rs.getTimestamp("first_seen")));
        return d;
    }
}
