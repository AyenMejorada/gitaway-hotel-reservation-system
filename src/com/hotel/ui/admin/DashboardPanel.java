package com.hotel.ui.admin;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.service.RoomService;
import com.hotel.service.ReservationService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main dashboard: displays the hotel's primary operations dashboard, combining
 * both summary statistics and the complete room inventory in one place,
 * along with operational widgets for upcoming check-ins and check-outs.
 */
public class DashboardPanel extends JPanel {

    private final RoomService roomService = new RoomService();
    private final ReservationService reservationService = new ReservationService();

    // Table elements
    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;

    // Buttons
    private JButton toggleArchiveButton;
    private JLabel titleLabel;

    // Filters & Search
    private PlaceholderTextField searchField;
    private JComboBox<String> typeFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JComboBox<String> sortByCombo;

    // Summary Card Labels
    private JLabel totalRoomsVal;
    private JLabel availableRoomsVal;
    private JLabel occupiedRoomsVal;
    private JLabel reservedRoomsVal;
    private JLabel maintenanceRoomsVal;
    private JLabel todayCheckInsVal;
    private JLabel todayCheckOutsVal;
    private JLabel totalRevenueVal;

    // Upcoming check-in/out elements
    private ReadOnlyTableModel checkInsTableModel;
    private JTable checkInsTable;
    private ReadOnlyTableModel checkOutsTableModel;
    private JTable checkOutsTable;

    // Master list of loaded rooms
    private List<Room> allRooms = new ArrayList<>();

    private static final String[] COLUMNS = {
            "ID", "Room Number", "Type", "Capacity", "Price/Night", "Status", "Current Guest", "Check-in", "Check-out"
    };

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
        titleLabel = UIUtils.createSectionTitle("Dashboard Overview");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton addButton = UIUtils.createStyledButton("Add Room", UIUtils.SUCCESS_COLOR);
        addButton.addActionListener(e -> handleAdd());

        JButton editButton = UIUtils.createStyledButton("Edit", UIUtils.ACCENT_COLOR);
        editButton.addActionListener(e -> handleEdit());

        JButton deleteButton = UIUtils.createStyledButton("Delete", UIUtils.DANGER_COLOR);
        deleteButton.addActionListener(e -> handleDelete());

        toggleArchiveButton = UIUtils.createStyledButton("View Archived", UIUtils.PRIMARY_COLOR);
        toggleArchiveButton.addActionListener(e -> toggleArchivedView());

        buttonsRow.add(addButton);
        buttonsRow.add(editButton);
        buttonsRow.add(deleteButton);
        buttonsRow.add(toggleArchiveButton);
        headerRow.add(buttonsRow, BorderLayout.EAST);
        northContainer.add(headerRow);
        northContainer.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- 2. SUMMARY CARDS (8 Cards in 2x4 grid) ---
        JPanel summaryPanel = new JPanel(new GridLayout(2, 4, 12, 12));
        summaryPanel.setOpaque(false);
        summaryPanel.setPreferredSize(new Dimension(0, 170));

        totalRoomsVal = new JLabel("0");
        availableRoomsVal = new JLabel("0");
        occupiedRoomsVal = new JLabel("0");
        reservedRoomsVal = new JLabel("0");
        maintenanceRoomsVal = new JLabel("0");
        todayCheckInsVal = new JLabel("0");
        todayCheckOutsVal = new JLabel("0");
        totalRevenueVal = new JLabel("₱0.00");

        summaryPanel.add(createCard("Total Rooms", totalRoomsVal, UIUtils.PRIMARY_COLOR));
        summaryPanel.add(createCard("Available Rooms", availableRoomsVal, new Color(30, 115, 45)));
        summaryPanel.add(createCard("Occupied Rooms", occupiedRoomsVal, new Color(185, 30, 30)));
        summaryPanel.add(createCard("Reserved Rooms", reservedRoomsVal, new Color(25, 95, 180)));
        summaryPanel.add(createCard("Rooms Under Maintenance", maintenanceRoomsVal, new Color(200, 100, 0)));
        summaryPanel.add(createCard("Today's Check-ins", todayCheckInsVal, new Color(142, 68, 173)));
        summaryPanel.add(createCard("Today's Check-outs", todayCheckOutsVal, new Color(22, 160, 133)));
        summaryPanel.add(createCard("Total Revenue", totalRevenueVal, new Color(192, 57, 43)));
        northContainer.add(summaryPanel);
        northContainer.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- 3. CONTROLS PANEL (SEARCH & FILTERS) ---
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setBackground(Color.WHITE);
        controlsPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 233, 237), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 6, 0, 6);
        gbc.gridy = 0;

        // Search Room Number
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setOpaque(false);
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UIUtils.FONT_BOLD);
        searchField = new PlaceholderTextField("Search by Room Number or Room Type...");
        searchField.setFont(UIUtils.FONT_REGULAR);
        searchField.setPreferredSize(new Dimension(0, 30));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterAndRender(); }
            public void removeUpdate(DocumentEvent e) { filterAndRender(); }
            public void changedUpdate(DocumentEvent e) { filterAndRender(); }
        });
        searchBox.add(searchLbl, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);
        controlsPanel.add(searchBox, gbc);

        // Room Type Filter
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        JPanel typeBox = new JPanel(new BorderLayout(6, 0));
        typeBox.setOpaque(false);
        JLabel typeLbl = new JLabel("Type:");
        typeLbl.setFont(UIUtils.FONT_BOLD);
        typeFilterCombo = new JComboBox<>();
        typeFilterCombo.setFont(UIUtils.FONT_REGULAR);
        typeFilterCombo.addItem("ALL");
        for (RoomType rt : RoomType.values()) {
            typeFilterCombo.addItem(rt.name());
        }
        typeFilterCombo.addActionListener(e -> filterAndRender());
        typeBox.add(typeLbl, BorderLayout.WEST);
        typeBox.add(typeFilterCombo, BorderLayout.CENTER);
        controlsPanel.add(typeBox, gbc);

        // Status Filter
        gbc.gridx = 2;
        gbc.weightx = 0.2;
        JPanel statusBox = new JPanel(new BorderLayout(6, 0));
        statusBox.setOpaque(false);
        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UIUtils.FONT_BOLD);
        statusFilterCombo = new JComboBox<>();
        statusFilterCombo.setFont(UIUtils.FONT_REGULAR);
        statusFilterCombo.addItem("ALL");
        for (RoomStatus rs : RoomStatus.values()) {
            statusFilterCombo.addItem(rs.name());
        }
        statusFilterCombo.addActionListener(e -> filterAndRender());
        statusBox.add(statusLbl, BorderLayout.WEST);
        statusBox.add(statusFilterCombo, BorderLayout.CENTER);
        controlsPanel.add(statusBox, gbc);

        // Sort By
        gbc.gridx = 3;
        gbc.weightx = 0.2;
        JPanel sortBox = new JPanel(new BorderLayout(6, 0));
        sortBox.setOpaque(false);
        JLabel sortLbl = new JLabel("Sort By:");
        sortLbl.setFont(UIUtils.FONT_BOLD);
        sortByCombo = new JComboBox<>(new String[]{"Room Number", "Room Type"});
        sortByCombo.setFont(UIUtils.FONT_REGULAR);
        sortByCombo.addActionListener(e -> filterAndRender());
        sortBox.add(sortLbl, BorderLayout.WEST);
        sortBox.add(sortByCombo, BorderLayout.CENTER);
        controlsPanel.add(sortBox, gbc);

        northContainer.add(controlsPanel);
        add(northContainer, BorderLayout.NORTH);

        // --- SPLIT PANE WORKSPACE ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(850);
        splitPane.setBorder(null);

        // Left Component: Main Room Inventory Table
        JPanel mainTablePanel = new JPanel(new BorderLayout());
        mainTablePanel.setOpaque(false);

        tableModel = new ReadOnlyTableModel(COLUMNS, 0);
        table = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.GRAY);
                    g2.setFont(UIUtils.FONT_HEADER);
                    String text = "No matching records found.";
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = getHeight() / 2;
                    g2.drawString(text, x, y);
                    g2.dispose();
                }
            }
        };
        UIUtils.styleTable(table);
        table.getTableHeader().setReorderingAllowed(false);

        // Format Status badge color
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 233, 237), 1, true));
        mainTablePanel.add(scrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(mainTablePanel);

        // Right Component: Sidebar Panel with Upcoming Check-ins and Upcoming Check-outs
        JPanel sidebarPanel = new JPanel(new GridLayout(2, 1, 16, 16));
        sidebarPanel.setOpaque(false);
        sidebarPanel.setPreferredSize(new Dimension(320, 0));

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

    private void refreshAllData() {
        UIUtils.runSafely(this, () -> {
            // Load rooms
            if (viewingArchived) {
                allRooms = roomService.getAllArchivedRooms();
            } else {
                allRooms = roomService.getAllActiveRoomsWithOccupancy();
            }

            // Load upcoming widgets
            loadUpcomingWidgets();

            // Update stats cards
            updateSummaryCards();

            // Filter & Sort
            filterAndRender();
        });
    }

    private void loadUpcomingWidgets() {
        List<Reservation> activeReservations = reservationService.getAllActiveReservations();
        LocalDate today = LocalDate.now();

        // 1. Upcoming Check-ins: PENDING or CONFIRMED reservations, sorted by checkInDate ascending, limit to 5
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

        // 2. Upcoming Check-outs: CHECKED_IN reservations, sorted by checkOutDate ascending, limit to 5
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
        long total = 0;
        long available = 0;
        long occupied = 0;
        long reserved = 0;
        long maintenance = 0;

        if (viewingArchived) {
            total = allRooms.size();
            for (Room r : allRooms) {
                if (r.getStatus() == RoomStatus.AVAILABLE) available++;
                else if (r.getStatus() == RoomStatus.OCCUPIED) occupied++;
                else if (r.getStatus() == RoomStatus.RESERVED) reserved++;
                else if (r.getStatus() == RoomStatus.MAINTENANCE) maintenance++;
            }
        } else {
            total = roomService.countActiveRooms();
            available = roomService.countByStatus(RoomStatus.AVAILABLE);
            occupied = roomService.countByStatus(RoomStatus.OCCUPIED);
            reserved = roomService.countByStatus(RoomStatus.RESERVED);
            maintenance = roomService.countByStatus(RoomStatus.MAINTENANCE);
        }

        List<Reservation> activeReservations = reservationService.getAllActiveReservations();
        LocalDate today = LocalDate.now();
        long todayCheckIns = activeReservations.stream()
                .filter(r -> r.getCheckInDate() != null && r.getCheckInDate().equals(today))
                .count();
        long todayCheckOuts = activeReservations.stream()
                .filter(r -> r.getCheckOutDate() != null && r.getCheckOutDate().equals(today))
                .count();

        BigDecimal totalRevenue = reservationService.getTotalRevenue();
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en", "PH"));
        String revenueStr = formatter.format(totalRevenue == null ? BigDecimal.ZERO : totalRevenue);

        totalRoomsVal.setText(String.valueOf(total));
        availableRoomsVal.setText(String.valueOf(available));
        occupiedRoomsVal.setText(String.valueOf(occupied));
        reservedRoomsVal.setText(String.valueOf(reserved));
        maintenanceRoomsVal.setText(String.valueOf(maintenance));
        todayCheckInsVal.setText(String.valueOf(todayCheckIns));
        todayCheckOutsVal.setText(String.valueOf(todayCheckOuts));
        totalRevenueVal.setText(revenueStr);
    }

    private void filterAndRender() {
        String searchText = searchField.getText().trim().toLowerCase();
        String typeFilter = (String) typeFilterCombo.getSelectedItem();
        String statusFilter = (String) statusFilterCombo.getSelectedItem();
        String sortBy = (String) sortByCombo.getSelectedItem();

        List<Room> filtered = allRooms.stream()
                .filter(r -> searchText.isEmpty() || r.getRoomNumber().toLowerCase().contains(searchText) || r.getRoomType().name().toLowerCase().contains(searchText))
                .filter(r -> "ALL".equals(typeFilter) || r.getRoomType().name().equals(typeFilter))
                .filter(r -> "ALL".equals(statusFilter) || r.getStatus().name().equals(statusFilter))
                .collect(Collectors.toList());

        // Custom sort
        if ("Room Number".equals(sortBy)) {
            filtered.sort(Comparator.comparing(Room::getRoomNumber));
        } else if ("Room Type".equals(sortBy)) {
            filtered.sort(Comparator.comparing(r -> r.getRoomType().name()));
        }

        tableModel.setRowCount(0);
        for (Room r : filtered) {
            tableModel.addRow(new Object[]{
                    r.getRoomId(),
                    r.getRoomNumber(),
                    r.getRoomType(),
                    r.getCapacity(),
                    String.format("₱%,.2f", r.getPricePerNight()),
                    r.getStatus(),
                    r.getCurrentGuest() == null ? "" : r.getCurrentGuest(),
                    r.getCheckInDate() == null ? "" : r.getCheckInDate().toString(),
                    r.getCheckOutDate() == null ? "" : r.getCheckOutDate().toString()
            });
        }
        UIUtils.formatTableColumns(table);
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Dashboard Overview — Archived Rooms");
            toggleArchiveButton.setText("View Active");
        } else {
            titleLabel.setText("Dashboard Overview");
            toggleArchiveButton.setText("View Archived");
        }
        refreshAllData();
    }

    public void refreshCurrentView() {
        refreshAllData();
    }

    private void handleAdd() {
        RoomFormDialog dialog = new RoomFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshAllData();
        }
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a room to edit.");
            return;
        }
        if (viewingArchived) {
            UIUtils.showInfo(this, "Archived rooms cannot be edited. Restore the room first.");
            return;
        }
        int roomId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Room room = roomService.getRoomOrThrow(roomId);
            RoomFormDialog dialog = new RoomFormDialog(SwingUtilities.getWindowAncestor(this), room);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshAllData();
            }
        });
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a room to delete.");
            return;
        }
        int roomId = (int) tableModel.getValueAt(row, 0);
        String roomNumber = String.valueOf(tableModel.getValueAt(row, 1));

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore room " + roomNumber + " back to active rooms?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    roomService.restoreRoom(roomId);
                    UIUtils.showSuccess(this, "Room restored successfully.");
                    refreshAllData();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move room " + roomNumber + " to archive? This is a soft delete; the record is not permanently lost.",
                    "Confirm Delete");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    roomService.softDeleteRoom(roomId);
                    UIUtils.showSuccess(this, "Room archived successfully.");
                    refreshAllData();
                });
            }
        }
    }

    // Custom cell renderer to display status badges
    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(UIUtils.FONT_BOLD);
            label.setOpaque(true);

            if (value instanceof RoomStatus) {
                RoomStatus status = (RoomStatus) value;
                switch (status) {
                    case AVAILABLE:
                        label.setForeground(new Color(30, 115, 45));
                        label.setBackground(new Color(230, 245, 233));
                        break;
                    case RESERVED:
                        label.setForeground(new Color(25, 95, 180));
                        label.setBackground(new Color(230, 240, 255));
                        break;
                    case OCCUPIED:
                        label.setForeground(new Color(185, 30, 30));
                        label.setBackground(new Color(255, 235, 235));
                        break;
                    case MAINTENANCE:
                        label.setForeground(new Color(200, 100, 0));
                        label.setBackground(new Color(255, 240, 225));
                        break;
                }
            }
            return label;
        }
    }
}