package com.hotel.ui.admin;

import com.hotel.model.Guest;
import com.hotel.service.GuestService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Admin screen for managing hotel guests: add, edit, soft-delete (archive),
 * view archived guests (with restore option), and refresh.
 */
public class GuestManagementPanel extends JPanel {

    private final GuestService guestService = new GuestService();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;
    private JButton toggleArchiveButton;
    private JLabel titleLabel;

    private static final String[] COLUMNS = {
            "ID", "First Name", "Last Name", "Email", "Phone", "Address", "ID Number"
    };

    public GuestManagementPanel() {
        initComponents();
        loadActiveGuests();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Guest Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton addButton = UIUtils.createStyledButton("Add Guest", UIUtils.SUCCESS_COLOR);
        addButton.addActionListener(e -> handleAdd());

        JButton editButton = UIUtils.createStyledButton("Edit", UIUtils.ACCENT_COLOR);
        editButton.addActionListener(e -> handleEdit());

        JButton deleteButton = UIUtils.createStyledButton("Delete", UIUtils.DANGER_COLOR);
        deleteButton.addActionListener(e -> handleDelete());

        toggleArchiveButton = UIUtils.createStyledButton("View Archived", UIUtils.PRIMARY_COLOR);
        toggleArchiveButton.addActionListener(e -> toggleArchivedView());

        JButton refreshButton = UIUtils.createStyledButton("Refresh", Color.GRAY);
        refreshButton.addActionListener(e -> refreshCurrentView());

        buttonsRow.add(addButton);
        buttonsRow.add(editButton);
        buttonsRow.add(deleteButton);
        buttonsRow.add(toggleArchiveButton);
        buttonsRow.add(refreshButton);
        headerRow.add(buttonsRow, BorderLayout.EAST);

        add(headerRow, BorderLayout.NORTH);

        tableModel = new ReadOnlyTableModel(COLUMNS, 0);
        table = new JTable(tableModel);
        UIUtils.styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadActiveGuests() {
        UIUtils.runSafely(this, () -> {
            List<Guest> guests = guestService.getAllActiveGuests();
            populateTable(guests);
        });
    }

    private void loadArchivedGuests() {
        UIUtils.runSafely(this, () -> {
            List<Guest> guests = guestService.getAllArchivedGuests();
            populateTable(guests);
        });
    }

    private void populateTable(List<Guest> guests) {
        tableModel.setRowCount(0);
        for (Guest g : guests) {
            tableModel.addRow(new Object[]{
                    g.getGuestId(),
                    g.getFirstName(),
                    g.getLastName(),
                    g.getEmail(),
                    g.getPhone(),
                    g.getAddress(),
                    g.getIdNumber()
            });
        }
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Guest Management — Archived Guests");
            toggleArchiveButton.setText("View Active");
            loadArchivedGuests();
        } else {
            titleLabel.setText("Guest Management");
            toggleArchiveButton.setText("View Archived");
            loadActiveGuests();
        }
    }

    private void refreshCurrentView() {
        if (viewingArchived) {
            loadArchivedGuests();
        } else {
            loadActiveGuests();
        }
    }

    private void handleAdd() {
        GuestFormDialog dialog = new GuestFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadActiveGuests();
        }
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a guest to edit.");
            return;
        }
        if (viewingArchived) {
            UIUtils.showInfo(this, "Archived guests cannot be edited. Restore the guest first.");
            return;
        }
        int guestId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Guest guest = guestService.getGuestOrThrow(guestId);
            GuestFormDialog dialog = new GuestFormDialog(SwingUtilities.getWindowAncestor(this), guest);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadActiveGuests();
            }
        });
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a guest to delete.");
            return;
        }
        int guestId = (int) tableModel.getValueAt(row, 0);
        String guestName = tableModel.getValueAt(row, 1) + " " + tableModel.getValueAt(row, 2);

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore guest " + guestName + " back to active guests?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    guestService.restoreGuest(guestId);
                    UIUtils.showSuccess(this, "Guest restored successfully.");
                    loadArchivedGuests();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move guest " + guestName + " to archive? This is a soft delete; the record is not permanently lost.",
                    "Confirm Delete");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    guestService.softDeleteGuest(guestId);
                    UIUtils.showSuccess(this, "Guest archived successfully.");
                    loadActiveGuests();
                });
            }
        }
    }
}
