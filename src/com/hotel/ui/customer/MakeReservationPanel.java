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
 * and lets the customer pick one with their desired dates; the maximum number of
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
        });
        gbc.gridy = row++;
        formCard.add(roomCombo, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Check-in Date"), gbc);
        checkInPicker = new DatePickerField();
        checkInPicker.addChangeListener(this::updateEstimate);
        gbc.gridy = row++;
        formCard.add(checkInPicker, gbc);

        gbc.gridy = row++;
        formCard.add(formLabel("Check-out Date"), gbc);
        checkOutPicker = new DatePickerField();
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
            estimatedTotalLabel.setText(String.format("Estimated Total: ₱%,.2f (%d night%s)", total, nights, nights == 1 ? "" : "s"));
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

    /**
     * Simple self-contained calendar dropdown field. Displays the selected date
     * (yyyy-MM-dd) in a read-only text box; clicking the "▼" button opens a popup
     * with a month-grid calendar for picking a date, plus prev/next month navigation.
     */
    private static class DatePickerField extends JPanel {

        private final JTextField displayField = new JTextField();
        private final JButton dropButton = new JButton("▼");
        private final JPopupMenu popup = new JPopupMenu();
        private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 2, 2));
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);

        private YearMonth currentMonth = YearMonth.now();
        private LocalDate selectedDate;
        private Runnable changeListener;

        DatePickerField() {
            super(new BorderLayout(4, 0));
            setOpaque(false);

            displayField.setEditable(false);
            displayField.setPreferredSize(new Dimension(0, 36));
            displayField.setText("Select date...");

            dropButton.setFocusable(false);
            dropButton.setPreferredSize(new Dimension(36, 36));

            add(displayField, BorderLayout.CENTER);
            add(dropButton, BorderLayout.EAST);

            buildPopup();

            dropButton.addActionListener(e -> showPopup());
            displayField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showPopup();
                }
            });
        }

        private void buildPopup() {
            JPanel popupContent = new JPanel(new BorderLayout(4, 4));
            popupContent.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            popupContent.setBackground(Color.WHITE);
            popupContent.setPreferredSize(new Dimension(260, 220));

            JPanel navRow = new JPanel(new BorderLayout());
            JButton prevButton = new JButton("<");
            JButton nextButton = new JButton(">");
            prevButton.addActionListener(e -> {
                currentMonth = currentMonth.minusMonths(1);
                renderMonth();
            });
            nextButton.addActionListener(e -> {
                currentMonth = currentMonth.plusMonths(1);
                renderMonth();
            });
            monthLabel.setFont(UIUtils.FONT_BOLD);
            navRow.add(prevButton, BorderLayout.WEST);
            navRow.add(monthLabel, BorderLayout.CENTER);
            navRow.add(nextButton, BorderLayout.EAST);

            calendarGrid.setBackground(Color.WHITE);

            popupContent.add(navRow, BorderLayout.NORTH);
            popupContent.add(calendarGrid, BorderLayout.CENTER);

            popup.add(popupContent);
        }

        private void showPopup() {
            currentMonth = selectedDate != null ? YearMonth.from(selectedDate) : YearMonth.now();
            renderMonth();
            popup.show(this, 0, getHeight());
        }

        private void renderMonth() {
            calendarGrid.removeAll();
            monthLabel.setText(currentMonth.getMonth().toString() + " " + currentMonth.getYear());

            String[] dayHeaders = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
            for (String d : dayHeaders) {
                JLabel header = new JLabel(d, SwingConstants.CENTER);
                header.setFont(UIUtils.FONT_BOLD);
                calendarGrid.add(header);
            }

            LocalDate firstOfMonth = currentMonth.atDay(1);
            int leadingBlanks = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
            for (int i = 0; i < leadingBlanks; i++) {
                calendarGrid.add(new JLabel(""));
            }

            int daysInMonth = currentMonth.lengthOfMonth();
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentMonth.atDay(day);
                JButton dayButton = new JButton(String.valueOf(day));
                dayButton.setMargin(new Insets(2, 2, 2, 2));
                dayButton.setFocusable(false);
                if (date.equals(selectedDate)) {
                    dayButton.setBackground(UIUtils.ACCENT_COLOR);
                    dayButton.setForeground(Color.WHITE);
                }
                dayButton.addActionListener(e -> {
                    setDate(date);
                    popup.setVisible(false);
                });
                calendarGrid.add(dayButton);
            }

            calendarGrid.revalidate();
            calendarGrid.repaint();
        }

        void setDate(LocalDate date) {
            this.selectedDate = date;
            displayField.setText(date == null ? "Select date..." : date.format(DATE_FORMAT));
            if (changeListener != null) {
                changeListener.run();
            }
        }

        LocalDate getDate() {
            return selectedDate;
        }

        void addChangeListener(Runnable listener) {
            this.changeListener = listener;
        }
    }
}
