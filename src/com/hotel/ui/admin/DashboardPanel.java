package com.hotel.ui.admin;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.service.RoomService;
import com.hotel.service.ReservationService;
import com.hotel.service.DashboardService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clean, simplified Dashboard: focuses on daily operational attention items.
 * Contains:
 * - 5 operational summary cards
 * - Split layout: Recent Activity log (left) and Upcoming Check-ins/Check-outs widgets (right)
 */
public class DashboardPanel extends JPanel {

    private final RoomService roomService = new RoomService();
    private final ReservationService reservationService = new ReservationService();
    private final DashboardService dashboardService = new DashboardService();

    // Summary Card Labels
    private JLabel todayCheckInsVal;
    private JLabel todayCheckOutsVal;
    private JLabel pendingReservationsVal;
    private JLabel maintenanceRoomsVal;
    private JLabel totalRevenueVal;

    // Upcoming check-in/out elements
    private ReadOnlyTableModel checkInsTableModel;
    private JTable checkInsTable;
    private ReadOnlyTableModel checkOutsTableModel;
    private JTable checkOutsTable;

    // Recent Activity panel
    private DefaultListModel<String> activityListModel;
    private JList<String> activityList;

    private static final String[] CHECKIN_COLUMNS = {
            "Guest Name", "Room", "Type", "Check-in", "Remaining"
    };

    private static final String[] CHECKOUT_COLUMNS = {
            "Guest Name", "Room", "Type", "Check-out", "Remaining"
    };

    public DashboardPanel() {
        initComponents();
        refreshAllData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);

        // --- 1. HEADER ROW ---
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel titleLabel = UIUtils.createSectionTitle("Dashboard Overview");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton reportButton = UIUtils.createStyledButton("Generate Report", UIUtils.ACCENT_COLOR);
        reportButton.addActionListener(e -> handleGenerateReport());
        buttonsRow.add(reportButton);
        headerRow.add(buttonsRow, BorderLayout.EAST);

        northContainer.add(headerRow);
        northContainer.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- 2. SUMMARY CARDS (5 Cards in 1x5 grid) ---
        JPanel summaryPanel = new JPanel(new GridLayout(1, 5, 12, 12));
        summaryPanel.setOpaque(false);
        summaryPanel.setPreferredSize(new Dimension(0, 85));

        todayCheckInsVal = new JLabel("0");
        todayCheckOutsVal = new JLabel("0");
        pendingReservationsVal = new JLabel("0");
        maintenanceRoomsVal = new JLabel("0");
        totalRevenueVal = new JLabel("₱0.00");

        summaryPanel.add(createCard("Today's Check-ins", todayCheckInsVal, new Color(142, 68, 173)));
        summaryPanel.add(createCard("Today's Check-outs", todayCheckOutsVal, new Color(22, 160, 133)));
        summaryPanel.add(createCard("Pending Reservations", pendingReservationsVal, new Color(25, 95, 180)));
        summaryPanel.add(createCard("Rooms In Maintenance", maintenanceRoomsVal, new Color(200, 100, 0)));
        summaryPanel.add(createCard("Estimated Revenue", totalRevenueVal, new Color(192, 57, 43)));
        northContainer.add(summaryPanel);
        northContainer.add(Box.createRigidArea(new Dimension(0, 16)));

        add(northContainer, BorderLayout.NORTH);

        // --- 3. SPLIT WORKSPACE: Recent Activity (Left) vs Upcoming Widgets (Right) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(500);
        splitPane.setBorder(null);

        // Left Component: Recent Activity Log
        JPanel activityPanel = new JPanel(new BorderLayout(8, 8));
        activityPanel.setBackground(Color.WHITE);
        activityPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 233, 237), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel activityTitle = new JLabel("Recent System Activity");
        activityTitle.setFont(UIUtils.FONT_HEADER);
        activityTitle.setForeground(UIUtils.PRIMARY_COLOR);
        activityTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 237)));
        activityPanel.add(activityTitle, BorderLayout.NORTH);

        activityListModel = new DefaultListModel<>();
        activityList = new JList<>(activityListModel);
        activityList.setFont(UIUtils.FONT_REGULAR);
        activityList.setSelectionBackground(new Color(240, 245, 255));
        activityList.setSelectionForeground(UIUtils.PRIMARY_COLOR);
        activityList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                if (value != null && value.toString().contains("maintenance")) {
                    label.setForeground(new Color(200, 100, 0));
                } else if (value != null && (value.toString().contains("checked in") || value.toString().contains("CONFIRMED"))) {
                    label.setForeground(new Color(30, 115, 45));
                } else if (value != null && value.toString().contains("cancelled")) {
                    label.setForeground(UIUtils.DANGER_COLOR);
                } else {
                    label.setForeground(Color.DARK_GRAY);
                }
                return label;
            }
        });

        JScrollPane activityScroll = new JScrollPane(activityList);
        activityScroll.setBorder(null);
        activityPanel.add(activityScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(activityPanel);

        // Right Component: Sidebar Panel with Upcoming Check-ins and Upcoming Check-outs
        JPanel sidebarPanel = new JPanel(new GridLayout(2, 1, 16, 16));
        sidebarPanel.setOpaque(false);
        sidebarPanel.setPreferredSize(new Dimension(500, 0));

        // Widget 1: Upcoming Check-ins
        JPanel checkInsWidget = new JPanel(new BorderLayout(8, 8));
        checkInsWidget.setBackground(Color.WHITE);
        checkInsWidget.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 233, 237), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        JLabel checkInsTitle = new JLabel("Upcoming Check-ins");
        checkInsTitle.setFont(UIUtils.FONT_HEADER);
        checkInsTitle.setForeground(UIUtils.PRIMARY_COLOR);
        checkInsTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 237)));
        checkInsWidget.add(checkInsTitle, BorderLayout.NORTH);

        checkInsTableModel = new ReadOnlyTableModel(CHECKIN_COLUMNS, 0);
        checkInsTable = new JTable(checkInsTableModel);
        UIUtils.styleTable(checkInsTable);
        checkInsTable.getTableHeader().setReorderingAllowed(false);
        checkInsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkInsTable.setRowHeight(24);
        JScrollPane checkInsScroll = new JScrollPane(checkInsTable);
        checkInsScroll.setBorder(null);
        checkInsWidget.add(checkInsScroll, BorderLayout.CENTER);

        // Widget 2: Upcoming Check-outs
        JPanel checkOutsWidget = new JPanel(new BorderLayout(8, 8));
        checkOutsWidget.setBackground(Color.WHITE);
        checkOutsWidget.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 233, 237), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        JLabel checkOutsTitle = new JLabel("Upcoming Check-outs");
        checkOutsTitle.setFont(UIUtils.FONT_HEADER);
        checkOutsTitle.setForeground(UIUtils.PRIMARY_COLOR);
        checkOutsTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 237)));
        checkOutsWidget.add(checkOutsTitle, BorderLayout.NORTH);

        checkOutsTableModel = new ReadOnlyTableModel(CHECKOUT_COLUMNS, 0);
        checkOutsTable = new JTable(checkOutsTableModel);
        UIUtils.styleTable(checkOutsTable);
        checkOutsTable.getTableHeader().setReorderingAllowed(false);
        checkOutsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkOutsTable.setRowHeight(24);
        JScrollPane checkOutsScroll = new JScrollPane(checkOutsTable);
        checkOutsScroll.setBorder(null);
        checkOutsWidget.add(checkOutsScroll, BorderLayout.CENTER);

        sidebarPanel.add(checkInsWidget);
        sidebarPanel.add(checkOutsWidget);

        splitPane.setRightComponent(sidebarPanel);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(UIUtils.PRIMARY_COLOR);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(valueLabel);
        return card;
    }

    public void refreshCurrentView() {
        refreshAllData();
    }

    private void refreshAllData() {
        UIUtils.runSafely(this, () -> {
            loadUpcomingWidgets();
            updateSummaryCards();
            loadRecentActivities();
        });
    }

    private void loadUpcomingWidgets() {
        List<Reservation> activeReservations = reservationService.getAllActiveReservations();
        LocalDate today = LocalDate.now();

        List<Reservation> upcomingCheckIns = activeReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.CONFIRMED)
                .sorted(Comparator.comparing(Reservation::getCheckInDate))
                .limit(5)
                .collect(Collectors.toList());

        checkInsTableModel.setRowCount(0);
        for (Reservation res : upcomingCheckIns) {
            long remaining = ChronoUnit.DAYS.between(today, res.getCheckInDate());
            String remainingStr;
            if (remaining == 0) {
                remainingStr = "Today";
            } else if (remaining == 1) {
                remainingStr = "Tomorrow";
            } else if (remaining < 0) {
                remainingStr = Math.abs(remaining) + " days overdue";
            } else {
                remainingStr = "In " + remaining + " days";
            }
            checkInsTableModel.addRow(new Object[]{
                    res.getGuestName(),
                    res.getRoomNumber() == null ? "N/A" : res.getRoomNumber(),
                    res.getRoomType(),
                    res.getCheckInDate(),
                    remainingStr
            });
        }

        List<Reservation> upcomingCheckOuts = activeReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
                .sorted(Comparator.comparing(Reservation::getCheckOutDate))
                .limit(5)
                .collect(Collectors.toList());

        checkOutsTableModel.setRowCount(0);
        for (Reservation res : upcomingCheckOuts) {
            long remaining = ChronoUnit.DAYS.between(today, res.getCheckOutDate());
            String remainingStr = remaining == 0 ? "Today" : remaining < 0 ? (Math.abs(remaining) + " days overdue") : (remaining + " days");
            checkOutsTableModel.addRow(new Object[]{
                    res.getGuestName(),
                    res.getRoomNumber() == null ? "N/A" : res.getRoomNumber(),
                    res.getRoomType(),
                    res.getCheckOutDate(),
                    remainingStr
            });
        }
        UIUtils.formatTableColumns(checkInsTable);
        UIUtils.formatTableColumns(checkOutsTable);
    }

    private void updateSummaryCards() {
        List<Reservation> activeReservations = reservationService.getAllActiveReservations();
        LocalDate today = LocalDate.now();
        
        long todayCheckIns = activeReservations.stream()
                .filter(r -> r.getCheckInDate() != null && r.getCheckInDate().equals(today))
                .count();
        long todayCheckOuts = activeReservations.stream()
                .filter(r -> r.getCheckOutDate() != null && r.getCheckOutDate().equals(today))
                .count();
        long pendingRes = activeReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .count();
        long maintenanceRooms = roomService.countByStatus(RoomStatus.MAINTENANCE);

        BigDecimal totalRevenue = reservationService.getTotalRevenue();
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en", "PH"));
        String revenueStr = formatter.format(totalRevenue == null ? BigDecimal.ZERO : totalRevenue);

        todayCheckInsVal.setText(String.valueOf(todayCheckIns));
        todayCheckOutsVal.setText(String.valueOf(todayCheckOuts));
        pendingReservationsVal.setText(String.valueOf(pendingRes));
        maintenanceRoomsVal.setText(String.valueOf(maintenanceRooms));
        totalRevenueVal.setText(revenueStr);
    }

    private void loadRecentActivities() {
        activityListModel.clear();
        
        // Compile activities based on real status changes in the database
        List<Reservation> activeReservations = reservationService.getAllActiveReservations();
        
        // 1. Sort reservations by ID descending to get the newest first
        List<Reservation> sortedReservations = activeReservations.stream()
                .sorted(Comparator.comparingInt(Reservation::getReservationId).reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (Reservation r : sortedReservations) {
            String guest = r.getGuestName() != null ? r.getGuestName() : "Guest";
            String roomNum = r.getRoomNumber() != null ? r.getRoomNumber() : "Room " + r.getRoomId();
            
            if (r.getStatus() == ReservationStatus.CHECKED_IN) {
                activityListModel.addElement("• Guest " + guest + " checked in to room " + roomNum + " (Res #" + r.getReservationId() + ")");
            } else if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
                activityListModel.addElement("• Guest " + guest + " checked out of room " + roomNum + " (Res #" + r.getReservationId() + ")");
            } else if (r.getStatus() == ReservationStatus.CANCELLED) {
                activityListModel.addElement("• Reservation #" + r.getReservationId() + " for guest " + guest + " was cancelled");
            } else if (r.getStatus() == ReservationStatus.CONFIRMED) {
                activityListModel.addElement("• Reservation #" + r.getReservationId() + " for guest " + guest + " confirmed in room " + roomNum);
            } else {
                activityListModel.addElement("• Reservation #" + r.getReservationId() + " created for guest " + guest + " (PENDING)");
            }
        }

        // Add rooms under maintenance
        List<Room> activeRooms = roomService.getAllActiveRooms();
        for (Room r : activeRooms) {
            if (r.getStatus() == RoomStatus.MAINTENANCE) {
                activityListModel.addElement("• Room " + r.getRoomNumber() + " marked under maintenance");
            }
        }
        
        if (activityListModel.isEmpty()) {
            activityListModel.addElement("• No recent system activities recorded.");
        }
    }

    private void handleGenerateReport() {
        UIUtils.runSafely(this, () -> {
            String reportText = dashboardService.generateReport();
            
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Generated Hotel Operations Report", true);
            dialog.setSize(550, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(16, 16));
            
            JPanel contentPanel = new JPanel(new BorderLayout(12, 12));
            contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
            contentPanel.setBackground(Color.WHITE);
            
            JTextArea textArea = new JTextArea(reportText);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
            textArea.setEditable(false);
            textArea.setMargin(new Insets(10, 10, 10, 10));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(new LineBorder(new Color(230, 233, 237), 1, true));
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            
            JLabel titleLabel = new JLabel("Operations & Metrics Report Summary");
            titleLabel.setFont(UIUtils.FONT_HEADER);
            titleLabel.setForeground(UIUtils.PRIMARY_COLOR);
            titleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
            contentPanel.add(titleLabel, BorderLayout.NORTH);
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.setBackground(Color.WHITE);
            JButton closeButton = UIUtils.createStyledButton("Close", UIUtils.PRIMARY_COLOR);
            closeButton.addActionListener(e -> dialog.dispose());
            bottomPanel.add(closeButton);
            contentPanel.add(bottomPanel, BorderLayout.SOUTH);
            
            dialog.add(contentPanel);
            dialog.setVisible(true);
        });
    }
}