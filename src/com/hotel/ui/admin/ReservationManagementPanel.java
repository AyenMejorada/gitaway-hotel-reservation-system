package com.hotel.ui.admin;

import com.hotel.model.Reservation;
import com.hotel.service.ReservationService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin screen for managing reservations: add, edit, soft-delete (archive),
 * view archived reservations (with restore option), and refresh. The table
 * shows full joined details (guest name, room number/type) for every
 * reservation in the database.
 */
public class ReservationManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;
    private JButton toggleArchiveButton;
    private JLabel titleLabel;

    private static final String[] COLUMNS = {
            "ID", "Guest", "Room", "Room Type", "Check-in", "Check-out", "Nights", "Guests", "Status", "Total Amount"
    };

    public ReservationManagementPanel() {
        initComponents();
        loadActiveReservations();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Reservation Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton addButton = UIUtils.createStyledButton("Add", UIUtils.SUCCESS_COLOR);
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

    private void loadActiveReservations() {
        UIUtils.runSafely(this, () -> {
            List<Reservation> reservations = reservationService.getAllActiveReservations();
            populateTable(reservations);
        });
    }

    private void loadArchivedReservations() {
        UIUtils.runSafely(this, () -> {
            List<Reservation> reservations = reservationService.getAllArchivedReservations();
            populateTable(reservations);
        });
    }

    private void populateTable(List<Reservation> reservations) {
        tableModel.setRowCount(0);
        for (Reservation r : reservations) {
            tableModel.addRow(new Object[]{
                    r.getReservationId(),
                    r.getGuestName(),
                    r.getRoomNumber(),
                    r.getRoomType(),
                    r.getCheckInDate().format(DATE_FORMAT),
                    r.getCheckOutDate().format(DATE_FORMAT),
                    r.getNumberOfNights(),
                    r.getNumGuests(),
                    r.getStatus(),
                    String.format("₱%,.2f", r.getTotalAmount())
            });
        }
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Reservation Management — Archived Reservations");
            toggleArchiveButton.setText("View Active");
            loadArchivedReservations();
        } else {
            titleLabel.setText("Reservation Management");
            toggleArchiveButton.setText("View Archived");
            loadActiveReservations();
        }
    }

    private void refreshCurrentView() {
        if (viewingArchived) {
            loadArchivedReservations();
        } else {
            loadActiveReservations();
        }
    }

    private void handleAdd() {
        ReservationFormDialog dialog = new ReservationFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadActiveReservations();
            if (dialog.getSavedStatus() == com.hotel.model.ReservationStatus.CHECKED_OUT) {
                Window win = SwingUtilities.getWindowAncestor(this);
                if (win instanceof AdminMainFrame) {
                    List<Reservation> activeRes = reservationService.getAllActiveReservations();
                    int latestResId = activeRes.stream().mapToInt(Reservation::getReservationId).max().orElse(-1);
                    if (latestResId > 0) {
                        ((AdminMainFrame) win).navigateToBillingAndShow(latestResId);
                    }
                }
            }
        }
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a reservation to edit.");
            return;
        }
        if (viewingArchived) {
            UIUtils.showInfo(this, "Archived reservations cannot be edited. Restore the reservation first.");
            return;
        }
        int reservationId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Reservation reservation = reservationService.getReservationOrThrow(reservationId);
            ReservationFormDialog dialog = new ReservationFormDialog(SwingUtilities.getWindowAncestor(this), reservation);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadActiveReservations();
                if (dialog.getSavedStatus() == com.hotel.model.ReservationStatus.CHECKED_OUT) {
                    Window win = SwingUtilities.getWindowAncestor(this);
                    if (win instanceof AdminMainFrame) {
                        ((AdminMainFrame) win).navigateToBillingAndShow(reservationId);
                    }
                }
            }
        });
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a reservation to delete.");
            return;
        }
        int reservationId = (int) tableModel.getValueAt(row, 0);

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore reservation #" + reservationId + " back to active reservations?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    reservationService.restoreReservation(reservationId);
                    UIUtils.showSuccess(this, "Reservation restored successfully.");
                    loadArchivedReservations();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move reservation #" + reservationId + " to archive? This is a soft delete; the record is not permanently lost.",
                    "Confirm Delete");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    reservationService.softDeleteReservation(reservationId);
                    UIUtils.showSuccess(this, "Reservation archived successfully.");
                    loadActiveReservations();
                });
            }
        }
    }
}
