package com.hotel.ui.common;

import com.hotel.exception.HotelException;

import javax.swing.*;
import java.awt.*;

/**
 * Shared UI helpers used across login, admin, and customer modules so that
 * dialogs, fonts, and colors stay consistent without duplicating Swing
 * boilerplate in every screen.
 */
public final class UIUtils {

    public static final Color PRIMARY_COLOR = new Color(25, 55, 95);
    public static final Color ACCENT_COLOR = new Color(0, 120, 215);
    public static final Color SUCCESS_COLOR = new Color(34, 139, 34);
    public static final Color DANGER_COLOR = new Color(178, 34, 34);
    public static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    public static final Color SIDEBAR_COLOR = new Color(33, 47, 61);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private UIUtils() {
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Runs the given action and displays a friendly error dialog if a
     * {@link HotelException} (or subclass) is thrown, instead of letting it
     * propagate and crash the UI thread. Use this to wrap every button
     * handler that calls into the service layer.
     */
    public static void runSafely(Component parent, Runnable action) {
        try {
            action.run();
        } catch (HotelException e) {
            showError(parent, e.getMessage());
        } catch (Exception e) {
            showError(parent, "An unexpected error occurred: " + e.getMessage());
        }
    }

    public static JButton createStyledButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 36));
        return button;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(FONT_REGULAR);
        table.setSelectionBackground(ACCENT_COLOR.brighter());
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(220, 220, 220));
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
    }

    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(PRIMARY_COLOR);
        return label;
    }
}
