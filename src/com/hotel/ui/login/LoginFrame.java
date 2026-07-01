package com.hotel.ui.login;

import com.hotel.exception.AuthenticationException;
import com.hotel.exception.ValidationException;
import com.hotel.model.User;
import com.hotel.model.UserRole;
import com.hotel.service.AuthService;
import com.hotel.ui.admin.AdminMainFrame;
import com.hotel.ui.common.Session;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.customer.CustomerMainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Login screen for the Hotel Reservation System.
 * Redesigned for a clean, professional, minimalistic desktop application look.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;
    private JLabel statusLabel;
    private JButton loginButton;
    private JButton exitButton;
    private int attemptsUsed = 0;

    public LoginFrame() {
        super("Hotel Reservation System - Login");
        initComponents();
        
        // Auto-focus on username field
        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 500);
        setMinimumSize(new Dimension(850, 500));
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        //---------------- LEFT PANEL (Branding) ----------------//
        JPanel leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(350, 500));
        leftPanel.setBackground(UIUtils.PRIMARY_COLOR);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Space at the top
        leftPanel.add(Box.createVerticalStrut(40));

        // Centered near the top
        JLabel lblIcon = new JLabel("\uD83C\uDFE8");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(lblIcon);
        
        // Vertical glue to center the rest of the text elements vertically
        leftPanel.add(Box.createVerticalGlue());

        JLabel lblHotel = new JLabel("GitAway");
        lblHotel.setForeground(Color.WHITE);
        lblHotel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblHotel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(lblHotel);

        leftPanel.add(Box.createVerticalStrut(16)); // Generous spacing

        JLabel lblSystem = new JLabel("HOTEL RESERVATION SYSTEM");
        lblSystem.setForeground(new Color(190, 210, 240));
        lblSystem.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblSystem.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(lblSystem);

        leftPanel.add(Box.createVerticalStrut(20)); // Generous spacing

        JLabel lblManagement = new JLabel("Manage Reservations with Ease");
        lblManagement.setForeground(new Color(220, 230, 245));
        lblManagement.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Regular plain text for tagline
        lblManagement.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(lblManagement);

        leftPanel.add(Box.createVerticalGlue());

        //---------------- RIGHT PANEL (Login Form) ----------------//
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(new EmptyBorder(40, 50, 40, 50));
        rightPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        int row = 0;

        // Welcome Header (Large bold heading)
        JLabel lblWelcome = new JLabel("Welcome Back");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        rightPanel.add(lblWelcome, gbc);

        // Subtitle (Medium subtitle, regular text)
        JLabel lblInstruction = new JLabel("Sign in to access your account.");
        lblInstruction.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblInstruction.setForeground(Color.GRAY);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 24, 0);
        rightPanel.add(lblInstruction, gbc);

        // Username Label (Bold field label)
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 6, 0);
        rightPanel.add(lblUser, gbc);

        // Username Field (No icons inside, clean border)
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(300, 38));
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);
        rightPanel.add(usernameField, gbc);

        // Password Label (Bold field label)
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 6, 0);
        rightPanel.add(lblPass, gbc);

        // Password Field (No icons inside, clean border)
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 38));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        rightPanel.add(passwordField, gbc);

        // Show Password checkbox (Regular text)
        showPasswordCheck = new JCheckBox("Show Password");
        showPasswordCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPasswordCheck.setBackground(Color.WHITE);
        showPasswordCheck.setFocusPainted(false);
        showPasswordCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPasswordCheck.isSelected()) {
                    passwordField.setEchoChar((char) 0);
                } else {
                    passwordField.setEchoChar('•');
                }
            }
        });
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);
        rightPanel.add(showPasswordCheck, gbc);

        // Status Error Label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UIUtils.DANGER_COLOR);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 12, 0);
        rightPanel.add(statusLabel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);

        // Sign In button (Primary Action using existing blue color, matching height/width of Exit)
        loginButton = UIUtils.createStyledButton("Sign In", UIUtils.ACCENT_COLOR);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(140, 38));

        // Exit button (Neutral gray, matching height/width of Sign In)
        exitButton = new JButton("Exit");
        exitButton.setBackground(new Color(220, 220, 220));
        exitButton.setForeground(Color.DARK_GRAY);
        exitButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        exitButton.setFocusPainted(false);
        exitButton.setBorderPainted(false);
        exitButton.setOpaque(true);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitButton.setPreferredSize(new Dimension(140, 38));

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);
        rightPanel.add(buttonPanel, gbc);

        // Create Account Link (Regular text except the clickable link font style)
        JPanel signUpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        signUpPanel.setOpaque(false);
        JButton signUpButton = new JButton("New Guest? Create an Account");
        signUpButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        signUpButton.setForeground(UIUtils.ACCENT_COLOR);
        signUpButton.setContentAreaFilled(false);
        signUpButton.setBorderPainted(false);
        signUpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signUpButton.addActionListener(e -> handleSignUp());
        signUpPanel.add(signUpButton);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 0, 0);
        rightPanel.add(signUpPanel, gbc);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        //---------------- LISTENERS ----------------//
        ActionListener loginAction = this::handleLogin;
        loginButton.addActionListener(loginAction);
        
        // Enter triggers Sign In on both fields
        usernameField.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);
        
        // Register default button on root pane so pressing Enter triggers the Sign In button
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(loginButton);
            }
        });

        exitButton.addActionListener(e -> System.exit(0));
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty()) {
            statusLabel.setForeground(UIUtils.DANGER_COLOR);
            statusLabel.setText("Username is required.");
            usernameField.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            statusLabel.setForeground(UIUtils.DANGER_COLOR);
            statusLabel.setText("Password is required.");
            passwordField.requestFocusInWindow();
            return;
        }

        try {
            User user = authService.login(username, password);
            statusLabel.setForeground(UIUtils.SUCCESS_COLOR);
            statusLabel.setText("Login successful!");
            Session.setCurrentUser(user);
            routeToDashboard(user);
        } catch (ValidationException ve) {
            statusLabel.setForeground(UIUtils.DANGER_COLOR);
            statusLabel.setText(ve.getMessage());
        } catch (AuthenticationException ae) {
            attemptsUsed++;
            statusLabel.setForeground(UIUtils.DANGER_COLOR);
            statusLabel.setText(ae.getMessage());
            passwordField.setText("");

            if (authService.isLocked(username)) {
                loginButton.setEnabled(false);
                usernameField.setEnabled(false);
                passwordField.setEnabled(false);
                UIUtils.showError(this,
                        "Maximum of " + AuthService.MAX_ATTEMPTS + " login attempts exceeded.\n"
                        + "The login form is now locked for this session.\n"
                        + "Please restart the application to try again.");
            }
        }
    }

    private void handleSignUp() {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);
        if (dialog.isRegistrationSuccessful()) {
            usernameField.setText(dialog.getRegisteredUsername());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            statusLabel.setText(" ");
        }
    }

    private void routeToDashboard(User user) {
        this.dispose();
        if (user.getRole() == UserRole.ADMIN) {
            SwingUtilities.invokeLater(() -> new AdminMainFrame().setVisible(true));
        } else {
            SwingUtilities.invokeLater(() -> new CustomerMainFrame().setVisible(true));
        }
    }
}
