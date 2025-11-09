package ui;

import config.AppConfig;
import config.ConfigManager;
import util.Messages;

import javax.swing.*;
import java.awt.*;

/**
 * Settings dialog for user preferences (language selection only)
 */
public class SettingsDialog extends JDialog {
    private final JComboBox<LanguageOption> languageCombo;
    private final AgentWindow parentWindow;
    
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
    
    public SettingsDialog(AgentWindow parent) {
        super(parent, Messages.get("menu.settings"), true);
        this.parentWindow = parent;
        
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
        
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel with padding
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Language label
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel languageLabel = new JLabel(Messages.get("settings.language") + ":");
        mainPanel.add(languageLabel, gbc);
        
        // Language combo
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(languageCombo, gbc);
        
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
    
    private void saveSettings() {
        LanguageOption selected = (LanguageOption) languageCombo.getSelectedItem();
        if (selected == null) return;
        
        String oldLang = Messages.getLanguage();
        String newLang = selected.code;
        
        if (!oldLang.equals(newLang)) {
            // Change language
            Messages.setLanguage(newLang);
            
            // Save to config file
            try {
                AppConfig config = ConfigManager.loadConfig();
                config.setLanguage(newLang);
                ConfigManager.saveConfig(config);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to save configuration: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
            
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
        }
        
        dispose();
    }
}
