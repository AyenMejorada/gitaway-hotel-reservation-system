package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.service.GuestService;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.common.DatePickerField;
import com.hotel.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modal dialog used for both "Add Reservation" and "Edit Reservation" in
 * Reservation Management.
 */
public class ReservationFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();
    private final GuestService guestService = new GuestService();
    private final RoomService roomService = new RoomService();

    private final Reservation existingReservation; // null when adding
    private boolean saved = false;
    private ReservationStatus savedStatus = null;

    private JTextField guestNameField; // null when editing
    private JComboBox<Room> roomCombo;
    private DatePickerField checkInPicker;
    private DatePickerField checkOutPicker;
    private JTextField numGuestsField;
    private JComboBox<ReservationStatus> statusCombo;
    private JTextArea notesArea;

    private List<Reservation> confirmedReservations = new java.util.ArrayList<>();

    public ReservationFormDialog(Window owner, Reservation existingReservation) {
        super(owner, existingReservation == null ? "Add Reservation" : "Edit Reservation",
                ModalityType.APPLICATION_MODAL);
        this.existingReservation = existingReservation;
        initComponents();
        loadComboData();
        if (existingReservation != null) {
            populateFields(existingReservation);
        }
    }

    private void initComponents() {
        setSize(440, 720);
        setLocationRelativeTo(getOwner());
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        if (existingReservation == null) {
            gbc.gridy = row++;
            form.add(formLabel("Guest Name (First Last)"), gbc);
            guestNameField = new JTextField();
            guestNameField.setPreferredSize(new Dimension(0, 36));
            guestNameField.setFont(UIUtils.FONT_REGULAR);
            gbc.gridy = row++;
            form.add(guestNameField, gbc);
        }

        gbc.gridy = row++;
        form.add(formLabel("Room"), gbc);
        roomCombo = new JComboBox<>();
        roomCombo.setPreferredSize(new Dimension(0, 36));
        roomCombo.setFont(UIUtils.FONT_REGULAR);
        roomCombo.addActionListener(e -> {
            Room selectedRoom = (Room) roomCombo.getSelectedItem();
            if (selectedRoom != null) {
                numGuestsField.setText(String.valueOf(selectedRoom.getCapacity()));
            }
            updateConfirmedReservations();
            
            // Re-validate dates on room change
            LocalDate inDate = checkInPicker.getDate();
            if (inDate != null && !isDateAvailable(inDate)) {
                checkInPicker.setDate(null);
            }
            LocalDate outDate = checkOutPicker.getDate();
            if (outDate != null && !isDateAvailable(outDate)) {
                checkOutPicker.setDate(null);
            }
        });
        gbc.gridy = row++;
        form.add(roomCombo, gbc);

        LocalDate initialCheckIn = existingReservation != null ? existingReservation.getCheckInDate() : null;
        LocalDate initialCheckOut = existingReservation != null ? existingReservation.getCheckOutDate() : null;

        gbc.gridy = row++;
        form.add(formLabel("Check-in Date"), gbc);
        checkInPicker = new DatePickerField();
        checkInPicker.setDateValidator(date -> (date.equals(initialCheckIn) || !date.isBefore(LocalDate.now())) && isDateAvailable(date));
        gbc.gridy = row++;
        form.add(checkInPicker, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Check-out Date"), gbc);
        checkOutPicker = new DatePickerField();
        checkOutPicker.setEnabled(false);
        checkOutPicker.setDateValidator(date -> (date.equals(initialCheckOut) || !date.isBefore(LocalDate.now())) && isDateAvailable(date));
        gbc.gridy = row++;
        form.add(checkOutPicker, gbc);

        checkInPicker.addChangeListener(() -> {
            LocalDate inDate = checkInPicker.getDate();
            if (inDate != null) {
                checkOutPicker.setEnabled(true);
                checkOutPicker.setDateValidator(date -> (date.equals(initialCheckOut) || (!date.isBefore(LocalDate.now()) && date.isAfter(inDate))) && isDateAvailable(date));
                LocalDate outDate = checkOutPicker.getDate();
                if (outDate != null && !outDate.isAfter(inDate) && !outDate.equals(initialCheckOut)) {
                    checkOutPicker.setDate(null);
                }
            } else {
                checkOutPicker.setDate(null);
                checkOutPicker.setEnabled(false);
            }
        });

        gbc.gridy = row++;
        form.add(formLabel("Number of Guests"), gbc);
        numGuestsField = new JTextField();
        numGuestsField.setPreferredSize(new Dimension(0, 36));
        numGuestsField.setFont(UIUtils.FONT_REGULAR);
        numGuestsField.setEditable(false);
        gbc.gridy = row++;
        form.add(numGuestsField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Status"), gbc);
        statusCombo = new JComboBox<>(ReservationStatus.values());
        statusCombo.setPreferredSize(new Dimension(0, 36));
        statusCombo.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(statusCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Notes"), gbc);
        notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(UIUtils.FONT_REGULAR);
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

    private void updateConfirmedReservations() {
        Room selectedRoom = (Room) roomCombo.getSelectedItem();
        if (selectedRoom == null) {
            confirmedReservations = new java.util.ArrayList<>();
        } else {
            try {
                confirmedReservations = reservationService.getConfirmedReservationsForRoom(selectedRoom.getRoomId());
            } catch (Exception ex) {
                confirmedReservations = new java.util.ArrayList<>();
            }
        }
    }

    private boolean isDateAvailable(LocalDate date) {
        Room selectedRoom = (Room) roomCombo.getSelectedItem();
        if (selectedRoom == null) {
            return true;
        }
        for (Reservation res : confirmedReservations) {
            if (existingReservation != null && res.getReservationId() == existingReservation.getReservationId()) {
                continue;
            }
            if (!date.isBefore(res.getCheckInDate()) && date.isBefore(res.getCheckOutDate())) {
                return false;
            }
        }
        return true;
    }

    private void loadComboData() {
        try {
            List<Room> rooms = roomService.getAllActiveRooms();
            for (Room r : rooms) {
                if (r.getStatus() == RoomStatus.MAINTENANCE) {
                    if (existingReservation == null || r.getRoomId() != existingReservation.getRoomId()) {
                        continue;
                    }
                }
                roomCombo.addItem(r);
            }
            updateConfirmedReservations();
        } catch (HotelException ex) {
            UIUtils.showError(this, "Failed to load rooms: " + ex.getMessage());
        }
    }

    private void populateFields(Reservation reservation) {
        selectComboById(roomCombo, reservation.getRoomId());
        updateConfirmedReservations();
        checkInPicker.setDate(reservation.getCheckInDate());
        checkOutPicker.setDate(reservation.getCheckOutDate());
        numGuestsField.setText(String.valueOf(reservation.getNumGuests()));
        statusCombo.setSelectedItem(reservation.getStatus());
        notesArea.setText(reservation.getNotes());
    }

    private void selectComboById(JComboBox<Room> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Room item = combo.getItemAt(i);
            if (item.getRoomId() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void handleSave() {
        try {
            Room selectedRoom = (Room) roomCombo.getSelectedItem();
            Validator.requireNonNull(selectedRoom, "Room");

            LocalDate checkIn = checkInPicker.getDate();
            LocalDate checkOut = checkOutPicker.getDate();
            Validator.requireNonNull(checkIn, "Check-in date");
            Validator.requireNonNull(checkOut, "Check-out date");
            Validator.validateDateRange(checkIn, checkOut);

            int numGuests = Validator.parseInt(numGuestsField.getText(), "Number of guests");
            ReservationStatus status = (ReservationStatus) statusCombo.getSelectedItem();
            String notes = notesArea.getText();

            if (existingReservation == null) {
                String guestName = guestNameField.getText();
                Guest guest = findOrCreateGuestByName(guestName);

                reservationService.addReservation(guest.getGuestId(), selectedRoom.getRoomId(),
                        checkIn, checkOut, numGuests, status, notes);
                UIUtils.showSuccess(this, "Reservation added successfully.");
            } else {
                reservationService.updateReservation(existingReservation.getReservationId(), selectedRoom.getRoomId(),
                        checkIn, checkOut, numGuests, status, notes);
                UIUtils.showSuccess(this, "Reservation updated successfully.");
            }
            saved = true;
            savedStatus = status;
            dispose();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private Guest findOrCreateGuestByName(String fullName) {
        String name = fullName.trim();
        Validator.requireNonBlank(name, "Guest Name");

        String firstName = "";
        String lastName = "";
        int spaceIndex = name.indexOf(' ');
        if (spaceIndex >= 0) {
            firstName = name.substring(0, spaceIndex).trim();
            lastName = name.substring(spaceIndex + 1).trim();
        } else {
            firstName = name;
            lastName = "";
        }

        if (firstName.isEmpty()) {
            throw new com.hotel.exception.ValidationException("First name cannot be empty.");
        }

        List<Guest> activeGuests = guestService.getAllActiveGuests();
        for (Guest g : activeGuests) {
            if (g.getFirstName().equalsIgnoreCase(firstName) && g.getLastName().equalsIgnoreCase(lastName)) {
                return g;
            }
        }

        String placeholderEmail = "auto_" + firstName.toLowerCase() + "_" + (lastName.isEmpty() ? "" : lastName.toLowerCase() + "_") + System.currentTimeMillis() + "@example.com";
        Guest newGuest = guestService.addGuest(
                null,
                firstName,
                lastName,
                placeholderEmail,
                "0000000000",
                "",
                ""
        );
        return newGuest;
    }

    public boolean isSaved() {
        return saved;
    }

    public ReservationStatus getSavedStatus() {
        return savedStatus;
    }
}
