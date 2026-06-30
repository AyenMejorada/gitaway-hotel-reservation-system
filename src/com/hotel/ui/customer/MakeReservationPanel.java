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
import com.hotel.ui.common.PlaceholderTextArea;
import com.hotel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Customer screen for making a new reservation. Lists available room types
 * and lets the customer choose one with their desired dates; the maximum number
 * of guests is derived automatically from the room type, and the system calculates
 * the total cost automatically.
 */
public class MakeReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RoomService roomService = new RoomService();
    private final ReservationService reservationService = new ReservationService();

    private JComboBox<RoomType> roomTypeCombo;
    private DatePickerField checkInPicker;
    private DatePickerField checkOutPicker;
    private JTextField numGuestsField;
    private PlaceholderTextArea notesArea;

    // Room Details Card display fields
    private JLabel pricePerNightLabel;
    private JLabel capacityLabel;
    private JLabel availableRoomsLabel;
    private JTextArea descriptionArea;

    // Booking Summary display fields
    private JLabel summaryRoomTypeVal;
    private JLabel summaryPriceVal;
    private JLabel summaryCheckInVal;
    private JLabel summaryCheckOutVal;
    private JLabel summaryNightsVal;
    private JLabel summaryGuestsVal;
    private JLabel summaryTotalVal;

    public MakeReservationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(UIUtils.createSectionTitle("Make a Reservation"), BorderLayout.NORTH);

        // Main layout container (Split Left and Right Columns)
        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.BOTH;
        mainGbc.weighty = 1.0;

        // Left Panel (Form & Details Card)
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.fill = GridBagConstraints.HORIZONTAL;
        leftGbc.weightx = 1.0;
        leftGbc.gridx = 0;
        leftGbc.insets = new Insets(0, 0, 16, 0);
        int leftRow = 0;

        // Section 1: Room Selection
        leftPanel.add(createSectionHeader("Room Selection"), leftGbc);
        roomTypeCombo = new JComboBox<>(RoomType.values());
        roomTypeCombo.setPreferredSize(new Dimension(0, 36));
        roomTypeCombo.setFont(UIUtils.FONT_REGULAR);
        roomTypeCombo.addActionListener(e -> {
            checkInPicker.setDate(null);
            checkOutPicker.setDate(null);
            checkOutPicker.setEnabled(false);
            updateEstimate();
        });
        leftGbc.gridy = ++leftRow;
        leftPanel.add(roomTypeCombo, leftGbc);

        // Section 2: Room Details Card
        leftGbc.gridy = ++leftRow;
        leftPanel.add(createSectionHeader("Room Details"), leftGbc);

        JPanel detailsCard = new JPanel(new GridBagLayout());
        detailsCard.setBackground(Color.WHITE);
        detailsCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 225, 235), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints dGbc = new GridBagConstraints();
        dGbc.fill = GridBagConstraints.HORIZONTAL;
        dGbc.weightx = 1.0;
        dGbc.gridx = 0;
        dGbc.gridy = 0;
        dGbc.insets = new Insets(2, 0, 2, 0);

        pricePerNightLabel = new JLabel("Price per Night: ₱0.00");
        pricePerNightLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pricePerNightLabel.setForeground(UIUtils.PRIMARY_COLOR);
        detailsCard.add(pricePerNightLabel, dGbc);

        capacityLabel = new JLabel("Max Guest Capacity: 0");
        capacityLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        capacityLabel.setForeground(Color.DARK_GRAY);
        dGbc.gridy = 1;
        detailsCard.add(capacityLabel, dGbc);

        availableRoomsLabel = new JLabel("Available Rooms: 0");
        availableRoomsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        availableRoomsLabel.setForeground(UIUtils.SUCCESS_COLOR);
        dGbc.gridy = 2;
        detailsCard.add(availableRoomsLabel, dGbc);

        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIUtils.FONT_REGULAR);
        descriptionArea.setBackground(Color.WHITE);
        descriptionArea.setForeground(Color.GRAY);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(null);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);
        dGbc.gridy = 3;
        dGbc.insets = new Insets(8, 0, 0, 0);
        detailsCard.add(descScroll, dGbc);

        leftGbc.gridy = ++leftRow;
        leftPanel.add(detailsCard, leftGbc);

        // Section 3: Stay Information
        leftGbc.gridy = ++leftRow;
        leftPanel.add(createSectionHeader("Stay Information"), leftGbc);

        JPanel stayPanel = new JPanel(new GridBagLayout());
        stayPanel.setOpaque(false);
        GridBagConstraints sGbc = new GridBagConstraints();
        sGbc.fill = GridBagConstraints.HORIZONTAL;
        sGbc.weightx = 0.5;
        sGbc.gridy = 0;
        sGbc.insets = new Insets(0, 0, 8, 8);

        // Check-in Date
        sGbc.gridx = 0;
        stayPanel.add(formLabel("Check-in Date"), sGbc);
        checkInPicker = new DatePickerField();
        checkInPicker.setPlaceholder("Choose your check-in date");
        checkInPicker.setDateValidator(this::isDateAvailable);
        sGbc.gridy = 1;
        stayPanel.add(checkInPicker, sGbc);

        // Check-out Date
        sGbc.gridx = 1;
        sGbc.gridy = 0;
        sGbc.insets = new Insets(0, 8, 8, 0);
        stayPanel.add(formLabel("Check-out Date"), sGbc);
        checkOutPicker = new DatePickerField();
        checkOutPicker.setPlaceholder("Choose your check-out date");
        checkOutPicker.setEnabled(false);
        checkOutPicker.setDateValidator(this::isDateAvailable);
        checkOutPicker.addChangeListener(this::updateEstimate);
        sGbc.gridy = 1;
        stayPanel.add(checkOutPicker, sGbc);

        checkInPicker.addChangeListener(() -> {
            LocalDate inDate = checkInPicker.getDate();
            if (inDate != null) {
                checkOutPicker.setEnabled(true);
                checkOutPicker.setDateValidator(date -> isDateAvailable(date) && date.isAfter(inDate));
                LocalDate outDate = checkOutPicker.getDate();
                if (outDate != null && !outDate.isAfter(inDate)) {
                    checkOutPicker.setDate(null);
                }
            } else {
                checkOutPicker.setDate(null);
                checkOutPicker.setEnabled(false);
            }
            updateEstimate();
        });

        // Number of Guests
        sGbc.gridx = 0;
        sGbc.gridy = 2;
        sGbc.gridwidth = 2;
        sGbc.insets = new Insets(8, 0, 4, 0);
        stayPanel.add(formLabel("Number of Guests"), sGbc);

        numGuestsField = new JTextField("1");
        numGuestsField.setPreferredSize(new Dimension(0, 36));
        numGuestsField.setFont(UIUtils.FONT_REGULAR);
        numGuestsField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateEstimate(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateEstimate(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateEstimate(); }
        });
        sGbc.gridy = 3;
        stayPanel.add(numGuestsField, sGbc);

        // Notes Area
        sGbc.gridy = 4;
        sGbc.insets = new Insets(8, 0, 4, 0);
        stayPanel.add(formLabel("Additional Requests"), sGbc);

        notesArea = new PlaceholderTextArea("Additional requests (optional)");
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(UIUtils.FONT_REGULAR);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setPreferredSize(new Dimension(0, 80));
        sGbc.gridy = 5;
        stayPanel.add(notesScroll, sGbc);

        leftGbc.gridy = ++leftRow;
        leftPanel.add(stayPanel, leftGbc);

        // Left scroll pane wrapper for responsive scaling
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);

        mainGbc.gridx = 0;
        mainGbc.weightx = 0.55;
        mainContent.add(leftScroll, mainGbc);

        // Right Panel: Booking Summary Card
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));

        JPanel summaryCard = new JPanel(new GridBagLayout());
        summaryCard.setBackground(Color.WHITE);
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(210, 220, 235), 1, true),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rGbc.insets = new Insets(8, 0, 8, 0);
        rGbc.gridx = 0;
        int rRow = 0;

        JLabel summaryTitle = new JLabel("Booking Summary", SwingConstants.CENTER);
        summaryTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        summaryTitle.setForeground(UIUtils.PRIMARY_COLOR);
        rGbc.gridy = rRow++;
        rGbc.gridwidth = 2;
        summaryCard.add(summaryTitle, rGbc);

        JSeparator summarySep = new JSeparator();
        rGbc.gridy = rRow++;
        summaryCard.add(summarySep, rGbc);
        rGbc.gridwidth = 1;

        summaryRoomTypeVal = addSummaryRow(summaryCard, "Room Type:", rRow++);
        summaryPriceVal = addSummaryRow(summaryCard, "Price per Night:", rRow++);
        summaryCheckInVal = addSummaryRow(summaryCard, "Check-in Date:", rRow++);
        summaryCheckOutVal = addSummaryRow(summaryCard, "Check-out Date:", rRow++);
        summaryNightsVal = addSummaryRow(summaryCard, "Number of Nights:", rRow++);
        summaryGuestsVal = addSummaryRow(summaryCard, "Number of Guests:", rRow++);

        rGbc.gridy = rRow++;
        rGbc.gridwidth = 2;
        rGbc.insets = new Insets(12, 0, 12, 0);
        summaryCard.add(new JSeparator(), rGbc);
        rGbc.gridwidth = 1;

        rGbc.gridy = rRow++;
        rGbc.gridx = 0;
        rGbc.weightx = 0;
        rGbc.anchor = GridBagConstraints.WEST;
        rGbc.insets = new Insets(6, 0, 6, 0);
        JLabel estLbl = new JLabel("Estimated Total:");
        estLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        summaryCard.add(estLbl, rGbc);

        rGbc.gridx = 1;
        rGbc.weightx = 1.0;
        rGbc.anchor = GridBagConstraints.EAST;
        summaryTotalVal = new JLabel("₱0.00");
        summaryTotalVal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        summaryTotalVal.setForeground(UIUtils.PRIMARY_COLOR);
        summaryCard.add(summaryTotalVal, rGbc);

        JButton submitButton = UIUtils.createStyledButton("Submit Reservation", UIUtils.SUCCESS_COLOR);
        submitButton.setPreferredSize(new Dimension(0, 46));
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        submitButton.addActionListener(e -> handleSubmit());

        rGbc.gridy = rRow++;
        rGbc.gridx = 0;
        rGbc.gridwidth = 2;
        rGbc.insets = new Insets(16, 0, 0, 0);
        summaryCard.add(submitButton, rGbc);

        rightPanel.add(summaryCard, BorderLayout.NORTH);

        mainGbc.gridx = 1;
        mainGbc.weightx = 0.45;
        mainContent.add(rightPanel, mainGbc);

        add(mainContent, BorderLayout.CENTER);

        updateRoomTypeDetails();
        updateEstimate();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                updateRoomTypeDetails();
                updateEstimate();
            }
        });
    }

    private JComponent createSectionHeader(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(UIUtils.PRIMARY_COLOR);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BOLD);
        return label;
    }

    private JLabel addSummaryRow(JPanel parent, String labelText, int rowNum) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = rowNum;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        parent.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel val = new JLabel("-");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        parent.add(val, gbc);

        return val;
    }

    private void updateRoomTypeDetails() {
        RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
        if (type == null) {
            pricePerNightLabel.setText("Price per Night: ₱0.00");
            capacityLabel.setText("Max Guest Capacity: 0");
            availableRoomsLabel.setText("Available Rooms: 0");
            descriptionArea.setText("");
            return;
        }

        int capacity = reservationService.getCapacityForType(type);
        java.math.BigDecimal price = reservationService.getPriceForType(type);

        capacityLabel.setText("Max Guest Capacity: " + capacity);
        pricePerNightLabel.setText(String.format("Price per Night: ₱%,.2f", price));

        LocalDate checkIn = checkInPicker.getDate();
        LocalDate checkOut = checkOutPicker.getDate();
        long availableCount;
        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            availableCount = reservationService.getAvailableRoomsOfType(type, checkIn, checkOut, null).size();
        } else {
            availableCount = roomService.getAllActiveRooms().stream()
                    .filter(r -> r.getRoomType() == type && r.getStatus() != RoomStatus.MAINTENANCE)
                    .count();
        }
        availableRoomsLabel.setText("Available Rooms: " + availableCount);

        String desc = roomService.getAllActiveRooms().stream()
                .filter(r -> r.getRoomType() == type)
                .map(Room::getDescription)
                .filter(d -> d != null && !d.trim().isEmpty())
                .findFirst()
                .orElseGet(() -> {
                    switch (type) {
                        case SINGLE: return "Cozy single room, perfect for solo travelers.";
                        case DOUBLE: return "Comfortable double room for two guests.";
                        case DELUXE: return "Deluxe room offering premium comfort and amenities.";
                        case SUITE: return "Luxurious executive suite with premium services.";
                        default: return "";
                    }
                });
        descriptionArea.setText(desc);
    }

    private void updateEstimate() {
        RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
        LocalDate checkIn = checkInPicker.getDate();
        LocalDate checkOut = checkOutPicker.getDate();

        updateRoomTypeDetails();

        summaryRoomTypeVal.setText(type != null ? type.name() : "-");
        if (type != null) {
            summaryPriceVal.setText(String.format("₱%,.2f", reservationService.getPriceForType(type)));
        } else {
            summaryPriceVal.setText("-");
        }
        summaryCheckInVal.setText(checkIn != null ? checkIn.toString() : "-");
        summaryCheckOutVal.setText(checkOut != null ? checkOut.toString() : "-");

        String guestsText = numGuestsField.getText().trim();
        summaryGuestsVal.setText(guestsText.isEmpty() ? "0" : guestsText);

        if (type != null && checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
            java.math.BigDecimal price = reservationService.getPriceForType(type);
            java.math.BigDecimal total = price.multiply(java.math.BigDecimal.valueOf(nights));

            summaryNightsVal.setText(String.format("%d night%s", nights, nights == 1 ? "" : "s"));
            summaryTotalVal.setText(String.format("₱%,.2f", total));
            return;
        }

        summaryNightsVal.setText("-");
        summaryTotalVal.setText("₱0.00");
    }

    private void handleSubmit() {
        try {
            RoomType selectedType = (RoomType) roomTypeCombo.getSelectedItem();
            Validator.requireNonNull(selectedType, "Room Type");

            LocalDate checkIn = checkInPicker.getDate();
            LocalDate checkOut = checkOutPicker.getDate();
            if (checkIn == null) {
                throw new com.hotel.exception.ValidationException("Please select a check-in date.");
            }
            if (checkOut == null) {
                throw new com.hotel.exception.ValidationException("Please select a check-out date.");
            }
            Validator.validateNotInPast(checkIn, "Check-in date");
            Validator.validateDateRange(checkIn, checkOut);

            int numGuests = Validator.parseInt(numGuestsField.getText(), "Number of guests");
            if (numGuests <= 0) {
                throw new com.hotel.exception.ValidationException("Number of guests must be greater than zero.");
            }
            int capacity = reservationService.getCapacityForType(selectedType);
            if (numGuests > capacity) {
                throw new com.hotel.exception.ValidationException("Number of guests exceeds the room type's maximum capacity of " + capacity + ".");
            }

            String notes = notesArea.getText();

            if (!reservationService.isRoomTypeAvailable(selectedType, checkIn, checkOut)) {
                throw new com.hotel.exception.ValidationException("No rooms of type " + selectedType + " are available for the selected dates.");
            }

            Guest guest = CustomerContext.getOrCreateCurrentGuest();
            long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
            java.math.BigDecimal price = reservationService.getPriceForType(selectedType);
            java.math.BigDecimal total = price.multiply(java.math.BigDecimal.valueOf(nights));

            // 1. Confirmation Dialog Flow
            boolean confirmed = showConfirmationDialog(guest, selectedType, checkIn, checkOut, nights, numGuests, total, notes);
            if (!confirmed) {
                return; // Cancelled
            }

            // 2. Save reservation to database
            Reservation reservation = reservationService.addReservation(
                    guest.getGuestId(), selectedType, 0, checkIn, checkOut,
                    numGuests, ReservationStatus.PENDING, notes);

            // 3. Show Success Dialog
            showSuccessDialog(reservation, guest);

            clearForm();
            updateEstimate();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    private boolean showConfirmationDialog(Guest guest, RoomType type, LocalDate checkIn, LocalDate checkOut, long nights, int numGuests, java.math.BigDecimal total, String notes) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Confirm Your Reservation", true);
        dialog.setSize(480, 580);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        JLabel title = new JLabel("Confirm Your Reservation", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIUtils.PRIMARY_COLOR);
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = row++;
        panel.add(sep, gbc);
        gbc.gridwidth = 1;

        addDialogRow(panel, "Guest Name:", guest.getFullName(), row++);
        addDialogRow(panel, "Room Type:", type.name(), row++);
        addDialogRow(panel, "Price per Night:", String.format("₱%,.2f", reservationService.getPriceForType(type)), row++);
        addDialogRow(panel, "Check-in Date:", checkIn.toString(), row++);
        addDialogRow(panel, "Check-out Date:", checkOut.toString(), row++);
        addDialogRow(panel, "Number of Nights:", String.valueOf(nights), row++);
        addDialogRow(panel, "Number of Guests:", String.valueOf(numGuests), row++);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(statusLbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JComponent badgePanel = UIUtils.createStatusBadge("PENDING");
        panel.add(badgePanel, gbc);

        addDialogRow(panel, "Notes:", (notes == null || notes.trim().isEmpty()) ? "-" : notes, row++);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel totalLbl = new JLabel("Estimated Total:");
        totalLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(totalLbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel totalVal = new JLabel(String.format("₱%,.2f", total));
        totalVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalVal.setForeground(UIUtils.PRIMARY_COLOR);
        panel.add(totalVal, gbc);

        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 0, 16, 0);
        panel.add(new JSeparator(), gbc);
        gbc.insets = new Insets(6, 0, 6, 0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);
        JButton cancelBtn = UIUtils.createStyledButton("Cancel", UIUtils.DANGER_COLOR);
        JButton confirmBtn = UIUtils.createStyledButton("Confirm Reservation", UIUtils.SUCCESS_COLOR);
        confirmBtn.setPreferredSize(new Dimension(180, 36));

        final boolean[] confirmed = {false};
        cancelBtn.addActionListener(e -> dialog.dispose());
        confirmBtn.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(confirmBtn);
        gbc.gridy = row++;
        panel.add(btnPanel, gbc);

        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
        return confirmed[0];
    }

    private void showSuccessDialog(Reservation reservation, Guest guest) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Reservation Successful", true);
        dialog.setSize(500, 620);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        JLabel iconLbl = new JLabel("✓", SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI", Font.BOLD, 48));
        iconLbl.setForeground(UIUtils.SUCCESS_COLOR);
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(iconLbl, gbc);

        JLabel title = new JLabel("Reservation Submitted Successfully", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIUtils.PRIMARY_COLOR);
        gbc.gridy = row++;
        panel.add(title, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = row++;
        panel.add(sep, gbc);
        gbc.gridwidth = 1;

        addDialogRow(panel, "Reservation ID:", String.valueOf(reservation.getReservationId()), row++);
        addDialogRow(panel, "Guest Name:", guest.getFullName(), row++);
        addDialogRow(panel, "Room Type:", reservation.getRoomType().name(), row++);
        addDialogRow(panel, "Check-in Date:", reservation.getCheckInDate().toString(), row++);
        addDialogRow(panel, "Check-out Date:", reservation.getCheckOutDate().toString(), row++);
        addDialogRow(panel, "Number of Nights:", String.valueOf(reservation.getNumberOfNights()), row++);
        addDialogRow(panel, "Number of Guests:", String.valueOf(reservation.getNumGuests()), row++);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(statusLbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JComponent badgePanel = UIUtils.createStatusBadge("PENDING");
        panel.add(badgePanel, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel totalLbl = new JLabel("Estimated Total:");
        totalLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(totalLbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel totalVal = new JLabel(String.format("₱%,.2f", reservation.getTotalAmount()));
        totalVal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalVal.setForeground(UIUtils.PRIMARY_COLOR);
        panel.add(totalVal, gbc);

        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 0, 16, 0);
        
        JTextPane msgPane = new JTextPane();
        msgPane.setText("Your reservation has been submitted successfully. An administrator will review your booking and assign an available room. You can monitor your reservation status anytime in the 'My Reservations' page.");
        msgPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgPane.setEditable(false);
        msgPane.setBackground(Color.WHITE);
        msgPane.setMargin(new Insets(0, 24, 0, 24));
        
        // Center alignment paragraph attributes
        javax.swing.text.StyledDocument doc = msgPane.getStyledDocument();
        javax.swing.text.SimpleAttributeSet center = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setAlignment(center, javax.swing.text.StyleConstants.ALIGN_CENTER);
        javax.swing.text.StyleConstants.setLineSpacing(center, 0.2f);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        panel.add(msgPane, gbc);
        gbc.insets = new Insets(6, 0, 6, 0);

        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 0, 0, 0);
        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        okPanel.setOpaque(false);
        JButton okBtn = UIUtils.createStyledButton("OK", UIUtils.PRIMARY_COLOR);
        okBtn.addActionListener(e -> dialog.dispose());
        okPanel.add(okBtn);
        panel.add(okPanel, gbc);

        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void addDialogRow(JPanel panel, String labelText, String valText, int rowNum) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = rowNum;
        gbc.insets = new Insets(6, 0, 6, 12);

        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel val = new JLabel(valText);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(val, gbc);
    }

    private void clearForm() {
        checkInPicker.setDate(null);
        checkOutPicker.setDate(null);
        checkOutPicker.setEnabled(false);
        numGuestsField.setText("1");
        notesArea.setText("");
    }

    private boolean isDateAvailable(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return false;
        }
        RoomType selectedType = (RoomType) roomTypeCombo.getSelectedItem();
        if (selectedType == null) {
            return true;
        }
        return reservationService.isRoomTypeAvailable(selectedType, date, date.plusDays(1));
    }
}
