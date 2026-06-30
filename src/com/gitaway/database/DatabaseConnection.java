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
    private static final String PASSWORD = "@Ianpogi08";

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
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS hotel_reservation_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
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
                }
            }
        } catch (Exception e) {
            System.out.println("Auto-initialization of database failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
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
