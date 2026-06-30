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
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Login screen for the Hotel Reservation System.
 * <p>
 * Enforces a maximum of {@link AuthService#MAX_ATTEMPTS} failed login attempts
 * (delegated to {@link AuthService}); once exceeded, the login button is
 * disabled for that session to prevent further attempts.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private JButton loginButton;
    private JButton exitButton;
    private int attemptsUsed = 0;

    public LoginFrame() {
        super("Hotel Reservation System - Login");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 500);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        //---------------- LEFT PANEL ----------------//
        JPanel leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(320, 500));
        leftPanel.setBackground(UIUtils.PRIMARY_COLOR);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        leftPanel.add(Box.createVerticalGlue());

        JLabel lblIcon = new JLabel("\uD83C\uDFE8");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblHotel = new JLabel("GitAway: HOTEL");
        lblHotel.setForeground(Color.WHITE);
        lblHotel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        lblHotel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSystem = new JLabel("RESERVATION");
        lblSystem.setForeground(Color.WHITE);
        lblSystem.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblSystem.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblManagement = new JLabel("MANAGEMENT SYSTEM");
        lblManagement.setForeground(new Color(220, 230, 245));
        lblManagement.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblManagement.setAlignmentX(Component.CENTER_ALIGNMENT);

        leftPanel.add(lblIcon);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(lblHotel);
        leftPanel.add(lblSystem);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(lblManagement);

        leftPanel.add(Box.createVerticalGlue());

        //---------------- RIGHT PANEL ----------------//
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(new EmptyBorder(50, 60, 50, 60));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        JLabel lblWelcome = new JLabel("Welcome Back");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLogin = new JLabel("Please sign in to continue");
        lblLogin.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblLogin.setForeground(Color.GRAY);
        lblLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightPanel.add(lblWelcome);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(lblLogin);
        rightPanel.add(Box.createVerticalStrut(40));

        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new JTextField();
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setPreferredSize(new Dimension(260, 40));
        usernameField.setMinimumSize(new Dimension(260, 40));
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setPreferredSize(new Dimension(260, 40));
        passwordField.setMinimumSize(new Dimension(260, 40));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightPanel.add(lblUser);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(usernameField);
        rightPanel.add(Box.createVerticalStrut(20));

        rightPanel.add(lblPass);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(passwordField);
        rightPanel.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(UIUtils.DANGER_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(statusLabel);
        rightPanel.add(Box.createVerticalStrut(20));

        //---------------- BUTTONS ----------------//
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginButton = UIUtils.createStyledButton("Sign In", UIUtils.ACCENT_COLOR);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginButton.setFocusPainted(false);

        exitButton = new JButton("Exit");
        exitButton.setBackground(Color.LIGHT_GRAY);
        exitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        exitButton.setFocusPainted(false);

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        rightPanel.add(buttonPanel);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        //---------------- LISTENERS ----------------//
        loginButton.addActionListener(this::handleLogin);
        passwordField.addActionListener(this::handleLogin);
        usernameField.addActionListener(e -> passwordField.requestFocus());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

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

    private void routeToDashboard(User user) {
        this.dispose();
        if (user.getRole() == UserRole.ADMIN) {
            SwingUtilities.invokeLater(() -> new AdminMainFrame().setVisible(true));
        } else {
            SwingUtilities.invokeLater(() -> new CustomerMainFrame().setVisible(true));
        }
    }
}
