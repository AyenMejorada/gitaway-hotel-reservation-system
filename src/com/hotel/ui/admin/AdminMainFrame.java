package com.hotel.ui.admin;

import com.hotel.ui.common.Session;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.login.LoginFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Main shell for the Admin side of the application. Provides a sidebar with
 * navigation to Dashboard, Reservation Management, Room Management, Guest
 * Management, Billing, and Logout, swapping the center content panel
 * (CardLayout) as the admin navigates.
 */
public class AdminMainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    static final String CARD_DASHBOARD = "DASHBOARD";
    static final String CARD_ROOMS = "ROOMS";
    static final String CARD_RESERVATIONS = "RESERVATIONS";
    static final String CARD_GUESTS = "GUESTS";
    static final String CARD_BILLING = "BILLING";

    private DashboardPanel dashboardPanel;
    private RoomManagementPanel roomPanel;
    private ReservationManagementPanel reservationPanel;
    private GuestManagementPanel guestPanel;
    private BillingManagementPanel billingManagementPanel;
    private JButton activeNavButton;
    private final java.util.Map<String, JButton> navButtons = new java.util.HashMap<>();

    public AdminMainFrame() {
        super("Hotel Reservation System - Admin Panel");
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
        showCard(CARD_DASHBOARD);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIUtils.SIDEBAR_COLOR);
        sidebar.setPreferredSize(new Dimension(300, 0));

        JLabel logo = new JLabel("  HOTEL ADMIN");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
        sidebar.add(logo);

        String welcomeName = Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "Admin";
        JLabel welcome = new JLabel("  Welcome, " + welcomeName);
        welcome.setFont(UIUtils.FONT_REGULAR);
        welcome.setForeground(new Color(190, 200, 215));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcome.setBorder(BorderFactory.createEmptyBorder(0, 8, 20, 8));
        sidebar.add(welcome);

        sidebar.add(createNavButton("Dashboard", CARD_DASHBOARD, true));
        sidebar.add(createNavButton("Room Management", CARD_ROOMS, false));
        sidebar.add(createNavButton("Reservation Management", CARD_RESERVATIONS, false));
        sidebar.add(createNavButton("Guest Management", CARD_GUESTS, false));
        sidebar.add(createNavButton("Billing & Payments", CARD_BILLING, false));

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

        navButtons.put(cardName, button);

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

        dashboardPanel = new DashboardPanel();
        roomPanel = new RoomManagementPanel();
        reservationPanel = new ReservationManagementPanel();
        guestPanel = new GuestManagementPanel();
        billingManagementPanel = new BillingManagementPanel();

        contentPanel.add(dashboardPanel, CARD_DASHBOARD);
        contentPanel.add(roomPanel, CARD_ROOMS);
        contentPanel.add(reservationPanel, CARD_RESERVATIONS);
        contentPanel.add(guestPanel, CARD_GUESTS);
        contentPanel.add(billingManagementPanel, CARD_BILLING);

        return contentPanel;
    }

    private void showCard(String cardName) {
        if (cardName.equals(CARD_DASHBOARD) && dashboardPanel != null) {
            dashboardPanel.refreshCurrentView();
        } else if (cardName.equals(CARD_ROOMS) && roomPanel != null) {
            roomPanel.refreshCurrentView();
        } else if (cardName.equals(CARD_RESERVATIONS) && reservationPanel != null) {
            reservationPanel.refreshCurrentView();
        } else if (cardName.equals(CARD_GUESTS) && guestPanel != null) {
            guestPanel.refreshCurrentView();
        } else if (cardName.equals(CARD_BILLING) && billingManagementPanel != null) {
            billingManagementPanel.refreshCurrentView();
        }
        cardLayout.show(contentPanel, cardName);
    }

    public void navigateToCard(String cardName) {
        showCard(cardName);
        JButton button = navButtons.get(cardName);
        if (button != null) {
            setActiveButton(button);
        }
    }

    public void navigateToBillingAndShow(int reservationId) {
        navigateToCard(CARD_BILLING);
        if (billingManagementPanel != null) {
            billingManagementPanel.showBillForReservation(reservationId);
        }
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
