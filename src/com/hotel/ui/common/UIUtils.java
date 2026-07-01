package com.hotel.ui.common;

import com.hotel.exception.HotelException;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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

        // Align header text to match corresponding cell alignments
        table.getTableHeader().setDefaultRenderer(new javax.swing.table.TableCellRenderer() {
            private final javax.swing.table.TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component comp = defaultRenderer.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    label.setFont(UIUtils.FONT_BOLD);
                    label.setBackground(UIUtils.PRIMARY_COLOR);
                    label.setForeground(Color.WHITE);
                    // Match cell horizontal alignment
                    javax.swing.table.TableColumn tableCol = t.getColumnModel().getColumn(col);
                    javax.swing.table.TableCellRenderer cellRen = tableCol.getCellRenderer();
                    if (cellRen instanceof DefaultTableCellRenderer) {
                        label.setHorizontalAlignment(((DefaultTableCellRenderer) cellRen).getHorizontalAlignment());
                    } else {
                        label.setHorizontalAlignment(JLabel.CENTER);
                    }
                }
                return comp;
            }
        });
    }

    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(PRIMARY_COLOR);
        return label;
    }

    public static void formatTableColumns(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int columnCount = table.getColumnCount();
        FontMetrics fm = table.getFontMetrics(table.getFont());
        FontMetrics headerFm = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());

        for (int i = 0; i < columnCount; i++) {
            javax.swing.table.TableColumn column = table.getColumnModel().getColumn(i);
            String colName = table.getColumnName(i).toLowerCase();

            // Calculate content-based widths
            int maxCellWidth = headerFm.stringWidth(table.getColumnName(i)) + 24;
            for (int r = 0; r < table.getRowCount(); r++) {
                Object val = table.getValueAt(r, i);
                if (val != null) {
                    String valStr = val.toString();
                    if (colName.contains("status")) {
                        valStr = valStr.toUpperCase().replace("_", " ");
                    } else if ((colName.contains("amount") || colName.contains("price") || colName.contains("charges") || colName.contains("total") || colName.contains("discount") || colName.contains("tax")) && !valStr.startsWith("₱")) {
                        try {
                            double num = Double.parseDouble(valStr.replaceAll("[^\\d.]", ""));
                            valStr = String.format("₱%,.2f", num);
                        } catch (Exception ignored) {}
                    }
                    int cellWidth = fm.stringWidth(valStr) + 20;
                    if (cellWidth > maxCellWidth) {
                        maxCellWidth = cellWidth;
                    }
                }
            }

            int minWidth = 60;
            int prefWidth = maxCellWidth;

            // Enforce bounds based on standard fields
            if (colName.contains("id")) {
                prefWidth = Math.max(60, Math.min(prefWidth, 80));
            } else if ((colName.contains("room") && colName.contains("number")) || colName.equals("room") || colName.equals("room number")) {
                prefWidth = Math.max(80, Math.min(prefWidth, 100));
            } else if (colName.contains("guest") && colName.contains("number") || colName.equals("guests") || colName.contains("capacity") || colName.contains("nights")) {
                prefWidth = Math.max(80, Math.min(prefWidth, 100));
            } else if (colName.contains("guest") || colName.contains("name")) {
                prefWidth = Math.max(180, prefWidth);
            } else if (colName.contains("type")) {
                prefWidth = Math.max(130, prefWidth);
            } else if (colName.contains("status")) {
                prefWidth = Math.max(130, prefWidth);
            } else if (colName.contains("amount") || colName.contains("total") || colName.contains("price") || colName.contains("charges") || colName.contains("discount") || colName.contains("tax")) {
                prefWidth = Math.max(130, prefWidth);
            }

            column.setPreferredWidth(prefWidth);
            column.setMinWidth(minWidth);

            // Apply Alignments & Renderers
            DefaultTableCellRenderer cellRenderer;
            if (colName.contains("status")) {
                cellRenderer = new StatusBadgeRenderer();
            } else {
                cellRenderer = new DefaultTableCellRenderer();
                if (colName.contains("id") || colName.contains("room") || colName.contains("nights") || colName.contains("guests") || colName.contains("capacity") || colName.contains("date") || colName.contains("in") || colName.contains("out")) {
                    cellRenderer.setHorizontalAlignment(JLabel.CENTER);
                } else if (colName.contains("amount") || colName.contains("price") || colName.contains("charges") || colName.contains("total") || colName.contains("discount") || colName.contains("tax")) {
                    cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    cellRenderer.setHorizontalAlignment(JLabel.LEFT);
                }
            }
            column.setCellRenderer(cellRenderer);
        }
    }

    public static JComponent createStatusBadge(String status) {
        if (status == null) {
            status = "-";
        }
        String valStr = status.toUpperCase().replace("_", " ");
        JLabel label = new JLabel(valStr, SwingConstants.CENTER);
        label.setFont(FONT_BOLD);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        Color fg, bg;
        if (valStr.equals("PENDING") || valStr.equals("MAINTENANCE")) {
            fg = new Color(216, 107, 0);
            bg = new Color(255, 243, 230);
        } else if (valStr.equals("CONFIRMED") || valStr.equals("RESERVED")) {
            fg = new Color(0, 102, 204);
            bg = new Color(230, 242, 255);
        } else if (valStr.equals("CHECKED IN") || valStr.equals("AVAILABLE") || valStr.equals("PAID")) {
            fg = new Color(30, 120, 45);
            bg = new Color(230, 245, 233);
        } else if (valStr.equals("CHECKED OUT") || valStr.equals("COMPLETED") || valStr.equals("UNPAID")) {
            fg = new Color(100, 100, 100);
            bg = new Color(240, 240, 240);
        } else if (valStr.equals("CANCELLED") || valStr.equals("OCCUPIED") || valStr.equals("OVERDUE")) {
            fg = new Color(185, 30, 30);
            bg = new Color(255, 235, 235);
        } else {
            fg = Color.DARK_GRAY;
            bg = Color.WHITE;
        }
        label.setForeground(fg);
        label.setBackground(bg);

        JPanel container = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        container.setOpaque(false);
        container.add(label);
        return container;
    }

    public static boolean confirmPermanentDelete(Component parent) {
        Object[] options = {"Delete Permanently", "Cancel"};
        int result = JOptionPane.showOptionDialog(
                parent,
                "Are you sure you want to permanently delete the selected record(s)?\n\nThis action cannot be undone.",
                "Confirm Permanent Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]
        );
        return result == JOptionPane.YES_OPTION;
    }
}
