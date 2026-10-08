package com.fraudshield.db;

import com.fraudshield.exception.DatabaseOperationException;
import com.fraudshield.util.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single place that creates JDBC connections. Credentials come from AppConfig
 * (application.properties / environment variables) - never hard-coded.
 * Callers MUST close the connection (try-with-resources).
 */
public final class DBConnection {
    static {
        try {
            Class.forName(AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError("JDBC driver not found: " + ex.getMessage());
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws DatabaseOperationException {
        try {
            return DriverManager.getConnection(
                    AppConfig.get("db.url", ""),
                    AppConfig.get("db.user", ""),
                    AppConfig.get("db.password", ""));
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Cannot connect to the database: " + ex.getMessage(), ex);
        }
    }

    /** Quick health check used by the dashboard and error handling. */
    public static boolean isAvailable() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (DatabaseOperationException | SQLException ex) {
            return false;
        }
    }
}
