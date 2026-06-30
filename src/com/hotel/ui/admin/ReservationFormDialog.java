package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.service.GuestService;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;
import com.hotel.ui.common.UIUtils;
import com.hotel.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modal dialog used for both "Add Reservation" and "Edit Reservation" in
 * Reservation Management. Guest and Room are chosen from combo boxes
 * populated from the active (non-deleted) records in each table.
 */
public class ReservationFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();
    private final GuestService guestService = new GuestService();
    private final RoomService roomService = new RoomService();

    private final Reservation existingReservation; // null when adding
    private boolean saved = false;

    private JComboBox<Guest> guestCombo;
    private JComboBox<Room> roomCombo;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JTextField numGuestsField;
    private JComboBox<ReservationStatus> statusCombo;
    private JTextArea notesArea;

    public ReservationFormDialog(Window owner, Reservation existingReservation) {
        super(owner, existingReservation == null ? "Add Reservation" : "Edit Reservation", ModalityType.APPLICATION_MODAL);
        this.existingReservation = existingReservation;
        initComponents();
        loadComboData();
        if (existingReservation != null) {
            populateFields(existingReservation);
        }
    }

    private void initComponents() {
        setSize(440, 700);
        setLocationRelativeTo(getOwner());
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        gbc.gridy = row++;
        form.add(formLabel("Guest"), gbc);
        guestCombo = new JComboBox<>();
        gbc.gridy = row++;
        form.add(guestCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Room"), gbc);
        roomCombo = new JComboBox<>();
        gbc.gridy = row++;
        form.add(roomCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Check-in Date (yyyy-MM-dd)"), gbc);
        checkInField = new JTextField();
        gbc.gridy = row++;
        form.add(checkInField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Check-out Date (yyyy-MM-dd)"), gbc);
        checkOutField = new JTextField();
        gbc.gridy = row++;
        form.add(checkOutField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Number of Guests"), gbc);
        numGuestsField = new JTextField();
        gbc.gridy = row++;
        form.add(numGuestsField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Status"), gbc);
        statusCombo = new JComboBox<>(ReservationStatus.values());
        gbc.gridy = row++;
        form.add(statusCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Notes"), gbc);
        notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        gbc.gridy = row++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(notesScroll, gbc);

        add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = UIUtils.createStyledButton("Save", UIUtils.SUCCESS_COLOR);
        saveButton.addActionListener(e -> handleSave());
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BOLD);
        return label;
    }

    private void loadComboData() {
        try {
            List<Guest> guests = guestService.getAllActiveGuests();
            for (Guest g : guests) {
                guestCombo.addItem(g);
            }
            List<Room> rooms = roomService.getAllActiveRooms();
            for (Room r : rooms) {
                roomCombo.addItem(r);
            }
        } catch (HotelException ex) {
            UIUtils.showError(this, "Failed to load guests/rooms: " + ex.getMessage());
        }
    }

    private void populateFields(Reservation reservation) {
        selectComboById(guestCombo, reservation.getGuestId(), Guest.class);
        selectComboById(roomCombo, reservation.getRoomId(), Room.class);
        checkInField.setText(reservation.getCheckInDate().format(DATE_FORMAT));
        checkOutField.setText(reservation.getCheckOutDate().format(DATE_FORMAT));
        numGuestsField.setText(String.valueOf(reservation.getNumGuests()));
        statusCombo.setSelectedItem(reservation.getStatus());
        notesArea.setText(reservation.getNotes());
    }

    @SuppressWarnings("unchecked")
    private <T> void selectComboById(JComboBox<T> combo, int id, Class<T> type) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            T item = combo.getItemAt(i);
            int itemId = (item instanceof Guest) ? ((Guest) item).getGuestId() : ((Room) item).getRoomId();
            if (itemId == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void handleSave() {
        try {
            Guest selectedGuest = (Guest) guestCombo.getSelectedItem();
            Room selectedRoom = (Room) roomCombo.getSelectedItem();
            Validator.requireNonNull(selectedGuest, "Guest");
            Validator.requireNonNull(selectedRoom, "Room");

            LocalDate checkIn = parseDate(checkInField.getText(), "Check-in date");
            LocalDate checkOut = parseDate(checkOutField.getText(), "Check-out date");
            int numGuests = Validator.parseInt(numGuestsField.getText(), "Number of guests");
            ReservationStatus status = (ReservationStatus) statusCombo.getSelectedItem();
            String notes = notesArea.getText();

            if (existingReservation == null) {
                reservationService.addReservation(selectedGuest.getGuestId(), selectedRoom.getRoomId(),
                        checkIn, checkOut, numGuests, status, notes);
                UIUtils.showSuccess(this, "Reservation added successfully.");
            } else {
                reservationService.updateReservation(existingReservation.getReservationId(), selectedRoom.getRoomId(),
                        checkIn, checkOut, numGuests, status, notes);
                UIUtils.showSuccess(this, "Reservation updated successfully.");
            }
            saved = true;
            dispose();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
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

    public boolean isSaved() {
        return saved;
    }
}
