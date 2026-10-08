package com.fraudshield.dao;

import com.fraudshield.db.DBConnection;
import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.model.RiskFactor;
import com.fraudshield.model.RiskResult;
import com.fraudshield.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC access for risk_analysis. The six factor scores are stored in their own columns
 * (for SQL analytics) and the full factor list is stored as text so the explanation can be rebuilt.
 */
public class RiskAnalysisDAO implements DAOOperations<RiskResult> {
    private static final String COLS = "transaction_id, amount_score, frequency_score, time_score, device_score, "
            + "location_score, behavior_score, rule_score, ml_score, final_score, risk_level, analysis_method, "
            + "explanation, factor_details, created_at";

    @Override
    public int create(RiskResult r) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection()) {
            return save(c, r);
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not save risk analysis: " + ex.getMessage(), ex);
        }
    }

    /** Saves (or replaces) the analysis of a transaction using the caller's connection. */
    public int save(Connection c, RiskResult r) throws SQLException {
        String sql = "INSERT INTO risk_analysis (transaction_id, amount_score, frequency_score, time_score, device_score, "
                + "location_score, behavior_score, rule_score, ml_score, final_score, risk_level, analysis_method, "
                + "explanation, factor_details) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE amount_score=VALUES(amount_score), frequency_score=VALUES(frequency_score), "
                + "time_score=VALUES(time_score), device_score=VALUES(device_score), location_score=VALUES(location_score), "
                + "behavior_score=VALUES(behavior_score), rule_score=VALUES(rule_score), ml_score=VALUES(ml_score), "
                + "final_score=VALUES(final_score), risk_level=VALUES(risk_level), analysis_method=VALUES(analysis_method), "
                + "explanation=VALUES(explanation), factor_details=VALUES(factor_details), created_at=CURRENT_TIMESTAMP";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getTransactionId());
            ps.setInt(2, r.scoreOf("AMOUNT"));
            ps.setInt(3, r.scoreOf("FREQUENCY"));
            ps.setInt(4, r.scoreOf("TIME"));
            ps.setInt(5, r.scoreOf("DEVICE"));
            ps.setInt(6, r.scoreOf("LOCATION"));
            ps.setInt(7, r.scoreOf("BEHAVIOR"));
            ps.setInt(8, r.getRuleScore());
            if (r.getMlScore() == null) ps.setNull(9, java.sql.Types.INTEGER);
            else ps.setInt(9, r.getMlScore());
            ps.setInt(10, r.getFinalScore());
            ps.setString(11, r.getRiskLevel().name());
            ps.setString(12, r.getAnalysisMethod());
            ps.setString(13, r.getExplanation());
            ps.setString(14, serializeFactors(r));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** The stored analysis for a transaction (rebuilt into a RiskResult), if one exists. */
    public Optional<RiskResult> findByTransactionId(int transactionId) throws DatabaseOperationException {
        String sql = "SELECT " + COLS + " FROM risk_analysis WHERE transaction_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read risk analysis: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<RiskResult> findById(int id) throws DatabaseOperationException {
        String sql = "SELECT " + COLS + " FROM risk_analysis WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not read risk analysis: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<RiskResult> findAll() throws DatabaseOperationException {
        List<RiskResult> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM risk_analysis ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not list risk analyses: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean update(RiskResult r) throws DatabaseOperationException {
        create(r);
        return true;
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM risk_analysis WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not delete risk analysis: " + ex.getMessage(), ex);
        }
    }

    /** Average score (0-100) of every factor across all stored analyses; key = factor column name. */
    public Map<String, Double> averageFactorScores() throws DatabaseOperationException {
        String sql = "SELECT COALESCE(AVG(amount_score),0), COALESCE(AVG(frequency_score),0), COALESCE(AVG(time_score),0), "
                + "COALESCE(AVG(device_score),0), COALESCE(AVG(location_score),0), COALESCE(AVG(behavior_score),0), "
                + "COALESCE(AVG(ml_score),0) FROM risk_analysis";
        String[] names = {"Amount", "Frequency", "Time", "Device", "Location", "Behaviour", "ML probability"};
        Map<String, Double> m = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                for (int i = 0; i < names.length; i++) m.put(names[i], rs.getDouble(i + 1));
            }
            return m;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Could not average factor scores: " + ex.getMessage(), ex);
        }
    }

    private String serializeFactors(RiskResult r) {
        StringBuilder sb = new StringBuilder();
        for (RiskFactor f : r.getFactors()) sb.append(f.serialize()).append('\n');
        return sb.toString();
    }

    private RiskResult map(ResultSet rs) throws SQLException {
        RiskResult r = new RiskResult();
        r.setTransactionId(rs.getInt("transaction_id"));
        r.setFinalScore(rs.getInt("final_score"));
        r.setRuleScore(rs.getInt("rule_score"));
        int ml = rs.getInt("ml_score");
        r.setMlScore(rs.wasNull() ? null : ml);
        r.setAnalysisMethod(rs.getString("analysis_method"));
        r.setExplanation(rs.getString("explanation"));
        r.setAnalyzedAt(DateUtil.fromTimestamp(rs.getTimestamp("created_at")));
        String details = rs.getString("factor_details");
        if (details != null) {
            for (String line : details.split("\n")) {
                if (!line.isBlank()) r.addFactor(RiskFactor.deserialize(line));
            }
        }
        return r;
    }
}
