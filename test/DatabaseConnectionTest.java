package test;

import com.gitaway.database.DatabaseConnection;
import com.hotel.db.ConnectionFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Standalone connectivity check for the hotel reservation database.
 * <p>
 * This is a plain class with a {@code main()} method (no JUnit dependency
 * required) so it can be run directly from an IDE like NetBeans 8.2:
 * right-click this file -&gt; "Run File", or right-click -&gt; "Run" if it's
 * set as the project's main class.
 * <p>
 * It performs three checks, each printed clearly to the console:
 * <ol>
 * <li>Can {@link DatabaseConnection#getConnection()} open a raw
 * connection?</li>
 * <li>Does {@link ConnectionFactory#testConnection()} (the helper used by
 * the rest of the app) agree?</li>
 * <li>Can a simple query actually run against the {@code hotel_reservation_db}
 * schema (confirms the database/tables from schema.sql actually exist,
 * not just that MySQL itself is reachable)?</li>
 * </ol>
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" Hotel Reservation System - Database Connection Test");
        System.out.println("=================================================");

        boolean rawConnectionOk = testRawConnection();
        boolean factoryCheckOk = testConnectionFactory();
        boolean queryOk = testSampleQuery();

        System.out.println("-------------------------------------------------");
        if (rawConnectionOk && factoryCheckOk && queryOk) {
            System.out.println("RESULT: All checks passed. Database is reachable and ready.");
        } else {
            System.out.println("RESULT: One or more checks FAILED. See messages above for details.");
            System.out.println("Common causes:");
            System.out.println("  - MySQL service is not running");
            System.out.println("  - schema.sql has not been executed yet");
            System.out
                    .println("  - URL / USERNAME / PASSWORD in DatabaseConnection.java do not match your MySQL setup");
        }
        System.out.println("=================================================");
    }

    /** Step 1: directly call the user-provided DatabaseConnection class. */
    private static boolean testRawConnection() {
        System.out.println("\n[1/3] Testing DatabaseConnection.getConnection() ...");
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.out.println("  FAILED: getConnection() returned null. "
                        + "Check MySQL is running and credentials in DatabaseConnection.java are correct.");
                return false;
            }
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("  OK: Connected to " + meta.getURL());
            System.out.println("       Database product: " + meta.getDatabaseProductName()
                    + " " + meta.getDatabaseProductVersion());
            return true;
        } catch (SQLException e) {
            System.out.println("  FAILED: SQLException while reading connection metadata: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    /** Step 2: confirm the app's own ConnectionFactory wrapper agrees. */
    private static boolean testConnectionFactory() {
        System.out.println("\n[2/3] Testing ConnectionFactory.testConnection() ...");
        boolean ok = ConnectionFactory.testConnection();
        System.out.println(ok ? "  OK: ConnectionFactory reports the database is reachable."
                : "  FAILED: ConnectionFactory reports the database is NOT reachable.");
        return ok;
    }

    /** Step 3: run a real query to confirm the schema/tables actually exist. */
    private static boolean testSampleQuery() {
        System.out.println("\n[3/3] Testing a sample query against the 'users' table ...");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.out.println("  SKIPPED: no connection available.");
                return false;
            }
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users");
            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("  OK: 'users' table exists and contains " + total + " row(s).");
                if (total == 0) {
                    System.out.println("       NOTE: table is empty - make sure schema.sql's INSERT "
                            + "statements were executed (seed accounts: admin/admin123, customer/customer123).");
                }
                return true;
            }
            System.out.println("  FAILED: query returned no result.");
            return false;
        } catch (SQLException e) {
            System.out.println("  FAILED: " + e.getMessage());
            System.out.println("       This usually means the database/tables from schema.sql don't exist yet.");
            return false;
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // Nothing useful to do here; this is best-effort cleanup for a test utility.
        }
    }
}