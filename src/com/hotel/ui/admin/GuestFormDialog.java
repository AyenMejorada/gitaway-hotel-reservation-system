package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Guest;
import com.hotel.service.GuestService;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog used for both "Add Guest" and "Edit Guest" in Guest Management.
 */
public class GuestFormDialog extends JDialog {

    private final GuestService guestService = new GuestService();
    private final Guest existingGuest; // null when adding
    private boolean saved = false;

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextField idNumberField;

    public GuestFormDialog(Window owner, Guest existingGuest) {
        super(owner, existingGuest == null ? "Add Guest" : "Edit Guest", ModalityType.APPLICATION_MODAL);
        this.existingGuest = existingGuest;
        initComponents();
        if (existingGuest != null) {
            populateFields(existingGuest);
        }
    }

    private void initComponents() {
        setSize(420, 600);
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
        form.add(formLabel("First Name"), gbc);
        firstNameField = new JTextField();
        gbc.gridy = row++;
        form.add(firstNameField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Last Name"), gbc);
        lastNameField = new JTextField();
        gbc.gridy = row++;
        form.add(lastNameField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Email"), gbc);
        emailField = new JTextField();
        gbc.gridy = row++;
        form.add(emailField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Phone Number"), gbc);
        phoneField = new JTextField();
        gbc.gridy = row++;
        form.add(phoneField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Address"), gbc);
        addressField = new JTextField();
        gbc.gridy = row++;
        form.add(addressField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("ID Number (optional)"), gbc);
        idNumberField = new JTextField();
        gbc.gridy = row++;
        form.add(idNumberField, gbc);

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

    private void populateFields(Guest guest) {
        firstNameField.setText(guest.getFirstName());
        lastNameField.setText(guest.getLastName());
        emailField.setText(guest.getEmail());
        phoneField.setText(guest.getPhone());
        addressField.setText(guest.getAddress());
        idNumberField.setText(guest.getIdNumber());
    }

    private void handleSave() {
        try {
            String firstName = firstNameField.getText();
            String lastName = lastNameField.getText();
            String email = emailField.getText();
            String phone = phoneField.getText();
            String address = addressField.getText();
            String idNumber = idNumberField.getText();

            if (existingGuest == null) {
                guestService.addGuest(null, firstName, lastName, email, phone, address, idNumber);
                UIUtils.showSuccess(this, "Guest added successfully.");
            } else {
                guestService.updateGuest(existingGuest.getGuestId(), firstName, lastName, email, phone, address, idNumber);
                UIUtils.showSuccess(this, "Guest updated successfully.");
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
