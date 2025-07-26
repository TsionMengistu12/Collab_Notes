package src;
import src.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginDialog extends JDialog {
    
    // Green theme colors
    private static final Color PRIMARY_GREEN = new Color(46, 125, 50);
    private static final Color LIGHT_GREEN = new Color(129, 199, 132);
    private static final Color DARK_GREEN = new Color(27, 94, 32);
    private static final Color ACCENT_GREEN = new Color(102, 187, 106);
    private static final Color BACKGROUND_GREEN = new Color(232, 245, 233);
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private boolean authenticated = false;
    private String username = null;
    
    public LoginDialog(Frame parent) {
        super(parent, "Collaborative Notepad - Login", true);
        initializeComponents();
        layoutComponents();
        applyGreenTheme();
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        
        // Style text fields
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Style buttons
        styleButton(loginButton, PRIMARY_GREEN);
        styleButton(registerButton, ACCENT_GREEN);
        
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
            }
        });
        
        // Allow pressing Enter to login
        getRootPane().setDefaultButton(loginButton);
    }
    
    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Main panel with green background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_GREEN);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        
        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(BACKGROUND_GREEN);
        JLabel titleLabel = new JLabel("Welcome to Collaborative Notepad");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(DARK_GREEN);
        titlePanel.add(titleLabel);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GREEN, 2),
            new EmptyBorder(25, 25, 25, 25)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(DARK_GREEN);
        formPanel.add(userLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.gridwidth = 2;
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GREEN),
            new EmptyBorder(8, 10, 8, 10)
        ));
        formPanel.add(usernameField, gbc);
        
        // Password field
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passLabel.setForeground(DARK_GREEN);
        formPanel.add(passLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.gridwidth = 2;
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GREEN),
            new EmptyBorder(8, 10, 8, 10)
        ));
        formPanel.add(passwordField, gbc);
        
        // Status label
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(statusLabel, gbc);
        
        // Button panel
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(loginButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(registerButton);
        formPanel.add(buttonPanel, gbc);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
        mainPanel.add(formPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        pack();
    }
    
    private void applyGreenTheme() {
        getContentPane().setBackground(BACKGROUND_GREEN);
    }
    
    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password are required", Color.RED);
            return;
        }
        
        if (UserService.authenticateUser(username, password)) {
            this.authenticated = true;
            this.username = username;
            dispose();
        } else {
            showStatus("Invalid username or password", Color.RED);
            passwordField.setText("");
        }
    }
    
    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password are required", Color.RED);
            return;
        }
        
        if (UserService.userExists(username)) {
            showStatus("Username already exists", Color.RED);
            return;
        }
        
        if (password.length() < 6) {
            showStatus("Password must be at least 6 characters", Color.RED);
            return;
        }
        
        if (UserService.registerUser(username, password)) {
            showStatus("Registration successful! You can now login.", PRIMARY_GREEN);
        } else {
            showStatus("Registration failed. Please try again.", Color.RED);
        }
    }
    
    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public String getUsername() {
        return username;
    }
    
    public static LoginResult showLoginDialog(Frame parent) {
        LoginDialog dialog = new LoginDialog(parent);
        dialog.setVisible(true);
        return new LoginResult(dialog.isAuthenticated(), dialog.getUsername());
    }
    
    public static class LoginResult {
        private final boolean authenticated;
        private final String username;
        
        public LoginResult(boolean authenticated, String username) {
            this.authenticated = authenticated;
            this.username = username;
        }
        
        public boolean isAuthenticated() {
            return authenticated;
        }
        
        public String getUsername() {
            return username;
        }
    }
}
