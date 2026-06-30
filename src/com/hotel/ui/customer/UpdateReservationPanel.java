package com.hotel.ui.customer;

import com.hotel.exception.HotelException;
import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;
import com.hotel.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Customer screen for updating an existing reservation's dates, room,
 * or number of guests. Only reservations belonging to the logged-in
 * customer are shown, and only reservations that have not yet been
 * checked out or cancelled may be updated.
 */
public class UpdateReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();
    private final RoomService roomService = new RoomService();

    private ReadOnlyTableModel tableModel;
    private JTable table;

    private static final String[] COLUMNS = {
            "Reservation ID", "Room", "Check-in", "Check-out", "Guests", "Status", "Total Amount"
    };

    public UpdateReservationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtils.createSectionTitle("Update Reservation"), BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);
        JButton updateButton = UIUtils.createStyledButton("Update Selected", UIUtils.ACCENT_COLOR);
        updateButton.addActionListener(e -> handleUpdate());
        JButton refreshButton = UIUtils.createStyledButton("Refresh", Color.GRAY);
        refreshButton.addActionListener(e -> refresh());
        buttonsRow.add(updateButton);
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

    private void handleUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a reservation to update.");
            return;
        }
        int reservationId = (int) tableModel.getValueAt(row, 0);

        UIUtils.runSafely(this, () -> {
            Reservation reservation = reservationService.getReservationOrThrow(reservationId);

            if (reservation.getStatus() == ReservationStatus.CANCELLED
                    || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
                UIUtils.showError(this, "This reservation can no longer be updated (status: " + reservation.getStatus() + ").");
                return;
            }

            showUpdateDialog(reservation);
        });
    }

    private void showUpdateDialog(Reservation reservation) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Update Reservation #" + reservation.getReservationId(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        gbc.gridy = row++;
        form.add(boldLabel("Room"), gbc);
        JComboBox<Room> roomCombo = new JComboBox<>();
        UIUtils.runSafely(this, () -> {
            List<Room> rooms = roomService.getAllActiveRooms();
            for (Room r : rooms) {
                roomCombo.addItem(r);
                if (r.getRoomId() == reservation.getRoomId()) {
                    roomCombo.setSelectedItem(r);
                }
            }
        });
        gbc.gridy = row++;
        form.add(roomCombo, gbc);

        gbc.gridy = row++;
        form.add(boldLabel("Check-in Date (yyyy-MM-dd)"), gbc);
        JTextField checkInField = new JTextField(reservation.getCheckInDate().format(DATE_FORMAT));
        gbc.gridy = row++;
        form.add(checkInField, gbc);

        gbc.gridy = row++;
        form.add(boldLabel("Check-out Date (yyyy-MM-dd)"), gbc);
        JTextField checkOutField = new JTextField(reservation.getCheckOutDate().format(DATE_FORMAT));
        gbc.gridy = row++;
        form.add(checkOutField, gbc);

        gbc.gridy = row++;
        form.add(boldLabel("Number of Guests"), gbc);
        JTextField numGuestsField = new JTextField(String.valueOf(reservation.getNumGuests()));
        gbc.gridy = row++;
        form.add(numGuestsField, gbc);

        dialog.add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton saveBtn = UIUtils.createStyledButton("Save Changes", UIUtils.SUCCESS_COLOR);
        saveBtn.addActionListener(e -> {
            try {
                Room selectedRoom = (Room) roomCombo.getSelectedItem();
                Validator.requireNonNull(selectedRoom, "Room");
                LocalDate checkIn = parseDate(checkInField.getText(), "Check-in date");
                LocalDate checkOut = parseDate(checkOutField.getText(), "Check-out date");
                int numGuests = Validator.parseInt(numGuestsField.getText(), "Number of guests");

                reservationService.updateReservation(reservation.getReservationId(), selectedRoom.getRoomId(),
                        checkIn, checkOut, numGuests, reservation.getStatus(), reservation.getNotes());

                UIUtils.showSuccess(dialog, "Reservation updated successfully.");
                dialog.dispose();
                refresh();
            } catch (HotelException ex) {
                UIUtils.showError(dialog, ex.getMessage());
            }
        });
        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BOLD);
        return label;
    }

    private LocalDate parseDate(String text, String fieldName) {
        Validator.requireNonBlank(text, fieldName);
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new com.hotel.exception.ValidationException(
                    fieldName + " must be in yyyy-MM-dd format (e.g. 2026-07-15).");
        }
    }
}
