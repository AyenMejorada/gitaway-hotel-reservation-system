package com.gitaway.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    // Database information
    private static final String URL = "jdbc:mysql://localhost:3306/hotel_reservation_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "M@rd3n4tu$110206";

    private static boolean initialized = false;

    // Returns a database connection
    public static synchronized Connection getConnection() {
        if (!initialized) {
            initialized = true;
            tryInitDatabase();
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Database Connected Successfully!");
            return conn;
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL Driver Not Found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Database Connection Failed!");
            e.printStackTrace();
        }
        return null;
    }

    private static void tryInitDatabase() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Connect to server (no DB name in URL)
            String serverUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true";
            conn = DriverManager.getConnection(serverUrl, USERNAME, PASSWORD);

            // Create database if not exists
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "CREATE DATABASE IF NOT EXISTS hotel_reservation_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }

            // Switch to the database and check if tables exist
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE hotel_reservation_db");
                // Check if users table exists
                boolean tablesExist = false;
                try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'users'")) {
                    if (rs.next()) {
                        tablesExist = true;
                    }
                }

                if (!tablesExist) {
                    System.out.println("Database tables not found. Initializing schema...");
                    executeSqlScript(stmt, "schema.sql");
                    System.out.println("Database schema initialized successfully.");
                } else {
                    // Run migrations
                    try {
                        boolean needsBillingRecreate = false;
                        try (ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM billing LIKE 'payment_status'")) {
                            if (rs.next()) {
                                needsBillingRecreate = true;
                            }
                        }
                        if (needsBillingRecreate) {
                            System.out.println("Outdated billing table schema detected. Dropping old billing table...");
                            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                            stmt.executeUpdate("DROP TABLE IF EXISTS billing");
                            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
                            System.out.println("Re-initializing schema to create correct billing table...");
                            executeSqlScript(stmt, "schema.sql");
                        }
                    } catch (Exception ex) {
                        System.out.println("Migration warning (billing table update check): " + ex.getMessage());
                    }

                    try {
                        stmt.executeUpdate("ALTER TABLE reservations MODIFY COLUMN room_id INT NULL");
                    } catch (Exception ex) {
                        System.out.println("Migration warning (modify room_id): " + ex.getMessage());
                    }

                    try {
                        stmt.executeUpdate("ALTER TABLE rooms MODIFY COLUMN status ENUM('AVAILABLE', 'RESERVED', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE'");
                    } catch (Exception ex) {
                        System.out.println("Migration warning (modify rooms status enum): " + ex.getMessage());
                    }

                    try {
                        // Fix the query to avoid using non-existent column 'full_name' in guests table
                        stmt.executeUpdate("UPDATE guests SET phone = '09175557456' WHERE CONCAT(first_name, ' ', last_name) = 'Juan Dela Cruz'");
                    } catch (Exception ex) {
                        System.out.println("Migration warning (update Juan Dela Cruz phone): " + ex.getMessage());
                    }

                    try {
                        stmt.executeUpdate("INSERT IGNORE INTO users (username, password, role, full_name, email) "
                                + "VALUES ('guest', 'guest123', 'CUSTOMER', 'Guest User', 'guest@example.com')");
                    } catch (Exception ex) {
                        System.out.println("Migration warning (add guest user): " + ex.getMessage());
                    }

                    // Seed standard 80 rooms if not already there
                    try {
                        for (int i = 101; i <= 120; i++) {
                            stmt.executeUpdate("INSERT IGNORE INTO rooms (room_number, room_type, price_per_night, capacity, status, description) VALUES ('" + i + "', 'SINGLE', 1500.00, 1, 'AVAILABLE', 'Cozy single room')");
                        }
                        for (int i = 201; i <= 220; i++) {
                            stmt.executeUpdate("INSERT IGNORE INTO rooms (room_number, room_type, price_per_night, capacity, status, description) VALUES ('" + i + "', 'DOUBLE', 2500.00, 2, 'AVAILABLE', 'Spacious double room')");
                        }
                        for (int i = 301; i <= 320; i++) {
                            stmt.executeUpdate("INSERT IGNORE INTO rooms (room_number, room_type, price_per_night, capacity, status, description) VALUES ('" + i + "', 'DELUXE', 4000.00, 3, 'AVAILABLE', 'Deluxe room')");
                        }
                        for (int i = 401; i <= 420; i++) {
                            stmt.executeUpdate("INSERT IGNORE INTO rooms (room_number, room_type, price_per_night, capacity, status, description) VALUES ('" + i + "', 'SUITE', 7000.00, 4, 'AVAILABLE', 'Executive suite')");
                        }
                    } catch (Exception ex) {
                        System.out.println("Migration warning (seed rooms): " + ex.getMessage());
                    }

                    // Seed sample guests and reservations if none exist
                    try {
                        boolean reservationsExist = false;
                        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reservations")) {
                            if (rs.next() && rs.getInt(1) >= 15) {
                                reservationsExist = true;
                            }
                        }
                        if (!reservationsExist) {
                            System.out.println("Under 15 reservations found. Re-seeding clean sample guests and reservations...");
                            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                            stmt.executeUpdate("TRUNCATE TABLE billing");
                            stmt.executeUpdate("TRUNCATE TABLE reservations");
                            stmt.executeUpdate("TRUNCATE TABLE guests");
                            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
                            
                            // Insert guests
                            String[] guestSqls = {
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('John', 'Doe', 'john.doe@email.com', '09171112222', 'Manila', 'ID-1001')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Alice', 'Smith', 'alice.s@email.com', '09172223333', 'Quezon City', 'ID-1002')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Bob', 'Johnson', 'bob.j@email.com', '09173334444', 'Cebu', 'ID-1003')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Emma', 'Brown', 'emma.b@email.com', '09174445555', 'Davao', 'ID-1004')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Michael', 'Davis', 'michael.d@email.com', '09175556666', 'Makati', 'ID-1005')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Jessica', 'Wilson', 'jess.w@email.com', '09176667777', 'Pasig', 'ID-1006')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('David', 'Taylor', 'david.t@email.com', '09177778888', 'Taguig', 'ID-1007')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Sarah', 'Thomas', 'sarah.t@email.com', '09178889999', 'Alabang', 'ID-1008')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('James', 'Anderson', 'james.a@email.com', '09179990000', 'Baguio', 'ID-1009')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Taylor', 'Martinez', 'taylor.m@email.com', '09170001111', 'Iloilo', 'ID-1010')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Robert', 'White', 'robert.w@email.com', '09171113333', 'Bacoor', 'ID-1011')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Mary', 'Clark', 'mary.c@email.com', '09172224444', 'Cagayan de Oro', 'ID-1012')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Joseph', 'Lewis', 'joseph.l@email.com', '09173335555', 'Angeles', 'ID-1013')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Linda', 'Rodriguez', 'linda.r@email.com', '09174446666', 'Antipolo', 'ID-1014')",
                                "INSERT IGNORE INTO guests (first_name, last_name, email, phone, address, id_number) VALUES ('Daniel', 'Lee', 'daniel.l@email.com', '09175557777', 'Dumaguete', 'ID-1015')"
                            };
                            for (String sql : guestSqls) {
                                stmt.executeUpdate(sql);
                            }
                            
                            String[] reservationSqls = {
                                // Completed reservations (in the past, room status AVAILABLE)
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='John' AND last_name='Doe'), 'SINGLE', (SELECT room_id FROM rooms WHERE room_number='101'), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), 1, 'CHECKED_OUT', 4500.00, 'Regular guest. Cleaned and checked out.')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Alice' AND last_name='Smith'), 'DOUBLE', (SELECT room_id FROM rooms WHERE room_number='201'), DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), 2, 'CHECKED_OUT', 7500.00, 'Loved the balcony view')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Bob' AND last_name='Johnson'), 'DELUXE', (SELECT room_id FROM rooms WHERE room_number='301'), DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), 3, 'CHECKED_OUT', 16000.00, 'Family vacation')",
                                
                                // Checked-in reservations (active now, room status should be OCCUPIED)
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Emma' AND last_name='Brown'), 'SINGLE', (SELECT room_id FROM rooms WHERE room_number='102'), DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), 1, 'CHECKED_IN', 6000.00, 'Requires late checkout')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Michael' AND last_name='Davis'), 'DOUBLE', (SELECT room_id FROM rooms WHERE room_number='202'), DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 2, 'CHECKED_IN', 10000.00, 'Corporate traveler')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Jessica' AND last_name='Wilson'), 'DELUXE', (SELECT room_id FROM rooms WHERE room_number='302'), DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 2, 'CHECKED_IN', 16000.00, 'Anniversary stay')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='David' AND last_name='Taylor'), 'SUITE', (SELECT room_id FROM rooms WHERE room_number='401'), DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 4, 'CHECKED_IN', 35000.00, 'VIP treatment')",
                                
                                // Confirmed/Reserved reservations (upcoming check-in, room status should be RESERVED)
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Sarah' AND last_name='Thomas'), 'SINGLE', (SELECT room_id FROM rooms WHERE room_number='103'), DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 1, 'CONFIRMED', 3000.00, 'Arriving in morning')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='James' AND last_name='Anderson'), 'DOUBLE', (SELECT room_id FROM rooms WHERE room_number='203'), DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 2, 'CONFIRMED', 7500.00, 'Requested twin beds')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Taylor' AND last_name='Martinez'), 'DELUXE', (SELECT room_id FROM rooms WHERE room_number='303'), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 3, 'CONFIRMED', 12000.00, 'No extra bed needed')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Robert' AND last_name='White'), 'SUITE', (SELECT room_id FROM rooms WHERE room_number='402'), DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), 3, 'CONFIRMED', 28000.00, 'Honeymoon couple')",
                                
                                // Pending reservations (no room assigned yet, room_id = null)
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Mary' AND last_name='Clark'), 'SINGLE', NULL, DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), 1, 'PENDING', 3000.00, 'Awaiting approval')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Joseph' AND last_name='Lewis'), 'DOUBLE', NULL, DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 2, 'PENDING', 7500.00, 'Pending payment confirmation')",
                                
                                // Cancelled reservations (room status AVAILABLE)
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Linda' AND last_name='Rodriguez'), 'DELUXE', (SELECT room_id FROM rooms WHERE room_number='304'), DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), 2, 'CANCELLED', 8000.00, 'Cancelled due to flight change')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Daniel' AND last_name='Lee'), 'SUITE', (SELECT room_id FROM rooms WHERE room_number='403'), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 4, 'CANCELLED', 28000.00, 'Duplicate booking')",
                                
                                // Additional dynamic records to hit 20-25 reservations
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='John' AND last_name='Doe'), 'DOUBLE', (SELECT room_id FROM rooms WHERE room_number='204'), DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 2, 'CHECKED_OUT', 5000.00, 'Quiet room preferred')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Alice' AND last_name='Smith'), 'DELUXE', (SELECT room_id FROM rooms WHERE room_number='305'), DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 3, 'CHECKED_OUT', 8000.00, 'Business trip')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Bob' AND last_name='Johnson'), 'SUITE', (SELECT room_id FROM rooms WHERE room_number='404'), DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 4, 'CHECKED_OUT', 28000.00, 'Luxury retreat')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Emma' AND last_name='Brown'), 'DOUBLE', (SELECT room_id FROM rooms WHERE room_number='205'), DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 2, 'CONFIRMED', 5000.00, 'Anniversary bouquet requested')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Michael' AND last_name='Davis'), 'DELUXE', (SELECT room_id FROM rooms WHERE room_number='306'), DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 11 DAY), 3, 'CONFIRMED', 12000.00, 'Company event')",
                                
                                "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, num_guests, status, total_amount, notes) VALUES " +
                                "((SELECT guest_id FROM guests WHERE first_name='Jessica' AND last_name='Wilson'), 'SUITE', (SELECT room_id FROM rooms WHERE room_number='405'), DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 4, 'CONFIRMED', 28000.00, 'Late check-in requested')"
                            };
                            for (String sql : reservationSqls) {
                                stmt.executeUpdate(sql);
                            }
                            
                            // Align room status in rooms table according to active reservations
                            stmt.executeUpdate("UPDATE rooms SET status = 'OCCUPIED' WHERE room_id IN (SELECT room_id FROM reservations WHERE status = 'CHECKED_IN' AND room_id IS NOT NULL)");
                            stmt.executeUpdate("UPDATE rooms SET status = 'RESERVED' WHERE room_id IN (SELECT room_id FROM reservations WHERE status = 'CONFIRMED' AND room_id IS NOT NULL)");
                            
                            System.out.println("Sample reservation data seeded successfully.");
                            
                            // Automatically seed billing records for CHECKED_OUT reservations
                            try {
                                try (ResultSet rs = stmt.executeQuery("SELECT reservation_id, total_amount FROM reservations WHERE status = 'CHECKED_OUT'")) {
                                    java.util.List<Integer> resIds = new java.util.ArrayList<>();
                                    java.util.List<java.math.BigDecimal> amounts = new java.util.ArrayList<>();
                                    while (rs.next()) {
                                        resIds.add(rs.getInt("reservation_id"));
                                        amounts.add(rs.getBigDecimal("total_amount"));
                                    }
                                    for (int j = 0; j < resIds.size(); j++) {
                                        int rId = resIds.get(j);
                                        java.math.BigDecimal amt = amounts.get(j);
                                        stmt.executeUpdate("INSERT IGNORE INTO billing (reservation_id, billing_date, room_charges, additional_charges, discount, tax, total_amount, bill_status) " +
                                                "VALUES (" + rId + ", CURRENT_DATE, " + amt + ", 0.00, 0.00, 0.00, " + amt + ", 'GENERATED')");
                                    }
                                }
                                System.out.println("Seeded billing records for CHECKED_OUT reservations successfully.");
                            } catch (Exception ex) {
                                System.out.println("Migration warning (seed billing from reservations): " + ex.getMessage());
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Migration warning (seed reservations/guests): " + ex.getMessage());
                    }

                    boolean roomTypeColExists = false;
                    try (ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM reservations LIKE 'room_type'")) {
                        if (rs.next()) {
                            roomTypeColExists = true;
                        }
                    }
                    if (!roomTypeColExists) {
                        try {
                            stmt.executeUpdate("ALTER TABLE reservations ADD COLUMN room_type ENUM('SINGLE', 'DOUBLE', 'DELUXE', 'SUITE') NOT NULL AFTER guest_id");
                            System.out.println("Successfully added room_type to reservations table.");
                        } catch (Exception ex) {
                            System.out.println("Migration error (add room_type): " + ex.getMessage());
                        }
                    }
                    // Handled above.
                }
            }
        } catch (Exception e) {
            System.out.println("Auto-initialization of database failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static void executeSqlScript(Statement stmt, String fileName) throws Exception {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Schema file not found at " + file.getAbsolutePath());
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--") || line.trim().startsWith("//") || line.trim().isEmpty()) {
                    continue;
                }
                sb.append(line).append("\n");
            }
        }

        String[] statements = sb.toString().split(";");
        for (String sql : statements) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty()) {
                stmt.execute(trimmed);
            }
        }
    }
}
