package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;
import com.hotel.service.RoomService;
import com.hotel.ui.common.UIUtils;
import com.hotel.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Modal dialog used for both "Add Room" and "Edit Room" in Room Management.
 * When {@code existingRoom} is null the dialog operates in add-mode;
 * otherwise it pre-fills fields and operates in edit-mode.
 */
public class RoomFormDialog extends JDialog {

    private final RoomService roomService = new RoomService();
    private final Room existingRoom; // null when adding
    private boolean saved = false;

    private JTextField roomNumberField;
    private JComboBox<RoomType> roomTypeCombo;
    private JTextField priceField;
    private JComboBox<RoomStatus> statusCombo;
    private JTextField capacityField;
    private JTextArea descriptionArea;

    public RoomFormDialog(Window owner, Room existingRoom) {
        super(owner, existingRoom == null ? "Add Room" : "Edit Room", ModalityType.APPLICATION_MODAL);
        this.existingRoom = existingRoom;
        initComponents();
        if (existingRoom != null) {
            populateFields(existingRoom);
        }
    }

    private void initComponents() {
        setSize(420, 700);
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
        form.add(formLabel("Room Number"), gbc);
        roomNumberField = new JTextField();
        roomNumberField.setPreferredSize(new Dimension(0, 36));
        roomNumberField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(roomNumberField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Room Type"), gbc);
        roomTypeCombo = new JComboBox<>(RoomType.values());
        roomTypeCombo.setPreferredSize(new Dimension(0, 36));
        roomTypeCombo.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(roomTypeCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Price per Night (₱)"), gbc);
        priceField = new JTextField();
        priceField.setPreferredSize(new Dimension(0, 36));
        priceField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(priceField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Status"), gbc);
        statusCombo = new JComboBox<>(RoomStatus.values());
        statusCombo.setPreferredSize(new Dimension(0, 36));
        statusCombo.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(statusCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Capacity (number of persons)"), gbc);
        capacityField = new JTextField();
        capacityField.setPreferredSize(new Dimension(0, 36));
        capacityField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(capacityField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Description"), gbc);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIUtils.FONT_REGULAR);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        gbc.gridy = row++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(descScroll, gbc);

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

    private void populateFields(Room room) {
        roomNumberField.setText(room.getRoomNumber());
        roomTypeCombo.setSelectedItem(room.getRoomType());
        priceField.setText(room.getPricePerNight().toPlainString());
        statusCombo.setSelectedItem(room.getStatus());
        capacityField.setText(String.valueOf(room.getCapacity()));
        descriptionArea.setText(room.getDescription());
    }

    private void handleSave() {
        try {
            String roomNumber = roomNumberField.getText().trim();
            RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
            BigDecimal price = Validator.parseBigDecimal(priceField.getText(), "Price per night");
            RoomStatus status = (RoomStatus) statusCombo.getSelectedItem();
            int capacity = Validator.parseInt(capacityField.getText(), "Capacity");
            String description = descriptionArea.getText().trim();

            Validator.requireNonBlank(roomNumber, "Room number");
            Validator.requirePositive(price, "Price per night");
            Validator.requirePositive(capacity, "Capacity");
            Validator.requireNonNull(type, "Room Type");
            Validator.requireNonNull(status, "Status");

            if (existingRoom == null) {
                roomService.addRoom(roomNumber, type, price, status, capacity, description);
                UIUtils.showSuccess(this, "Room added successfully.");
            } else {
                roomService.updateRoom(existingRoom.getRoomId(), roomNumber, type, price, status, capacity, description);
                UIUtils.showSuccess(this, "Room updated successfully.");
            }
            saved = true;
            dispose();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
