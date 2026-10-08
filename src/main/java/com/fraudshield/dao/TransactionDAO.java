package com.fraudshield.dao;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.Transaction;
import com.fraudshield.model.TransactionFilter;
import com.fraudshield.model.TransactionStatus;
import com.fraudshield.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC access for the transactions table, including search and aggregate queries. */
public class TransactionDAO implements DAOOperations<Transaction> {
    private static final String SELECT = "SELECT t.id, t.user_id, u.name AS user_name, t.amount, t.transaction_type, "
            + "t.receiver, t.transaction_time, t.device_id, t.location, t.status, t.risk_score, t.created_at "
            + "FROM transactions t JOIN users u ON u.id = t.user_id ";

    @Override
    public int create(Transaction t) throws DatabaseOperationException {
        String sql = "INSERT INTO transactions (user_id, amount, transaction_type, receiver, transaction_time, "
                + "device_id, location, status, risk_score) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getUserId());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getTransactionType());
            ps.setString(4, t.getReceiver());
            ps.setTimestamp(5, DateUtil.toTimestamp(t.getTransactionTime()));
            ps.setString(6, t.getDeviceId());
            ps.setString(7, t.getLocation());
            ps.setString(8, t.getStatus().name());
            if (t.getRiskScore() == null) ps.setNull(9, java.sql.Types.INTEGER);
            else ps.setInt(9, t.getRiskScore());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setId(keys.getInt(1));
                    return t.getId();
                }
                return -1;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not save transaction: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Transaction> findById(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE t.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read transaction: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseOperationException {
        return list(SELECT + "ORDER BY t.id DESC", new Object[0]);
    }

    public List<Transaction> findRecent(int limit) throws DatabaseOperationException {
        return list(SELECT + "ORDER BY t.id DESC LIMIT ?", new Object[]{limit});
    }

    public List<Transaction> findByUserId(int userId, int limit) throws DatabaseOperationException {
        return list(SELECT + "WHERE t.user_id = ? ORDER BY t.id DESC LIMIT ?", new Object[]{userId, limit});
    }

    /** APPROVED transactions of a customer older than beforeId - the trusted behavioural baseline. */
    public List<Transaction> findApprovedHistory(int userId, int beforeId, int limit) throws DatabaseOperationException {
        return list(SELECT + "WHERE t.user_id = ? AND t.status = 'APPROVED' AND t.id < ? ORDER BY t.id DESC LIMIT ?",
                new Object[]{userId, beforeId, limit});
    }

    /** Transactions still waiting for analysis, oldest first (used for crash recovery / seeding). */
    public List<Transaction> findPending() throws DatabaseOperationException {
        return list(SELECT + "WHERE t.status = 'PENDING' ORDER BY t.transaction_time, t.id", new Object[0]);
    }

    /**
     * Counts earlier transactions (id &lt; beforeId) of the same user whose time lies in [from, to].
     * Used for velocity checks ("how many payments in the last 10 minutes / hour / day").
     */
    public int countInWindow(int userId, int beforeId, LocalDateTime from, LocalDateTime to)
            throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND id < ? "
                + "AND transaction_time >= ? AND transaction_time <= ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, beforeId);
            ps.setTimestamp(3, Timestamp.valueOf(from));
            ps.setTimestamp(4, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not count transactions: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean update(Transaction t) throws DatabaseOperationException {
        String sql = "UPDATE transactions SET amount=?, transaction_type=?, receiver=?, transaction_time=?, "
                + "device_id=?, location=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, t.getAmount());
            ps.setString(2, t.getTransactionType());
            ps.setString(3, t.getReceiver());
            ps.setTimestamp(4, DateUtil.toTimestamp(t.getTransactionTime()));
            ps.setString(5, t.getDeviceId());
            ps.setString(6, t.getLocation());
            ps.setInt(7, t.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not update transaction: " + ex.getMessage(), ex);
        }
    }

    public boolean updateStatus(int id, TransactionStatus status) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection()) {
            return updateStatus(c, id, status);
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not update status: " + ex.getMessage(), ex);
        }
    }

    public boolean updateStatus(Connection c, int id, TransactionStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE transactions SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Writes the analysis outcome using the caller's connection so it joins a JDBC transaction. */
    public boolean updateStatusAndScore(Connection c, int id, TransactionStatus status, int score) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE transactions SET status = ?, risk_score = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, score);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not delete transaction: " + ex.getMessage(), ex);
        }
    }

    // ---------------------------------------------------------------- search

    public List<Transaction> search(TransactionFilter f) throws DatabaseOperationException {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(f, params);
        params.add(f.getPageSize());
        params.add(f.getOffset());
        return list(SELECT + where + " ORDER BY t.id DESC LIMIT ? OFFSET ?", params.toArray());
    }

    public int countSearch(TransactionFilter f) throws DatabaseOperationException {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(f, params);
        String sql = "SELECT COUNT(*) FROM transactions t JOIN users u ON u.id = t.user_id " + where;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindAll(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not count transactions: " + ex.getMessage(), ex);
        }
    }

    /** Builds a WHERE clause using only placeholders - user text never enters the SQL string. */
    private String buildWhere(TransactionFilter f, List<Object> params) {
        StringBuilder w = new StringBuilder("WHERE 1=1");
        if (f.getQuery() != null && !f.getQuery().isBlank()) {
            String like = "%" + f.getQuery().trim() + "%";
            w.append(" AND (t.receiver LIKE ? OR u.name LIKE ? OR t.device_id LIKE ? OR t.location LIKE ? OR t.id = ?)");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(Transaction.idFromReference(f.getQuery()));
        }
        if (f.getRiskLevel() != null) {
            switch (f.getRiskLevel()) {
                case LOW -> w.append(" AND t.risk_score <= 30");
                case MEDIUM -> w.append(" AND t.risk_score BETWEEN 31 AND 60");
                case HIGH -> w.append(" AND t.risk_score BETWEEN 61 AND 80");
                case CRITICAL -> w.append(" AND t.risk_score > 80");
            }
        }
        if (f.getStatus() != null) {
            w.append(" AND t.status = ?");
            params.add(f.getStatus().name());
        }
        if (f.getType() != null && !f.getType().isBlank()) {
            w.append(" AND t.transaction_type = ?");
            params.add(f.getType());
        }
        if (f.getFromDate() != null) {
            w.append(" AND t.transaction_time >= ?");
            params.add(Timestamp.valueOf(f.getFromDate().atStartOfDay()));
        }
        if (f.getToDate() != null) {
            w.append(" AND t.transaction_time < ?");
            params.add(Timestamp.valueOf(f.getToDate().plusDays(1).atStartOfDay()));
        }
        if (f.getMinAmount() != null) {
            w.append(" AND t.amount >= ?");
            params.add(f.getMinAmount());
        }
        if (f.getMaxAmount() != null) {
            w.append(" AND t.amount <= ?");
            params.add(f.getMaxAmount());
        }
        return w.toString();
    }

    // ------------------------------------------------------------ aggregates

    /** Keys: TOTAL, PENDING, LOW, MEDIUM, HIGH, CRITICAL. */
    public Map<String, Integer> getRiskCounts() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*), COALESCE(SUM(risk_score IS NULL),0), COALESCE(SUM(risk_score <= 30),0), "
                + "COALESCE(SUM(risk_score BETWEEN 31 AND 60),0), COALESCE(SUM(risk_score BETWEEN 61 AND 80),0), "
                + "COALESCE(SUM(risk_score > 80),0) FROM transactions";
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                m.put("TOTAL", rs.getInt(1));
                m.put("PENDING", rs.getInt(2));
                m.put("LOW", rs.getInt(3));
                m.put("MEDIUM", rs.getInt(4));
                m.put("HIGH", rs.getInt(5));
                m.put("CRITICAL", rs.getInt(6));
            }
            return m;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not compute risk counts: " + ex.getMessage(), ex);
        }
    }

    public double getAverageRiskScore() throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(AVG(risk_score),0) FROM transactions WHERE risk_score IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not compute average risk: " + ex.getMessage(), ex);
        }
    }

    /** Transactions per hour of day (0-23) - key = hour. */
    public Map<Integer, Integer> countByHour() throws DatabaseOperationException {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) m.put(h, 0);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT HOUR(transaction_time) h, COUNT(*) FROM transactions GROUP BY h");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getInt(1), rs.getInt(2));
            return m;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not group by hour: " + ex.getMessage(), ex);
        }
    }

    /** Count and average risk per transaction type: type -> {count, avgRisk}. */
    public Map<String, double[]> statsByType() throws DatabaseOperationException {
        Map<String, double[]> m = new LinkedHashMap<>();
        String sql = "SELECT transaction_type, COUNT(*), COALESCE(AVG(risk_score),0) FROM transactions GROUP BY transaction_type ORDER BY 2 DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString(1), new double[]{rs.getInt(2), rs.getDouble(3)});
            return m;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not group by type: " + ex.getMessage(), ex);
        }
    }

    /** Risk per location: location -> {count, avgRisk}, top 6 by average risk. */
    public Map<String, double[]> statsByLocation() throws DatabaseOperationException {
        Map<String, double[]> m = new LinkedHashMap<>();
        String sql = "SELECT location, COUNT(*), COALESCE(AVG(risk_score),0) a FROM transactions WHERE risk_score IS NOT NULL "
                + "GROUP BY location ORDER BY a DESC LIMIT 6";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString(1), new double[]{rs.getInt(2), rs.getDouble(3)});
            return m;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not group by location: " + ex.getMessage(), ex);
        }
    }

    // --------------------------------------------------------------- helpers

    private List<Transaction> list(String sql, Object[] params) throws DatabaseOperationException {
        List<Transaction> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read transactions: " + ex.getMessage(), ex);
        }
    }

    private void bindAll(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setUserName(rs.getString("user_name"));
        t.setAmount(rs.getDouble("amount"));
        t.setTransactionType(rs.getString("transaction_type"));
        t.setReceiver(rs.getString("receiver"));
        t.setTransactionTime(DateUtil.fromTimestamp(rs.getTimestamp("transaction_time")));
        t.setDeviceId(rs.getString("device_id"));
        t.setLocation(rs.getString("location"));
        t.setStatus(TransactionStatus.parse(rs.getString("status")));
        int score = rs.getInt("risk_score");
        t.setRiskScore(rs.wasNull() ? null : score);
        t.setCreatedAt(DateUtil.fromTimestamp(rs.getTimestamp("created_at")));
        return t;
    }
}
