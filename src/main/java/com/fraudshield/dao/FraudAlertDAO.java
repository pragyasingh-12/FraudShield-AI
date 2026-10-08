package com.fraudshield.dao;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.AlertStatus;
import com.fraudshield.model.FraudAlert;
import com.fraudshield.model.RiskLevel;
import com.fraudshield.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC access for fraud_alerts. */
public class FraudAlertDAO implements DAOOperations<FraudAlert> {
    private static final String SELECT = "SELECT a.id, a.transaction_id, a.risk_level, a.reason, a.alert_status, a.created_at, "
            + "u.name AS user_name, t.amount, t.receiver, t.risk_score "
            + "FROM fraud_alerts a JOIN transactions t ON t.id = a.transaction_id JOIN users u ON u.id = t.user_id ";

    @Override
    public int create(FraudAlert a) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection()) {
            return create(c, a);
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not create alert: " + ex.getMessage(), ex);
        }
    }

    /** Variant that joins the caller's JDBC transaction. */
    public int create(Connection c, FraudAlert a) throws SQLException {
        String sql = "INSERT INTO fraud_alerts (transaction_id, risk_level, reason, alert_status) VALUES (?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getTransactionId());
            ps.setString(2, a.getRiskLevel().name());
            ps.setString(3, a.getReason());
            ps.setString(4, a.getAlertStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    @Override
    public Optional<FraudAlert> findById(int id) throws DatabaseOperationException {
        List<FraudAlert> l = query(SELECT + "WHERE a.id = ?", new Object[]{id});
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public Optional<FraudAlert> findByTransactionId(int transactionId) throws DatabaseOperationException {
        List<FraudAlert> l = query(SELECT + "WHERE a.transaction_id = ?", new Object[]{transactionId});
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<FraudAlert> findAll() throws DatabaseOperationException {
        return query(SELECT + "ORDER BY a.id DESC", new Object[0]);
    }

    public List<FraudAlert> findRecent(int limit) throws DatabaseOperationException {
        return query(SELECT + "ORDER BY a.id DESC LIMIT ?", new Object[]{limit});
    }

    /** Filter by level and/or status (either may be null). */
    public List<FraudAlert> findFiltered(RiskLevel level, AlertStatus status) throws DatabaseOperationException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (level != null) {
            sql.append(" AND a.risk_level = ?");
            params.add(level.name());
        }
        if (status != null) {
            sql.append(" AND a.alert_status = ?");
            params.add(status.name());
        }
        sql.append(" ORDER BY a.id DESC");
        return query(sql.toString(), params.toArray());
    }

    public int countByStatus(AlertStatus status) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM fraud_alerts WHERE alert_status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not count alerts: " + ex.getMessage(), ex);
        }
    }

    public boolean updateStatus(int alertId, AlertStatus status) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE fraud_alerts SET alert_status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, alertId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not update alert: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean update(FraudAlert a) throws DatabaseOperationException {
        return updateStatus(a.getId(), a.getAlertStatus());
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM fraud_alerts WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not delete alert: " + ex.getMessage(), ex);
        }
    }

    private List<FraudAlert> query(String sql, Object[] params) throws DatabaseOperationException {
        List<FraudAlert> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read alerts: " + ex.getMessage(), ex);
        }
    }

    private FraudAlert map(ResultSet rs) throws SQLException {
        FraudAlert a = new FraudAlert();
        a.setId(rs.getInt("id"));
        a.setTransactionId(rs.getInt("transaction_id"));
        a.setRiskLevel(RiskLevel.parse(rs.getString("risk_level")));
        a.setReason(rs.getString("reason"));
        a.setAlertStatus(AlertStatus.parse(rs.getString("alert_status")));
        a.setCreatedAt(DateUtil.fromTimestamp(rs.getTimestamp("created_at")));
        a.setUserName(rs.getString("user_name"));
        a.setAmount(rs.getDouble("amount"));
        a.setReceiver(rs.getString("receiver"));
        int s = rs.getInt("risk_score");
        a.setRiskScore(rs.wasNull() ? null : s);
        return a;
    }
}
