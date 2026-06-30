package com.hotel.ui.admin;

import com.hotel.service.DashboardService;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Admin dashboard: displays the four headline metrics requested -
 * total rooms, total guests, total reservations, and total revenue -
 * each pulled live from {@link DashboardService}.
 */
public class DashboardPanel extends JPanel {

    private final DashboardService dashboardService = new DashboardService();

    private JLabel roomsValueLabel;
    private JLabel guestsValueLabel;
    private JLabel reservationsValueLabel;
    private JLabel revenueValueLabel;

    public DashboardPanel() {
        initComponents();
        refreshStats();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtils.createSectionTitle("Dashboard Overview"), BorderLayout.WEST);

        JButton refreshButton = UIUtils.createStyledButton("Refresh", UIUtils.ACCENT_COLOR);
        refreshButton.addActionListener(e -> refreshStats());
        headerRow.add(refreshButton, BorderLayout.EAST);

        add(headerRow, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 24, 24));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        roomsValueLabel = new JLabel("0");
        guestsValueLabel = new JLabel("0");
        reservationsValueLabel = new JLabel("0");
        revenueValueLabel = new JLabel("₱0.00");

        cardsPanel.add(createStatCard("Total Rooms", roomsValueLabel, new Color(0, 120, 215)));
        cardsPanel.add(createStatCard("Total Guests", guestsValueLabel, new Color(34, 139, 34)));
        cardsPanel.add(createStatCard("Total Reservations", reservationsValueLabel, new Color(218, 165, 32)));
        cardsPanel.add(createStatCard("Total Revenue", revenueValueLabel, new Color(178, 34, 34)));

        add(cardsPanel, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, accent),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIUtils.FONT_HEADER);
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        valueLabel.setForeground(UIUtils.PRIMARY_COLOR);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valueLabel);

        return card;
    }

    public void refreshStats() {
        UIUtils.runSafely(this, () -> {
            DashboardService.DashboardStats stats = dashboardService.getStats();
            roomsValueLabel.setText(String.valueOf(stats.getTotalRooms()));
            guestsValueLabel.setText(String.valueOf(stats.getTotalGuests()));
            reservationsValueLabel.setText(String.valueOf(stats.getTotalReservations()));
            revenueValueLabel.setText(formatCurrency(stats.getTotalRevenue()));
        });
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        return formatter.format(amount == null ? BigDecimal.ZERO : amount);
    }
}
