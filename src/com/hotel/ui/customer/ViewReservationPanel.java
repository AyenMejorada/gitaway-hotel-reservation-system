package com.hotel.ui.customer;

import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.service.ReservationService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Customer screen for viewing their own reservations only (scoped to the
 * guest profile linked to the logged-in account).
 */
public class ViewReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();

    private ReadOnlyTableModel tableModel;
    private JTable table;

    private static final String[] COLUMNS = {
            "Reservation ID", "Room", "Room Type", "Check-in", "Check-out", "Nights", "Guests", "Status", "Total Amount"
    };

    public ViewReservationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtils.createSectionTitle("My Reservations"), BorderLayout.WEST);

        JButton refreshButton = UIUtils.createStyledButton("Refresh", UIUtils.ACCENT_COLOR);
        refreshButton.addActionListener(e -> refresh());
        headerRow.add(refreshButton, BorderLayout.EAST);

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
                        r.getRoomType(),
                        r.getCheckInDate().format(DATE_FORMAT),
                        r.getCheckOutDate().format(DATE_FORMAT),
                        r.getNumberOfNights(),
                        r.getNumGuests(),
                        r.getStatus(),
                        String.format("₱%,.2f", r.getTotalAmount())
                });
            }
            if (reservations.isEmpty()) {
                UIUtils.showInfo(this, "You have no reservations yet. Use \"Make Reservation\" to book a room.");
            }
        });
    }
}
