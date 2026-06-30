package com.hotel.ui.admin;

import com.hotel.model.Room;
import com.hotel.service.RoomService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Admin screen for managing hotel rooms: add, edit, soft-delete (archive),
 * view archived rooms (with restore option), and refresh. The table shows
 * full details for every room in the database.
 */
public class RoomManagementPanel extends JPanel {

    private final RoomService roomService = new RoomService();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;
    private JButton toggleArchiveButton;
    private JLabel titleLabel;

    private static final String[] COLUMNS = {
            "ID", "Room Number", "Type", "Price/Night", "Status", "Capacity", "Description"
    };

    public RoomManagementPanel() {
        initComponents();
        loadActiveRooms();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Room Management");
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

        add(headerRow, BorderLayout.NORTH);

        tableModel = new ReadOnlyTableModel(COLUMNS, 0);
        table = new JTable(tableModel);
        UIUtils.styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadActiveRooms() {
        UIUtils.runSafely(this, () -> {
            List<Room> rooms = roomService.getAllActiveRooms();
            populateTable(rooms);
        });
    }

    private void loadArchivedRooms() {
        UIUtils.runSafely(this, () -> {
            List<Room> rooms = roomService.getAllArchivedRooms();
            populateTable(rooms);
        });
    }

    private void populateTable(List<Room> rooms) {
        tableModel.setRowCount(0);
        for (Room r : rooms) {
            tableModel.addRow(new Object[]{
                    r.getRoomId(),
                    r.getRoomNumber(),
                    r.getRoomType(),
                    String.format("₱%,.2f", r.getPricePerNight()),
                    r.getStatus(),
                    r.getCapacity(),
                    r.getDescription()
            });
        }
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Room Management — Archived Rooms");
            toggleArchiveButton.setText("View Active");
            loadArchivedRooms();
        } else {
            titleLabel.setText("Room Management");
            toggleArchiveButton.setText("View Archived");
            loadActiveRooms();
        }
    }

    public void refreshCurrentView() {
        if (viewingArchived) {
            loadArchivedRooms();
        } else {
            loadActiveRooms();
        }
    }

    private void handleAdd() {
        RoomFormDialog dialog = new RoomFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadActiveRooms();
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
                loadActiveRooms();
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
                    loadArchivedRooms();
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
                    loadActiveRooms();
                });
            }
        }
    }
}
