package com.hotel.ui.customer;

import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.service.ReservationService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Customer screen for cancelling an existing reservation. Cancellation
 * sets the reservation status to CANCELLED rather than physically
 * deleting it, so the booking history remains intact for the admin side.
 */
public class CancelReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();

    private ReadOnlyTableModel tableModel;
    private JTable table;

    private static final String[] COLUMNS = {
            "Reservation ID", "Room", "Check-in", "Check-out", "Guests", "Status", "Total Amount"
    };

    public CancelReservationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtils.createSectionTitle("Cancel Reservation"), BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);
        JButton cancelButton = UIUtils.createStyledButton("Cancel Selected", UIUtils.DANGER_COLOR);
        cancelButton.addActionListener(e -> handleCancel());
        JButton refreshButton = UIUtils.createStyledButton("Refresh", Color.GRAY);
        refreshButton.addActionListener(e -> refresh());
        buttonsRow.add(cancelButton);
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

    public void refresh() {
        UIUtils.runSafely(this, () -> {
            Guest guest = CustomerContext.getOrCreateCurrentGuest();
            List<Reservation> reservations = reservationService.getReservationsForGuest(guest.getGuestId());
            tableModel.setRowCount(0);
            for (Reservation r : reservations) {
                tableModel.addRow(new Object[]{
                        r.getReservationId(),
                        r.getRoomNumber(),
                        r.getCheckInDate().format(DATE_FORMAT),
                        r.getCheckOutDate().format(DATE_FORMAT),
                        r.getNumGuests(),
                        r.getStatus(),
                        String.format("₱%,.2f", r.getTotalAmount())
                });
            }
        });
    }

    private void handleCancel() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a reservation to cancel.");
            return;
        }
        int reservationId = (int) tableModel.getValueAt(row, 0);
        ReservationStatus currentStatus = (ReservationStatus) tableModel.getValueAt(row, 5);

        if (currentStatus == ReservationStatus.CANCELLED) {
            UIUtils.showInfo(this, "This reservation is already cancelled.");
            return;
        }
        if (currentStatus == ReservationStatus.CHECKED_OUT) {
            UIUtils.showInfo(this, "A checked-out reservation cannot be cancelled.");
            return;
        }

        boolean confirmed = UIUtils.confirm(this,
                "Are you sure you want to cancel reservation #" + reservationId + "?", "Confirm Cancellation");
        if (confirmed) {
            UIUtils.runSafely(this, () -> {
                reservationService.cancelReservation(reservationId);
                UIUtils.showSuccess(this, "Reservation cancelled successfully.");
                refresh();
            });
        }
    }
}
