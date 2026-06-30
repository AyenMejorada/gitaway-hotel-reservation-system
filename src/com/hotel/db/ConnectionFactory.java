package com.hotel.db;

import com.gitaway.database.DatabaseConnection;
import com.hotel.exception.DatabaseException;

import java.sql.Connection;

/**
 * Thin adapter around {@link DatabaseConnection#getConnection()}.
 * <p>
 * {@code DatabaseConnection.getConnection()} returns {@code null} on
 * failure (and prints a stack trace) rather than throwing. DAO classes
 * use try-with-resources, which requires a non-null {@link Connection}
 * (or a clean failure) — so this factory calls the user-supplied
 * connection class and converts a {@code null} result into a
 * {@link DatabaseException}, which the UI layer already knows how to
 * catch and display via {@code UIUtils.runSafely(...)}.
 */
public final class ConnectionFactory {

    private ConnectionFactory() {
    }

    public static Connection getConnection() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            throw new DatabaseException(
                    "Unable to connect to the database. Please verify that MySQL is running "
                            + "and that the connection settings in DatabaseConnection.java are correct.");
        }
        return conn;
    }

    /**
     * Quick connectivity check used at application startup so the user gets
     * an immediate, friendly error instead of failures scattered later on.
     * Closes the connection immediately after verifying it.
     */
    public static boolean testConnection() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            return false;
        }
        try {
            conn.close();
        } catch (java.sql.SQLException ignored) {
            // Connection was valid enough to prove connectivity; closing failure is not fatal here.
        }
        return true;
    }
}
