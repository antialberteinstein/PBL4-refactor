package ui;

import service.AuthService;
import util.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Login Dialog for Manager GUI.
 * 
 * Modal dialog that prompts user for username and password.
 * Validates credentials via AuthService.
 */
public class LoginDialog extends JDialog {
    
    private static final long serialVersionUID = 1L;
    
    // ============================================================================== //
    //                                   COMPONENTS                                   //
    // ============================================================================== //
    
    private final AuthService authService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private JLabel messageLabel;
    private boolean loginSuccessful = false;
    
    
    // ============================================================================== //
    //                                  CONSTRUCTOR                                   //
    // ============================================================================== //
    
    /**
     * Constructor.
     * 
     * @param authService Authentication service for credential validation
     */
    public LoginDialog(AuthService authService) {
        this.authService = authService;
        
        initComponents();
        setupLayout();
        setupListeners();
    }
    
    
    // ============================================================================== //
    //                              INITIALIZATION METHODS                            //
    // ============================================================================== //
    
    /**
     * Initialize UI components.
     */
    private void initComponents() {
        setTitle(Messages.get("auth.title"));
        setModal(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Create components
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton(Messages.get("auth.login"));
        cancelButton = new JButton(Messages.get("auth.cancel"));
        messageLabel = new JLabel(" ");
        messageLabel.setForeground(Color.RED);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    /**
     * Setup layout with components.
     */
    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title panel
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel(Messages.get("auth.gui.header"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Username row
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel(Messages.get("auth.username") + ":"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(usernameField, gbc);
        
        // Password row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel(Messages.get("auth.password") + ":"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(passwordField, gbc);
        
        // Message row
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(messageLabel, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(loginButton);
        
        // Add panels to main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null); // Center on screen
    }
    
    /**
     * Setup event listeners.
     */
    private void setupListeners() {
        // Login button action
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        // Cancel button action
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginSuccessful = false;
                dispose();
            }
        });
        
        // Enter key in username field - move to password
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocus();
                }
            }
        });
        
        // Enter key in password field - attempt login
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });
    }
    
    
    // ============================================================================== //
    //                            AUTHENTICATION METHODS                              //
    // ============================================================================== //
    
    /**
     * Perform login attempt.
     */
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Validate inputs
        if (username.isEmpty()) {
            showMessage(Messages.get("auth.gui.empty.username"));
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showMessage(Messages.get("auth.gui.empty.password"));
            passwordField.requestFocus();
            return;
        }
        
        // Attempt authentication
        if (authService.login(username, password)) {
            loginSuccessful = true;
            dispose();
        } else {
            passwordField.setText(""); // Clear password field
            showMessage(Messages.get("auth.gui.invalid"));
            passwordField.requestFocus(); // Focus on password, not username
        }
    }
    
    /**
     * Show error message.
     * 
     * @param message Message to display
     */
    private void showMessage(String message) {
        messageLabel.setText(message);
    }
    
    
    // ============================================================================== //
    //                                PUBLIC INTERFACE                                //
    // ============================================================================== //
    
    /**
     * Check if login was successful.
     * 
     * @return true if user logged in successfully, false otherwise
     */
    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }
}
