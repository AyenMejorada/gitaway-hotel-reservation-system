package com.gitaway.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Database information
    private static final String URL = "jdbc:mysql://localhost:3306/hotel_reservation_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "M@rd3n4tu$110206";

    // Returns a database connection
    public static Connection getConnection() {
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
}
