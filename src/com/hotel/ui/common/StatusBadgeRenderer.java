package com.hotel.ui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class StatusBadgeRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(UIUtils.FONT_BOLD);
        label.setOpaque(true);

        if (value == null) {
            return label;
        }

        String valStr = value.toString().toUpperCase().replace("_", " ");
        label.setText(valStr);

        // Status coloring
        if (valStr.equals("PENDING") || valStr.equals("MAINTENANCE")) {
            label.setForeground(new Color(216, 107, 0));
            label.setBackground(new Color(255, 243, 230));
        } else if (valStr.equals("CONFIRMED") || valStr.equals("RESERVED")) {
            label.setForeground(new Color(0, 102, 204));
            label.setBackground(new Color(230, 242, 255));
        } else if (valStr.equals("CHECKED IN") || valStr.equals("AVAILABLE") || valStr.equals("PAID")) {
            label.setForeground(new Color(30, 120, 45));
            label.setBackground(new Color(230, 245, 233));
        } else if (valStr.equals("CHECKED OUT") || valStr.equals("COMPLETED") || valStr.equals("UNPAID")) {
            label.setForeground(new Color(100, 100, 100));
            label.setBackground(new Color(240, 240, 240));
        } else if (valStr.equals("CANCELLED") || valStr.equals("OCCUPIED") || valStr.equals("OVERDUE")) {
            label.setForeground(new Color(185, 30, 30));
            label.setBackground(new Color(255, 235, 235));
        } else {
            label.setForeground(Color.DARK_GRAY);
            label.setBackground(Color.WHITE);
        }

        // Keep selection background if row is selected
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        }

        return label;
    }
}
