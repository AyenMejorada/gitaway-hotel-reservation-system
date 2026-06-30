package com.hotel.db;

import com.gitaway.database.DatabaseConnection;
import com.hotel.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

public final class TransactionManager {

    private static final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> inTransaction = new ThreadLocal<>();

    private TransactionManager() {
    }

    public static void begin() {
        if (isTransactionActive()) {
            throw new DatabaseException("A transaction is already active on this thread.");
        }
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) {
                throw new DatabaseException(
                        "Unable to connect to the database. Verify MySQL settings in DatabaseConnection.java.");
            }
            conn.setAutoCommit(false);
            threadConnection.set(conn);
            inTransaction.set(true);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to begin transaction.", e);
        }
    }

    public static void commit() {
        Connection conn = threadConnection.get();
        if (conn == null) {
            throw new DatabaseException("No active transaction to commit.");
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to commit transaction.", e);
        } finally {
            closeAndClean();
        }
    }

    public static void rollback() {
        Connection conn = threadConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("Rollback failed: " + e.getMessage());
            } finally {
                closeAndClean();
            }
        }
    }

    private static void closeAndClean() {
        Connection conn = threadConnection.get();
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
        threadConnection.remove();
        inTransaction.remove();
    }

    public static boolean isTransactionActive() {
        return Boolean.TRUE.equals(inTransaction.get());
    }

    public static Connection getConnection() {
        if (isTransactionActive()) {
            Connection physicalConn = threadConnection.get();
            if (physicalConn == null) {
                return null;
            }
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName())) {
                            // Intercept close so try-with-resources does not close the physical connection
                            return null;
                        }
                        try {
                            return method.invoke(physicalConn, args);
                        } catch (java.lang.reflect.InvocationTargetException ite) {
                            throw ite.getCause();
                        }
                    }
            );
        }
        return null;
    }
}
