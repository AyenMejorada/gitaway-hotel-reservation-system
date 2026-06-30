package com.hotel.ui.common;

import javax.swing.*;
import java.awt.*;

public class PlaceholderTextArea extends JTextArea {

    private String placeholder;

    public PlaceholderTextArea() {
        super();
    }

    public PlaceholderTextArea(int rows, int columns) {
        super(rows, columns);
    }

    public PlaceholderTextArea(String placeholder) {
        super();
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder == null || placeholder.isEmpty() || !getText().isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.setFont(getFont().deriveFont(Font.ITALIC));
        Insets insets = getInsets();
        int x = insets.left + 5;
        int y = insets.top + g2.getFontMetrics().getAscent() + 5;
        g2.drawString(placeholder, x, y);
        g2.dispose();
    }
}
