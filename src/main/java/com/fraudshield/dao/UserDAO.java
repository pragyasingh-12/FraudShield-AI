package com.fraudshield.dao;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.User;
import com.fraudshield.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC access for the users table. */
public class UserDAO implements DAOOperations<User> {
    private static final String COLS = "id, name, email, password_hash, phone, role, created_at";

    @Override
    public int create(User u) throws DatabaseOperationException {
        String sql = "INSERT INTO users (name, email, password_hash, phone, role) VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not create user: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<User> findById(int id) throws DatabaseOperationException {
        return queryOne("SELECT " + COLS + " FROM users WHERE id = ?", String.valueOf(id));
    }

    public Optional<User> findByEmail(String email) throws DatabaseOperationException {
        return queryOne("SELECT " + COLS + " FROM users WHERE email = ?", email.trim().toLowerCase());
    }

    private Optional<User> queryOne(String sql, String param) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read user: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseOperationException {
        return findByRole(null);
    }

    /** All users, or only those with the given role when role != null. */
    public List<User> findByRole(String role) throws DatabaseOperationException {
        String sql = "SELECT " + COLS + " FROM users" + (role == null ? "" : " WHERE role = ?") + " ORDER BY id";
        List<User> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (role != null) ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not list users: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean update(User u) throws DatabaseOperationException {
        String sql = "UPDATE users SET name=?, email=?, phone=?, role=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getRole());
            ps.setInt(5, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not update user: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not delete user: " + ex.getMessage(), ex);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(DateUtil.fromTimestamp(rs.getTimestamp("created_at")));
        return u;
    }
}
