package ui;

import config.AppConfig;
import config.ConfigManager;
import database.EmailManager;
import util.Messages;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Settings dialog for user preferences (language, thresholds)
 */
public class SettingsDialog extends JDialog {
    private final JComboBox<LanguageOption> languageCombo;
    private final JSpinner cpuThresholdSpinner;
    private final JSpinner ramThresholdSpinner;
    private final AgentWindow parentWindow;
    private final AppConfig appConfig;
    private final EmailManager emailManager;
    private final DefaultListModel<String> emailListModel;
    private final JList<String> emailList;
    private final JTextField emailField;

    private static class LanguageOption {
        final String code;
        final String displayName;
        
        LanguageOption(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public SettingsDialog(AgentWindow parent, AppConfig config, EmailManager emailManager) {
        super(parent, Messages.get("menu.settings"), true);
        this.parentWindow = parent;
        this.appConfig = config;
        this.emailManager = emailManager;
        
        // Email list
        emailListModel = new DefaultListModel<>();
        emailList = new JList<>(emailListModel);
        emailField = new JTextField(20);

        // Language options
        LanguageOption[] languages = {
            new LanguageOption("en", "English"),
            new LanguageOption("vi", "Tiếng Việt")
        };
        
        languageCombo = new JComboBox<>(languages);
        
        // Select current language
        String currentLang = Messages.getLanguage();
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].code.equals(currentLang)) {
                languageCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Initialize threshold spinners
        SpinnerNumberModel cpuModel = new SpinnerNumberModel(
            config.getCpuThresholdPercent(), // current value
            0.0,    // min
            100.0,  // max
            1.0     // step
        );
        cpuThresholdSpinner = new JSpinner(cpuModel);
        
        SpinnerNumberModel ramModel = new SpinnerNumberModel(
            config.getRamThresholdPercent(), // current value
            0.0,    // min
            100.0,  // max
            1.0     // step
        );
        ramThresholdSpinner = new JSpinner(ramModel);
        
        initComponents();
        loadEmails();
        pack();
        setLocationRelativeTo(parent);
    }

    private void loadEmails() {
        emailListModel.clear();
        List<String> emails = emailManager.getAllEmails();
        for (String email : emails) {
            emailListModel.addElement(email);
        }
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel with padding
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Language section
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel sectionLabel1 = new JLabel("General Settings");
        sectionLabel1.setFont(sectionLabel1.getFont().deriveFont(Font.BOLD, 14f));
        mainPanel.add(sectionLabel1, gbc);
        
        // Language label
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel languageLabel = new JLabel(Messages.get("settings.language") + ":");
        mainPanel.add(languageLabel, gbc);
        
        // Language combo
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(languageCombo, gbc);
        
        // Threshold section
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 5, 5, 5);
        JLabel sectionLabel2 = new JLabel("Resource Monitoring Thresholds");
        sectionLabel2.setFont(sectionLabel2.getFont().deriveFont(Font.BOLD, 14f));
        mainPanel.add(sectionLabel2, gbc);
        
        // CPU Threshold label
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel cpuLabel = new JLabel("CPU Threshold (%):");
        mainPanel.add(cpuLabel, gbc);
        
        // CPU Threshold spinner
        gbc.gridx = 1;
        mainPanel.add(cpuThresholdSpinner, gbc);
        
        // RAM Threshold label
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel ramLabel = new JLabel("RAM Threshold (%):");
        mainPanel.add(ramLabel, gbc);
        
        // RAM Threshold spinner
        gbc.gridx = 1;
        mainPanel.add(ramThresholdSpinner, gbc);
        
        // Help text
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 5, 5);
        JLabel helpLabel = new JLabel("<html><i>Agents will receive warnings when CPU or RAM usage exceeds these thresholds.</i></html>");
        helpLabel.setFont(helpLabel.getFont().deriveFont(11f));
        mainPanel.add(helpLabel, gbc);

        // Email section
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 5, 5, 5);
        JLabel emailSectionLabel = new JLabel("Email Notifications");
        emailSectionLabel.setFont(emailSectionLabel.getFont().deriveFont(Font.BOLD, 14f));
        mainPanel.add(emailSectionLabel, gbc);

        // Email list
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        mainPanel.add(new JScrollPane(emailList), gbc);

        // Add email panel
        JPanel addEmailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addEmailPanel.add(new JLabel("New Email:"));
        addEmailPanel.add(emailField);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addEmail());
        addEmailPanel.add(addButton);

        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        mainPanel.add(addEmailPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveButton = new JButton(Messages.get("common.save"));
        saveButton.addActionListener(e -> saveSettings());
        
        JButton cancelButton = new JButton(Messages.get("common.cancel"));
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addEmail() {
        String email = emailField.getText().trim();
        if (!email.isEmpty()) {
            if (emailManager.addEmail(email)) {
                loadEmails();
                emailField.setText("");
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to add email. It might already exist.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private void saveSettings() {
        LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
        if (selected == null) return;
        
        String oldLang = Messages.getLanguage();
        String newLang = selected.code;
        
        // Get threshold values
        Double cpuThreshold = (Double) cpuThresholdSpinner.getValue();
        Double ramThreshold = (Double) ramThresholdSpinner.getValue();
        
        // Save to config file
        try {
            AppConfig config = ConfigManager.loadConfig();
            config.setLanguage(newLang);
            config.setCpuThresholdPercent(cpuThreshold);
            config.setRamThresholdPercent(ramThreshold);
            ConfigManager.saveConfig(config);
            
            // Update appConfig reference
            appConfig.setCpuThresholdPercent(cpuThreshold);
            appConfig.setRamThresholdPercent(ramThreshold);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to save configuration: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        if (!oldLang.equals(newLang)) {
            // Change language
            Messages.setLanguage(newLang);
            
            // Show restart message (not required, but UI may not update all components)
            JOptionPane.showMessageDialog(
                this,
                Messages.get("settings.restart.required"),
                Messages.get("settings.language.changed"),
                JOptionPane.INFORMATION_MESSAGE
            );
            
            // Refresh parent window UI
            if (parentWindow != null) {
                parentWindow.updateLanguage();
            }
        } else {
            // Just show success message for threshold changes
            JOptionPane.showMessageDialog(
                this,
                "Settings saved successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
        
        dispose();
    }
}
