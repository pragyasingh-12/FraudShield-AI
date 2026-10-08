package com.fraudshield.dao;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.BehaviorProfile;
import com.fraudshield.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC access for behavior_profiles (one persisted snapshot per customer). */
public class BehaviorProfileDAO implements DAOOperations<BehaviorProfile> {
    private static final String COLS = "id, user_id, avg_transaction_amount, std_dev_amount, max_amount, "
            + "usual_start_hour, usual_end_hour, usual_location, transaction_frequency, total_transactions, updated_at";

    @Override
    public int create(BehaviorProfile p) throws DatabaseOperationException {
        String sql = "INSERT INTO behavior_profiles (user_id, avg_transaction_amount, std_dev_amount, max_amount, "
                + "usual_start_hour, usual_end_hour, usual_location, transaction_frequency, total_transactions) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not create behaviour profile: " + ex.getMessage(), ex);
        }
    }

    /** Insert-or-update keyed on the unique user_id column. */
    public void save(BehaviorProfile p) throws DatabaseOperationException {
        String sql = "INSERT INTO behavior_profiles (user_id, avg_transaction_amount, std_dev_amount, max_amount, "
                + "usual_start_hour, usual_end_hour, usual_location, transaction_frequency, total_transactions) "
                + "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                + "avg_transaction_amount=VALUES(avg_transaction_amount), std_dev_amount=VALUES(std_dev_amount), "
                + "max_amount=VALUES(max_amount), usual_start_hour=VALUES(usual_start_hour), "
                + "usual_end_hour=VALUES(usual_end_hour), usual_location=VALUES(usual_location), "
                + "transaction_frequency=VALUES(transaction_frequency), total_transactions=VALUES(total_transactions)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not save behaviour profile: " + ex.getMessage(), ex);
        }
    }

    private void bind(PreparedStatement ps, BehaviorProfile p) throws SQLException {
        ps.setInt(1, p.getUserId());
        ps.setDouble(2, p.getAvgAmount());
        ps.setDouble(3, p.getStdDevAmount());
        ps.setDouble(4, p.getMaxAmount());
        ps.setInt(5, p.getUsualStartHour());
        ps.setInt(6, p.getUsualEndHour());
        ps.setString(7, p.getUsualLocation());
        ps.setDouble(8, p.getTransactionFrequency());
        ps.setInt(9, p.getTotalTransactions());
    }

    @Override
    public Optional<BehaviorProfile> findById(int id) throws DatabaseOperationException {
        return queryOne("SELECT " + COLS + " FROM behavior_profiles WHERE id = ?", id);
    }

    public Optional<BehaviorProfile> findByUserId(int userId) throws DatabaseOperationException {
        return queryOne("SELECT " + COLS + " FROM behavior_profiles WHERE user_id = ?", userId);
    }

    private Optional<BehaviorProfile> queryOne(String sql, int param) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read behaviour profile: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<BehaviorProfile> findAll() throws DatabaseOperationException {
        List<BehaviorProfile> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM behavior_profiles ORDER BY user_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not list behaviour profiles: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean update(BehaviorProfile p) throws DatabaseOperationException {
        save(p);
        return true;
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM behavior_profiles WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not delete behaviour profile: " + ex.getMessage(), ex);
        }
    }

    private BehaviorProfile map(ResultSet rs) throws SQLException {
        BehaviorProfile p = new BehaviorProfile();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setAvgAmount(rs.getDouble("avg_transaction_amount"));
        p.setStdDevAmount(rs.getDouble("std_dev_amount"));
        p.setMaxAmount(rs.getDouble("max_amount"));
        p.setUsualStartHour(rs.getInt("usual_start_hour"));
        p.setUsualEndHour(rs.getInt("usual_end_hour"));
        p.setUsualLocation(rs.getString("usual_location"));
        p.setTransactionFrequency(rs.getDouble("transaction_frequency"));
        p.setTotalTransactions(rs.getInt("total_transactions"));
        p.setUpdatedAt(DateUtil.fromTimestamp(rs.getTimestamp("updated_at")));
        return p;
    }
}
