package com.hotel.ui.customer;

import com.hotel.ui.common.Session;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.login.LoginFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Main shell for the Customer side of the application. Provides a sidebar with
 * navigation to Make Reservation, View Reservations, Update Reservation, Cancel
 * Reservation, and Logout.
 */
public class CustomerMainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private static final String CARD_MAKE = "MAKE";
    private static final String CARD_VIEW = "VIEW";
    private static final String CARD_UPDATE = "UPDATE";
    private static final String CARD_CANCEL = "CANCEL";

    private ViewReservationPanel viewPanel;
    private UpdateReservationPanel updatePanel;
    private CancelReservationPanel cancelPanel;
    private JButton activeNavButton;

    public CustomerMainFrame() {
        super("Hotel Reservation System - Customer Portal");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContentArea(), BorderLayout.CENTER);

        setContentPane(root);
        showCard(CARD_MAKE);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIUtils.SIDEBAR_COLOR);
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel("  HOTEL PORTAL");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
        sidebar.add(logo);

        String welcomeName = Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "Guest";
        JLabel welcome = new JLabel("  Welcome, " + welcomeName);
        welcome.setFont(UIUtils.FONT_REGULAR);
        welcome.setForeground(new Color(190, 200, 215));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcome.setBorder(BorderFactory.createEmptyBorder(0, 8, 20, 8));
        sidebar.add(welcome);

        sidebar.add(createNavButton("Make Reservation", CARD_MAKE, true));
        sidebar.add(createNavButton("View Reservations", CARD_VIEW, false));
        sidebar.add(createNavButton("Update Reservation", CARD_UPDATE, false));
        sidebar.add(createNavButton("Cancel Reservation", CARD_CANCEL, false));

        sidebar.add(Box.createVerticalGlue());

        JButton logoutBtn = createSidebarButton("Logout");
        logoutBtn.setBackground(UIUtils.DANGER_COLOR);
        logoutBtn.addActionListener(e -> handleLogout());
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(160, 32));

        JPanel logoutWrapper = new JPanel(new BorderLayout());
        logoutWrapper.setOpaque(false);
        logoutWrapper.setBorder(BorderFactory.createEmptyBorder(6, 10, 12, 10));
        logoutWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // <-- cap the wrapper's height
        logoutWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutWrapper.add(logoutBtn, BorderLayout.CENTER);
        sidebar.add(logoutWrapper);

        return sidebar;
    }

    private JButton createNavButton(String label, String cardName, boolean isDefault) {
        JButton button = createSidebarButton(label);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(isDefault ? UIUtils.ACCENT_COLOR : UIUtils.SIDEBAR_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isDefault) {
            activeNavButton = button;
        }

        button.addActionListener(e -> {
            showCard(cardName);
            setActiveButton(button);
        });

        return button;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UIUtils.FONT_BOLD);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        return button;
    }

    private void setActiveButton(JButton button) {
        if (activeNavButton != null) {
            activeNavButton.setBackground(UIUtils.SIDEBAR_COLOR);
        }
        button.setBackground(UIUtils.ACCENT_COLOR);
        activeNavButton = button;
    }

    private JPanel buildContentArea() {
        contentPanel.setBackground(UIUtils.BACKGROUND_COLOR);

        MakeReservationPanel makePanel = new MakeReservationPanel();
        viewPanel = new ViewReservationPanel();
        updatePanel = new UpdateReservationPanel();
        cancelPanel = new CancelReservationPanel();

        contentPanel.add(makePanel, CARD_MAKE);
        contentPanel.add(viewPanel, CARD_VIEW);
        contentPanel.add(updatePanel, CARD_UPDATE);
        contentPanel.add(cancelPanel, CARD_CANCEL);

        return contentPanel;
    }

    private void showCard(String cardName) {
        // Refresh data whenever a reservation-related tab is opened so the
        // customer always sees up-to-date information.
        switch (cardName) {
            case CARD_VIEW:
                viewPanel.refresh();
                break;
            case CARD_UPDATE:
                updatePanel.refresh();
                break;
            case CARD_CANCEL:
                cancelPanel.refresh();
                break;
            default:
                // no refresh needed
                break;
        }
        cardLayout.show(contentPanel, cardName);
    }

    private void handleLogout() {
        boolean confirmed = UIUtils.confirm(this, "Are you sure you want to logout?", "Confirm Logout");
        if (confirmed) {
            Session.clear();
            this.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}
