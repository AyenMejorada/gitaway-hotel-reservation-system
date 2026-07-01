package com.hotel.ui.admin;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;
import com.hotel.service.RoomService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dedicated Room Management screen: contains room statistics, filter controls,
 * complete room inventory table, and CRUD actions (Add, Edit, Soft-delete, View Archived).
 */
public class RoomManagementPanel extends JPanel {

    private final RoomService roomService = new RoomService();

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

    // Master list of loaded rooms
    private List<Room> allRooms = new ArrayList<>();

    private static final String[] COLUMNS = {
            "ID", "Room Number", "Type", "Capacity", "Price/Night", "Status", "Current Guest", "Check-in", "Check-out"
    };

    public RoomManagementPanel() {
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
        titleLabel = UIUtils.createSectionTitle("Room Inventory & Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton addButton = UIUtils.createStyledButton("Add Room", UIUtils.SUCCESS_COLOR);
        addButton.addActionListener(e -> handleAdd());

        JButton editButton = UIUtils.createStyledButton("Edit Room", UIUtils.ACCENT_COLOR);
        editButton.addActionListener(e -> handleEdit());

        JButton deleteButton = UIUtils.createStyledButton("Delete Room", UIUtils.DANGER_COLOR);
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

        // --- 2. SUMMARY CARDS (5 Cards in 1x5 grid) ---
        JPanel summaryPanel = new JPanel(new GridLayout(1, 5, 12, 12));
        summaryPanel.setOpaque(false);
        summaryPanel.setPreferredSize(new Dimension(0, 85));

        totalRoomsVal = new JLabel("0");
        availableRoomsVal = new JLabel("0");
        occupiedRoomsVal = new JLabel("0");
        reservedRoomsVal = new JLabel("0");
        maintenanceRoomsVal = new JLabel("0");

        summaryPanel.add(createCard("Total Rooms", totalRoomsVal, UIUtils.PRIMARY_COLOR));
        summaryPanel.add(createCard("Available Rooms", availableRoomsVal, new Color(30, 115, 45)));
        summaryPanel.add(createCard("Occupied Rooms", occupiedRoomsVal, new Color(185, 30, 30)));
        summaryPanel.add(createCard("Reserved Rooms", reservedRoomsVal, new Color(25, 95, 180)));
        summaryPanel.add(createCard("Maintenance Rooms", maintenanceRoomsVal, new Color(200, 100, 0)));
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

        // Main Room Inventory Table
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
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 233, 237), 1, true));
        mainTablePanel.add(scrollPane, BorderLayout.CENTER);
        add(mainTablePanel, BorderLayout.CENTER);
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
            if (viewingArchived) {
                allRooms = roomService.getAllArchivedRooms();
            } else {
                allRooms = roomService.getAllActiveRoomsWithOccupancy();
            }
            updateSummaryCards();
            filterAndRender();
        });
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

        totalRoomsVal.setText(String.valueOf(total));
        availableRoomsVal.setText(String.valueOf(available));
        occupiedRoomsVal.setText(String.valueOf(occupied));
        reservedRoomsVal.setText(String.valueOf(reserved));
        maintenanceRoomsVal.setText(String.valueOf(maintenance));
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
            titleLabel.setText("Room Inventory & Management — Archived");
            toggleArchiveButton.setText("View Active");
        } else {
            titleLabel.setText("Room Inventory & Management");
            toggleArchiveButton.setText("View Archived");
        }
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
