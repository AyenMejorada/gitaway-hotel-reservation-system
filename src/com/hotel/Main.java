package com.hotel;

import com.hotel.db.ConnectionFactory;
import com.hotel.ui.login.LoginFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Application entry point. This is the single file that unites every
 * package (db, model, dao, service, ui.login, ui.admin, ui.customer):
 * it verifies database connectivity up front, installs a global
 * uncaught-exception handler for the Swing event thread so unexpected
 * errors are shown to the user instead of silently crashing, applies
 * a native-looking Look and Feel, and finally launches the LoginFrame
 * which is responsible for routing to either the Admin or Customer UI.
 */
public class Main {

    public static void main(String[] args) {
        // Catch any exception that escapes a Swing event handler so the
        // application shows a friendly dialog instead of dying silently.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                            "An unexpected error occurred:\n" + throwable.getMessage(),
                            "Unexpected Error", JOptionPane.ERROR_MESSAGE));
        });

        applyLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            if (!verifyDatabaseConnection()) {
                return; // verifyDatabaseConnection already showed an error dialog and the app will exit.
            }
            new LoginFrame().setVisible(true);
        });
    }

    /**
     * Performs an early connectivity check so the user gets one clear
     * message about misconfigured database settings rather than a wall
     * of stack traces the first time they click a button.
     */
    private static boolean verifyDatabaseConnection() {
        if (ConnectionFactory.testConnection()) {
            return true;
        }

        JOptionPane.showMessageDialog(null,
                "Unable to connect to the hotel_db database.\n\n"
                        + "Please make sure:\n"
                        + "  1. MySQL is running\n"
                        + "  2. The schema.sql script has been executed\n"
                        + "  3. Connection settings are correct in "
                        + "com/gitaway/database/DatabaseConnection.java (URL / USERNAME / PASSWORD)\n\n"
                        + "The application will now close.",
                "Database Connection Failed", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private static void applyLookAndFeel() {
    try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception e) {
        // Non-fatal: fall back to whatever the platform default ends up being.
        System.err.println("Could not apply cross-platform Look and Feel: " + e.getMessage());
    }
}
}
