package com.hotel.ui.customer;

import com.hotel.exception.HotelException;
import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
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
 * Customer screen for making a new reservation. Lists currently available rooms
 * and lets the customer pick one with their desired dates and number of guests;
 * the system calculates the total cost automatically.
 */
public class MakeReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RoomService roomService = new RoomService();
    private final ReservationService reservationService = new ReservationService();

    private JComboBox<Room> roomCombo;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JTextField numGuestsField;
    private JTextArea notesArea;
    private JLabel estimatedTotalLabel;

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
        roomCombo.addActionListener(e -> updateEstimate());
        gbc.gridy = row++;
        formCard.add(roomCombo, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Check-in Date (yyyy-MM-dd)"), gbc);
        checkInField = new JTextField();
        checkInField.setPreferredSize(new Dimension(0, 36));
        checkInField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        formCard.add(checkInField, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Check-out Date (yyyy-MM-dd)"), gbc);
        checkOutField = new JTextField();
        checkOutField.setPreferredSize(new Dimension(0, 36));
        checkOutField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        formCard.add(checkOutField, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Number of Guests"), gbc);
        numGuestsField = new JTextField("1");
        numGuestsField.setPreferredSize(new Dimension(0, 36));
        numGuestsField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        formCard.add(numGuestsField, gbc);

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
            updateEstimate();
        });
    }

    private void updateEstimate() {
        Room room = (Room) roomCombo.getSelectedItem();
        try {
            LocalDate checkIn = LocalDate.parse(checkInField.getText().trim(), DATE_FORMAT);
            LocalDate checkOut = LocalDate.parse(checkOutField.getText().trim(), DATE_FORMAT);
            if (room != null && checkOut.isAfter(checkIn)) {
                long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
                java.math.BigDecimal total = room.getPricePerNight().multiply(java.math.BigDecimal.valueOf(nights));
                estimatedTotalLabel.setText(String.format("Estimated Total: ₱%,.2f (%d night%s)", total, nights, nights == 1 ? "" : "s"));
                return;
            }
        } catch (DateTimeParseException | NullPointerException ignored) {
            // Fall through to default display while the user is still typing dates.
        }
        estimatedTotalLabel.setText("Estimated Total: ₱0.00");
    }

    private void handleSubmit() {
        try {
            Room selectedRoom = (Room) roomCombo.getSelectedItem();
            Validator.requireNonNull(selectedRoom, "Room");

            LocalDate checkIn = parseDate(checkInField.getText(), "Check-in date");
            LocalDate checkOut = parseDate(checkOutField.getText(), "Check-out date");
            Validator.validateNotInPast(checkIn, "Check-in date");
            int numGuests = Validator.parseInt(numGuestsField.getText(), "Number of guests");
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

    private LocalDate parseDate(String text, String fieldName) {
        Validator.requireNonBlank(text, fieldName);
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new com.hotel.exception.ValidationException(
                    fieldName + " must be in yyyy-MM-dd format (e.g. 2026-07-15).");
        }
    }

    private void clearForm() {
        checkInField.setText("");
        checkOutField.setText("");
        numGuestsField.setText("1");
        notesArea.setText("");
        estimatedTotalLabel.setText("Estimated Total: ₱0.00");
    }
}
