package com.hotel.ui.login;

import com.hotel.exception.ValidationException;
import com.hotel.service.AuthService;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Registration form for new guests to sign up.
 */
public class RegisterDialog extends JDialog {

    private final AuthService authService = new AuthService();

    private JTextField txtFullName;
    private JTextField txtContactNumber;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel statusLabel;
    private JButton registerButton;
    private JButton cancelButton;

    private boolean registrationSuccessful = false;
    private String registeredUsername = "";

    public RegisterDialog(Frame parent) {
        super(parent, "Guest Registration", true);
        initComponents();
    }

    private void initComponents() {
        setSize(450, 580);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(UIUtils.PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Create Guest Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Fill in the fields to sign up");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(220, 230, 245));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(subLabel);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 4, 0);
        gbc.gridx = 0;
        int row = 0;

        // Full Name
        gbc.gridy = row++;
        formPanel.add(createFormLabel("Full Name"), gbc);
        txtFullName = createFormField();
        gbc.gridy = row++;
        formPanel.add(txtFullName, gbc);

        // Contact Number
        gbc.gridy = row++;
        formPanel.add(createFormLabel("Contact Number"), gbc);
        txtContactNumber = createFormField();
        gbc.gridy = row++;
        formPanel.add(txtContactNumber, gbc);

        // Username
        gbc.gridy = row++;
        formPanel.add(createFormLabel("Username"), gbc);
        txtUsername = createFormField();
        gbc.gridy = row++;
        formPanel.add(txtUsername, gbc);

        // Password
        gbc.gridy = row++;
        formPanel.add(createFormLabel("Password"), gbc);
        txtPassword = createPasswordField();
        gbc.gridy = row++;
        formPanel.add(txtPassword, gbc);

        // Confirm Password
        gbc.gridy = row++;
        formPanel.add(createFormLabel("Confirm Password"), gbc);
        txtConfirmPassword = createPasswordField();
        gbc.gridy = row++;
        formPanel.add(txtConfirmPassword, gbc);

        // Status Label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(UIUtils.DANGER_COLOR);
        gbc.gridy = row++;
        gbc.insets = new Insets(10, 0, 10, 0);
        formPanel.add(statusLabel, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        registerButton = UIUtils.createStyledButton("Register", UIUtils.SUCCESS_COLOR);
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        cancelButton = new JButton("Cancel");
        cancelButton.setBackground(Color.LIGHT_GRAY);
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setFocusPainted(false);

        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(buttonPanel, gbc);

        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);

        // Listeners
        registerButton.addActionListener(e -> handleRegister());
        cancelButton.addActionListener(e -> dispose());
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(Color.DARK_GRAY);
        return label;
    }

    private JTextField createFormField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(0, 32));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(new Dimension(0, 32));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return field;
    }

    private void handleRegister() {
        String fullName = txtFullName.getText();
        String contact = txtContactNumber.getText();
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());

        try {
            authService.register(fullName, contact, username, password, confirmPassword);
            registrationSuccessful = true;
            registeredUsername = username.trim();
            UIUtils.showSuccess(this, "Registration Successful!\nYou can now log in with your credentials.");
            dispose();
        } catch (ValidationException ve) {
            statusLabel.setForeground(UIUtils.DANGER_COLOR);
            statusLabel.setText(ve.getMessage());
        } catch (Exception e) {
            statusLabel.setForeground(UIUtils.DANGER_COLOR);
            statusLabel.setText("An unexpected database error occurred.");
            e.printStackTrace();
        }
    }

    public boolean isRegistrationSuccessful() {
        return registrationSuccessful;
    }

    public String getRegisteredUsername() {
        return registeredUsername;
    }
}
