package com.hotel.ui.customer;

import com.hotel.exception.HotelException;
import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.common.DatePickerField;
import com.hotel.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Customer screen for making a new reservation. Lists currently available rooms
 * and lets the customer pick one with their desired dates; the maximum number
 * of
 * guests is derived automatically from the room type, and the system calculates
 * the total cost automatically.
 */
public class MakeReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RoomService roomService = new RoomService();
    private final ReservationService reservationService = new ReservationService();

    private JComboBox<Room> roomCombo;
    private DatePickerField checkInPicker;
    private DatePickerField checkOutPicker;
    private JTextField maxGuestsField;
    private JTextArea notesArea;
    private JLabel estimatedTotalLabel;
    private List<Reservation> confirmedReservations = new java.util.ArrayList<>();

    public MakeReservationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(UIUtils.createSectionTitle("Make a Reservation"), BorderLayout.NORTH);

        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setOpaque(false);
        formWrapper.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createEmptyBorder(40, 48, 40, 48));
        formCard.setPreferredSize(new Dimension(680, 600));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridx = 0;
        int row = 0;

        gbc.gridy = row++;
        formCard.add(formLabel("Select Room"), gbc);
        roomCombo = new JComboBox<>();
        roomCombo.setPreferredSize(new Dimension(0, 36));
        roomCombo.setFont(UIUtils.FONT_REGULAR);
        roomCombo.addActionListener(e -> {
            updateMaxGuests();
            updateEstimate();
            updateConfirmedReservations();
        });
        gbc.gridy = row++;
        formCard.add(roomCombo, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Check-in Date"), gbc);
        checkInPicker = new DatePickerField();
        checkInPicker.setDateValidator(this::isDateAvailable);
        checkInPicker.addChangeListener(this::updateEstimate);
        gbc.gridy = row++;
        formCard.add(checkInPicker, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Check-out Date"), gbc);
        checkOutPicker = new DatePickerField();
        checkOutPicker.setDateValidator(this::isDateAvailable);
        checkOutPicker.addChangeListener(this::updateEstimate);
        gbc.gridy = row++;
        formCard.add(checkOutPicker, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Max Number of Guests"), gbc);
        maxGuestsField = new JTextField("1");
        maxGuestsField.setPreferredSize(new Dimension(0, 36));
        maxGuestsField.setFont(UIUtils.FONT_REGULAR);
        maxGuestsField.setEditable(false);
        maxGuestsField.setBackground(new Color(240, 240, 240));
        gbc.gridy = row++;
        formCard.add(maxGuestsField, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Notes (optional)"), gbc);
        notesArea = new JTextArea(5, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(UIUtils.FONT_REGULAR);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        gbc.gridy = row++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        formCard.add(notesScroll, gbc);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        estimatedTotalLabel = new JLabel("Estimated Total: ₱0.00");
        estimatedTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        estimatedTotalLabel.setForeground(UIUtils.PRIMARY_COLOR);
        gbc.gridy = row++;
        gbc.insets = new Insets(18, 0, 8, 0);
        formCard.add(estimatedTotalLabel, gbc);

        JButton submitButton = UIUtils.createStyledButton("Submit Reservation", UIUtils.SUCCESS_COLOR);
        submitButton.setPreferredSize(new Dimension(0, 46));
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        submitButton.addActionListener(e -> handleSubmit());
        gbc.gridy = row++;
        gbc.insets = new Insets(10, 0, 0, 0);
        formCard.add(submitButton, gbc);

        formWrapper.add(formCard);
        add(formWrapper, BorderLayout.CENTER);

        loadAvailableRooms();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadAvailableRooms();
            }
        });
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BOLD);
        return label;
    }

    private void loadAvailableRooms() {
        roomCombo.removeAllItems();
        UIUtils.runSafely(this, () -> {
            List<Room> rooms = roomService.getAllActiveRooms();
            for (Room r : rooms) {
                if (r.getStatus() == RoomStatus.AVAILABLE) {
                    roomCombo.addItem(r);
                }
            }
            updateMaxGuests();
            updateEstimate();
            updateConfirmedReservations();
        });
    }

    /** Auto-fills the max guest count based on the selected room's type. */
    private void updateMaxGuests() {
        Room room = (Room) roomCombo.getSelectedItem();
        if (room == null || room.getRoomType() == null) {
            maxGuestsField.setText("");
            return;
        }
        maxGuestsField.setText(String.valueOf(maxGuestsForType(room.getRoomType())));
    }

    private int maxGuestsForType(RoomType type) {
        switch (type) {
            case SINGLE:
                return 1;
            case DOUBLE:
                return 2;
            case DELUXE:
                return 3;
            case SUITE:
                return 4;
            default:
                return 1;
        }
    }

    private void updateEstimate() {
        Room room = (Room) roomCombo.getSelectedItem();
        LocalDate checkIn = checkInPicker.getDate();
        LocalDate checkOut = checkOutPicker.getDate();
        if (room != null && checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
            java.math.BigDecimal total = room.getPricePerNight().multiply(java.math.BigDecimal.valueOf(nights));
            estimatedTotalLabel.setText(
                    String.format("Estimated Total: ₱%,.2f (%d night%s)", total, nights, nights == 1 ? "" : "s"));
            return;
        }
        estimatedTotalLabel.setText("Estimated Total: ₱0.00");
    }

    private void handleSubmit() {
        try {
            Room selectedRoom = (Room) roomCombo.getSelectedItem();
            Validator.requireNonNull(selectedRoom, "Room");

            LocalDate checkIn = checkInPicker.getDate();
            LocalDate checkOut = checkOutPicker.getDate();
            if (checkIn == null) {
                throw new com.hotel.exception.ValidationException("Please select a check-in date.");
            }
            if (checkOut == null) {
                throw new com.hotel.exception.ValidationException("Please select a check-out date.");
            }
            Validator.validateNotInPast(checkIn, "Check-in date");

            int numGuests = Validator.parseInt(maxGuestsField.getText(), "Number of guests");
            String notes = notesArea.getText();

            Guest guest = CustomerContext.getOrCreateCurrentGuest();

            Reservation reservation = reservationService.addReservation(
                    guest.getGuestId(), selectedRoom.getRoomId(), checkIn, checkOut,
                    numGuests, ReservationStatus.PENDING, notes);

            UIUtils.showSuccess(this,
                    "Reservation submitted successfully!\nReservation ID: " + reservation.getReservationId()
                            + "\nTotal: " + String.format("₱%,.2f", reservation.getTotalAmount())
                            + "\nStatus: PENDING (awaiting confirmation)");

            clearForm();
            loadAvailableRooms();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private void clearForm() {
        checkInPicker.setDate(null);
        checkOutPicker.setDate(null);
        notesArea.setText("");
        updateMaxGuests();
        estimatedTotalLabel.setText("Estimated Total: ₱0.00");
    }

    private void updateConfirmedReservations() {
        Room selectedRoom = (Room) roomCombo.getSelectedItem();
        if (selectedRoom == null) {
            confirmedReservations = new java.util.ArrayList<>();
        } else {
            UIUtils.runSafely(this, () -> {
                confirmedReservations = reservationService.getConfirmedReservationsForRoom(selectedRoom.getRoomId());
            });
        }
    }

    private boolean isDateAvailable(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return false;
        }
        Room selectedRoom = (Room) roomCombo.getSelectedItem();
        if (selectedRoom == null) {
            return true;
        }
        for (Reservation res : confirmedReservations) {
            if (!date.isBefore(res.getCheckInDate()) && date.isBefore(res.getCheckOutDate())) {
                return false;
            }
        }
        return true;
    }

}
