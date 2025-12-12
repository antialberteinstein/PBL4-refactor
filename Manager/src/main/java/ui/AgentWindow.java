package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import database.ComputerManager;
import database.SessionManager;
import database.ProcessManager;
import service.HostScanner;
import service.SessionRetriever;
import service.RemoteCommandClient;
import service.ResourceMonitor;
import model.Computer;
import model.Session;
import model.Process;
import util.Messages;
import config.AppConfig;
import database.EmailManager;

/**
 * AgentWindow - GUI interface for Manager application
 * 
 * Provides graphical view of:
 * - Discovered Agents
 * - Session data collection
 * - Process monitoring
 */
public class AgentWindow extends JFrame {
    
    private final ComputerManager computerManager;
    private final SessionManager sessionManager;
    private final ProcessManager processManager;
    private final HostScanner hostScanner;
    private final SessionRetriever sessionRetriever;
    private final RemoteCommandClient remoteCommandClient;
    private final ResourceMonitor resourceMonitor;
    private final AppConfig appConfig;
    private final EmailManager emailManager;
    
    private JTextArea logArea;
    private JButton scanButton;
    private JButton refreshButton;
    private JList<String> agentList;
    private DefaultListModel<String> agentListModel;
    
    // Chart components with type switching
    private ChartPanel cpuChartPanel;
    private ChartPanel ramChartPanel;
    private ProcessesList processesList;
    private JTextArea agentDetailsArea;
    private String selectedAgentMac;
    private Timer updateTimer;
    private JPanel chartsPanel;
    
    // Flag to track if RAM chart has been initialized with correct max value
    private boolean ramChartInitialized = false;
    
    /**
     * Constructor - Initialize GUI with all necessary dependencies
     */
    public AgentWindow(ComputerManager computerManager, 
                      SessionManager sessionManager,
                      ProcessManager processManager,
                      HostScanner hostScanner,
                      SessionRetriever sessionRetriever,
                      RemoteCommandClient remoteCommandClient,
                      ResourceMonitor resourceMonitor,
                      AppConfig appConfig,
                      EmailManager emailManager) {
        this.computerManager = computerManager;
        this.sessionManager = sessionManager;
        this.processManager = processManager;
        this.hostScanner = hostScanner;
        this.sessionRetriever = sessionRetriever;
        this.remoteCommandClient = remoteCommandClient;
        this.resourceMonitor = resourceMonitor;
        this.appConfig = appConfig;
        this.emailManager = emailManager;
        
        initializeGUI();
        
        // Enable multiple selection for process table after GUI is initialized
        processesList.setMultipleSelectionEnabled(true);
    }
    
    /**
     * Initialize GUI components and layout
     */
    private void initializeGUI() {
        setTitle(Messages.get("window.title"));
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        
        // Create menu bar
        setJMenuBar(createMenuBar());
        
        // Main layout
        setLayout(new BorderLayout(10, 10));
        
        // Top panel - Controls
        JPanel topPanel = createControlPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // Center - Split view
        JSplitPane splitPane = createSplitPane();
        add(splitPane, BorderLayout.CENTER);
        
        // Bottom - Log area
        JPanel bottomPanel = createLogPanel();
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Load initial data
        refreshAgentList();
    }
    
    /**
     * Create menu bar with complete menu structure
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // ===== FILE MENU =====
        JMenu fileMenu = new JMenu(Messages.get("menu.file"));
        fileMenu.setMnemonic('F');
        
        JMenuItem settingsItem = new JMenuItem(Messages.get("menu.file.settings"));
        settingsItem.setMnemonic('S');
        settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        settingsItem.addActionListener(e -> openSettings());
        
        JMenuItem exitItem = new JMenuItem(Messages.get("menu.file.exit"));
        exitItem.setMnemonic('X');
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.addActionListener(e -> exitApplication());
        
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // ===== VIEW MENU =====
        JMenu viewMenu = new JMenu(Messages.get("menu.view"));
        viewMenu.setMnemonic('V');
        
        JMenuItem refreshItem = new JMenuItem(Messages.get("menu.view.refresh"));
        refreshItem.setMnemonic('R');
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshItem.addActionListener(e -> refreshAll());
        
        JMenuItem clearLogItem = new JMenuItem(Messages.get("menu.view.clear.log"));
        clearLogItem.setMnemonic('C');
        clearLogItem.addActionListener(e -> clearLog());
        
        viewMenu.add(refreshItem);
        viewMenu.addSeparator();
        viewMenu.add(clearLogItem);
        
        // ===== TOOLS MENU =====
        JMenu toolsMenu = new JMenu(Messages.get("menu.tools"));
        toolsMenu.setMnemonic('T');
        
        JMenuItem scanItem = new JMenuItem(Messages.get("menu.tools.scan"));
        scanItem.setMnemonic('S');
        scanItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        scanItem.addActionListener(e -> triggerScan());
        
        JMenuItem exportItem = new JMenuItem(Messages.get("menu.tools.export"));
        exportItem.setMnemonic('E');
        exportItem.addActionListener(e -> exportData());
        
        toolsMenu.add(scanItem);
        toolsMenu.addSeparator();
        toolsMenu.add(exportItem);
        
        // ===== HELP MENU =====
        JMenu helpMenu = new JMenu(Messages.get("menu.help"));
        helpMenu.setMnemonic('H');
        
        JMenuItem docItem = new JMenuItem(Messages.get("menu.help.documentation"));
        docItem.setMnemonic('D');
        docItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        docItem.addActionListener(e -> showDocumentation());
        
        JMenuItem aboutItem = new JMenuItem(Messages.get("menu.help.about"));
        aboutItem.setMnemonic('A');
        aboutItem.addActionListener(e -> showAbout());
        
        JMenuItem versionItem = new JMenuItem(Messages.get("menu.help.version"));
        versionItem.setMnemonic('V');
        versionItem.addActionListener(e -> showVersion());
        
        helpMenu.add(docItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);
        helpMenu.add(versionItem);
        
        // Add all menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    /**
     * Open settings dialog
     */
    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, appConfig, emailManager);
        dialog.setVisible(true);
    }
    
    /**
     * Update UI language after settings change
     */
    public void updateLanguage() {
        // Update window title
        setTitle(Messages.get("window.title"));
        
        // Rebuild menu bar
        setJMenuBar(createMenuBar());
        
        // Refresh all components
        revalidate();
        repaint();
    }
    
    // ============================================================================
    //                            MENU ACTION METHODS
    // ============================================================================
    
    /**
     * Exit application with confirmation
     */
    private void exitApplication() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            Messages.get("common.confirm") + ": " + Messages.get("menu.file.exit") + "?",
            Messages.get("menu.file.exit"),
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
    
    /**
     * Refresh all data (agents, sessions, processes)
     */
    private void refreshAll() {
        refreshAgentList();
        if (selectedAgentMac != null) {
            updateSessionData(selectedAgentMac);
        }
        log("All data refreshed");
    }
    
    /**
     * Clear activity log
     */
    private void clearLog() {
        logArea.setText("");
        log("Log cleared");
    }
    
    /**
     * Trigger network scan
     */
    private void triggerScan() {
        scanNetwork();
    }
    
    /**
     * Export data to file
     */
    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.get("menu.tools.export"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(
                this,
                Messages.get("menu.tools.export") + " - " + Messages.get("common.info") + ": Feature coming soon",
                Messages.get("common.info"),
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    /**
     * Show documentation
     */
    private void showDocumentation() {
        String docs = "=== Manager Documentation ===\n\n" +
                     "1. Scan Network: Discover agents on the network\n" +
                     "2. Select Agent: Click on an agent to view details\n" +
                     "3. View Charts: Monitor CPU and RAM usage\n" +
                     "4. View Processes: See running processes on selected agent\n" +
                     "5. Settings: Configure user preferences\n\n" +
                     "For more information, check README.md files in project root.";
        
        JTextArea textArea = new JTextArea(docs);
        textArea.setEditable(false);
        textArea.setRows(15);
        textArea.setColumns(50);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            Messages.get("menu.help.documentation"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Show about dialog
     */
    private void showAbout() {
        String about = Messages.get("app.title") + "\n\n" +
                      "Version: 1.0.0\n" +
                      "Author: PBL4 Team\n" +
                      "Year: 2025\n\n" +
                      "A comprehensive system monitoring solution\n" +
                      "for managing multiple agents and tracking\n" +
                      "system resources in real-time.";
        
        JOptionPane.showMessageDialog(
            this,
            about,
            Messages.get("menu.help.about"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Show version information
     */
    private void showVersion() {
        String version = "Manager Version Information\n\n" +
                        "Version: 1.0.0\n" +
                        "Build Date: November 2025\n" +
                        "Java Version: " + System.getProperty("java.version") + "\n" +
                        "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version");
        
        JOptionPane.showMessageDialog(
            this,
            version,
            Messages.get("menu.help.version"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Create control panel with action buttons
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.get("window.actions")));
        
        // Scan button
        scanButton = new JButton(Messages.get("menu.tools.scan"));
        scanButton.setToolTipText(Messages.get("window.scan.tooltip"));
        scanButton.addActionListener(e -> scanNetwork());
        panel.add(scanButton);
        
        // Refresh button
        refreshButton = new JButton(Messages.get("menu.view.refresh"));
        refreshButton.setToolTipText("Refresh agent list from database");
        refreshButton.addActionListener(e -> refreshAgentList());
        panel.add(refreshButton);
        
        // Kill Process button
        JButton killProcessButton = new JButton("Kill Process");
        killProcessButton.setToolTipText("Kill selected process(es) on the selected agent");
        killProcessButton.addActionListener(e -> killSelectedProcesses());
        panel.add(killProcessButton);
        
        // Send Message button
        JButton sendMessageButton = new JButton("Send Message");
        sendMessageButton.setToolTipText("Send a message to the selected agent");
        sendMessageButton.addActionListener(e -> sendMessageToAgent());
        panel.add(sendMessageButton);
        
        // Shutdown button
        JButton shutdownButton = new JButton("Shutdown Agent");
        shutdownButton.setToolTipText("Shutdown the selected agent computer");
        shutdownButton.setForeground(Color.RED);
        shutdownButton.addActionListener(e -> shutdownAgent());
        panel.add(shutdownButton);
        
        // Clear log button
        JButton clearLogButton = new JButton(Messages.get("window.clear.log"));
        clearLogButton.addActionListener(e -> logArea.setText(""));
        panel.add(clearLogButton);

        // Delete Agent button
        JButton deleteAgentButton = new JButton("Delete Agent");
        deleteAgentButton.setToolTipText("Delete the selected agent from database");
        deleteAgentButton.setForeground(Color.RED);
        deleteAgentButton.addActionListener(e -> deleteAgent());
        panel.add(deleteAgentButton);
        
        return panel;
    }
    
    /**
     * Create split pane with agent list and details
     */
    private JSplitPane createSplitPane() {
        // Left panel - Agent list
        JPanel leftPanel = createAgentListPanel();
        
        // Right panel - Agent details with charts and processes
        JPanel rightPanel = createDetailsPanel();
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.3);
        
        return splitPane;
    }
    
    /**
     * Create details panel with charts and process list
     */
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.get("window.agent.details")));
        
        // Top - Agent info
        agentDetailsArea = new JTextArea(3, 40);
        agentDetailsArea.setEditable(false);
        agentDetailsArea.setText(Messages.get("window.select.agent"));
        agentDetailsArea.setBackground(new Color(245, 245, 245));
        JScrollPane infoScroll = new JScrollPane(agentDetailsArea);
        panel.add(infoScroll, BorderLayout.NORTH);
        
        // Center - Split between charts and processes
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.4);
        
        // Top of center - Charts
        JPanel chartsPanel = createChartsPanel();
        centerSplit.setTopComponent(chartsPanel);
        
        // Bottom of center - Processes
        processesList = new ProcessesList();
        centerSplit.setBottomComponent(processesList);
        
        panel.add(centerSplit, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create charts panel for CPU and RAM
     */
    private JPanel createChartsPanel() {
        chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        chartsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // CPU Chart Panel with type selector (single value format)
        cpuChartPanel = new ChartPanel(Messages.get("window.cpu.usage"), "%", new Color(52, 152, 219), 100.0, 50, "single");
        chartsPanel.add(cpuChartPanel);
        
        // RAM Chart Panel with type selector (ratio format for current/total)
        ramChartPanel = new ChartPanel(Messages.get("window.ram.usage"), "GB", new Color(46, 204, 113), 64.0, 50, "ratio");
        chartsPanel.add(ramChartPanel);
        
        return chartsPanel;
    }
    
    /**
     * Create agent list panel
     */
    private JPanel createAgentListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(Messages.get("window.discovered.agents")));
        
        agentListModel = new DefaultListModel<>();
        agentList = new JList<>(agentListModel);
        agentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add selection listener to update details when agent is selected
        agentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onAgentSelected();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(agentList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Agent");
        deleteItem.addActionListener(e -> deleteAgent());
        contextMenu.add(deleteItem);

        agentList.setComponentPopupMenu(contextMenu);
        
        return panel;
    }
    
    /**
     * Create log panel at bottom
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(Messages.get("window.activity.log")));
        panel.setPreferredSize(new Dimension(0, 150));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Scan network for Agents and refresh the list
     */
    /**
     * Scan network for Agents
     */
    private void scanNetwork() {
        log("Scanning network for Agents...");
        scanButton.setEnabled(false);
        scanButton.setText(Messages.get("window.scanning"));
        
        // Run scan in background thread
        new Thread(() -> {
            try {
                hostScanner.scan();
                Thread.sleep(3000); // Wait for responses
                SwingUtilities.invokeLater(() -> {
                    // Auto-refresh after scan
                    refreshAgentList();
                    scanButton.setEnabled(true);
                    scanButton.setText(Messages.get("menu.tools.scan"));
                    log("Network scan completed");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("Error during scan: " + e.getMessage());
                    scanButton.setEnabled(true);
                    scanButton.setText(Messages.get("menu.tools.scan"));
                });
            }
        }).start();
    }
    
    /**
     * Refresh agent list from database
     */
    private void refreshAgentList() {
        agentListModel.clear();
        
        java.util.Map<String, String> ipMacMap = computerManager.getAllComputerIpMap();
        if (ipMacMap.isEmpty()) {
            agentListModel.addElement("No Agents discovered yet");
            log("No Agents in database");
        } else {
            for (java.util.Map.Entry<String, String> entry : ipMacMap.entrySet()) {
                String mac = entry.getKey();
                String ip = entry.getValue();
                agentListModel.addElement(mac + " @ " + ip);
            }
            log("Loaded " + ipMacMap.size() + " Agent(s)");
        }
    }
    
    /**
     * Add message to log area
     */
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto-scroll
        });
    }
    
    /**
     * Handle agent selection - load and display agent data
     */
    private void onAgentSelected() {
        String selectedValue = agentList.getSelectedValue();
        if (selectedValue == null || selectedValue.equals("No Agents discovered yet")) {
            selectedAgentMac = null;
            clearAgentDetails();
            stopAutoUpdate();
            return;
        }
        
        // Extract MAC address from "MAC @ IP" format
        String[] parts = selectedValue.split(" @ ");
        if (parts.length >= 1) {
            selectedAgentMac = parts[0];
            loadAgentDetails(selectedAgentMac);
            startAutoUpdate();
        }
    }
    
    /**
     * Load and display details for selected agent
     */
    private void loadAgentDetails(String macAddress) {
        try {
            // Get computer info
            model.Computer computer = computerManager.getComputerByMacAddress(macAddress);
            if (computer != null) {
                StringBuilder info = new StringBuilder();
                info.append("MAC: ").append(computer.getMacAddress()).append("\n");
                info.append("IP: ").append(computer.getIpAddress()).append("\n");
                info.append("Hostname: ").append(computer.getHostname() != null ? computer.getHostname() : "N/A").append(" | ");
                info.append("OS: ").append(computer.getOs() != null ? computer.getOs() : "N/A").append(" | ");
                info.append("CPU: ").append(computer.getCpuName() != null ? computer.getCpuName() : "N/A").append(" (")
                     .append(computer.getPhysicalCores()).append(" cores)");
                agentDetailsArea.setText(info.toString());
            }
            
            // Load latest session data
            updateSessionData(macAddress);
            
            log("Loaded details for Agent: " + macAddress);
        } catch (Exception e) {
            log("Error loading agent details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update session data (CPU, RAM, processes) for selected agent
     */
    private void updateSessionData(String macAddress) {
        // Request fresh data from Agent in background
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Request fresh session data from Agent
                requestSessionDataFromAgent(macAddress);
                
                // Wait for response to be received and saved
                Thread.sleep(1000);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    // Now read from database and update UI
                    List<Session> sessions = sessionManager.getSessionsByMac(macAddress, 1);
                    if (sessions != null && !sessions.isEmpty()) {
                        Session latestSession = sessions.get(0);
                        
                        // Update CPU chart
                        cpuChartPanel.addDataPoint(latestSession.getCpuUsage());
                        
                        double ramGB = latestSession.getRamUsage() / (1024.0 * 1024.0 * 1024.0);
                        
                        // Initialize RAM chart with correct max value on first load
                        if (!ramChartInitialized) {
                            double totalRamGB = latestSession.getTotalRam() / (1024.0 * 1024.0 * 1024.0);
                            if (totalRamGB > 0) {
                                // Update max value for all RAM chart types
                                ramChartPanel.setMaxValue(totalRamGB);
                                ramChartInitialized = true;
                            }
                        }
                        
                        // Update RAM chart
                        ramChartPanel.addDataPoint(ramGB);
                        
                        // Update process list
                        List<Process> processes = processManager.getProcessesBySessionId((int) latestSession.getId());
                        processesList.updateProcesses(processes);
                        
                        log("Updated: CPU=" + String.format("%.1f%%", latestSession.getCpuUsage()) + 
                            ", RAM=" + String.format("%.2fGB", ramGB) + 
                            ", Processes=" + processes.size());
                    } else {
                        log("No session data found for Agent: " + macAddress);
                    }
                } catch (Exception e) {
                    log("Error updating session data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }
    
    /**
     * Request fresh session data from Agent
     */
    private void requestSessionDataFromAgent(String macAddress) {
        try {
            // Get Agent's IP address
            model.Computer computer = computerManager.getComputerByMacAddress(macAddress);
            if (computer == null) {
                log("ERROR: Computer not found for MAC: " + macAddress);
                return;
            }
            
            String agentIp = computer.getIpAddress();
            
            log("Requesting session data from Agent: " + agentIp);
            
            // Request session data from Agent (port is now handled by SessionRetriever via AppConfig)
            sessionRetriever.sendGetSessions(macAddress, agentIp);
                           
        } catch (Exception e) {
            log("ERROR requesting session data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear agent details display
     */
    private void clearAgentDetails() {
        agentDetailsArea.setText(Messages.get("window.select.agent"));
        cpuChartPanel.clear();
        ramChartPanel.clear();
        processesList.clear();
        ramChartInitialized = false; // Reset flag for next agent selection
    }
    
    /**
     * Start auto-update timer for selected agent
     */
    private void startAutoUpdate() {
        stopAutoUpdate(); // Stop existing timer if any
        
        // Create timer that updates every 10 seconds
        updateTimer = new Timer(10000, e -> {
            if (selectedAgentMac != null) {
                updateSessionData(selectedAgentMac);
            }
        });
        updateTimer.start();
        log("Started auto-update for Agent: " + selectedAgentMac);
    }
    
    /**
     * Stop auto-update timer
     */
    private void stopAutoUpdate() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }
    
    /**
     * Kill selected process(es) on the selected agent
     */
    private void killSelectedProcesses() {
        if (selectedAgentMac == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select an agent first", 
                "No Agent Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get selected process PIDs
        long[] selectedPids = processesList.getSelectedProcessPids();
        if (selectedPids.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select one or more processes to kill", 
                "No Process Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Confirm action
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to kill " + selectedPids.length + " process(es)?",
            "Confirm Kill Process",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Get agent IP
        Computer computer = computerManager.getComputerByMac(selectedAgentMac);
        if (computer == null) {
            log("ERROR: Cannot find agent with MAC: " + selectedAgentMac);
            return;
        }
        
        String agentIp = computer.getIpAddress();
        
        // Kill each process
        int successCount = 0;
        for (long pid : selectedPids) {
            try {
                remoteCommandClient.killProcess(agentIp, (int) pid);
                successCount++;
                log("Killed process " + pid + " on " + agentIp);
            } catch (Exception e) {
                log("Failed to kill process " + pid + " on " + agentIp + ": " + e.getMessage());
                // Show error for first failure if multiple
                if (successCount == 0 && selectedPids.length == 1) {
                     JOptionPane.showMessageDialog(this,
                        "Failed to kill process: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        // Show result
        JOptionPane.showMessageDialog(this,
            "Successfully killed " + successCount + " of " + selectedPids.length + " process(es)",
            "Kill Process Result",
            JOptionPane.INFORMATION_MESSAGE);
            
        // Refresh session data after a short delay
        Timer refreshTimer = new Timer(2000, e -> {
            if (selectedAgentMac != null) {
                updateSessionData(selectedAgentMac);
            }
        });
        refreshTimer.setRepeats(false);
        refreshTimer.start();
    }
    
    /**
     * Send message to the selected agent
     */
    private void sendMessageToAgent() {
        if (selectedAgentMac == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select an agent first", 
                "No Agent Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get agent IP
        Computer computer = computerManager.getComputerByMac(selectedAgentMac);
        if (computer == null) {
            log("ERROR: Cannot find agent with MAC: " + selectedAgentMac);
            return;
        }
        
        String agentIp = computer.getIpAddress();
        String hostname = computer.getHostname();
        
        // Prompt for message
        String message = JOptionPane.showInputDialog(this,
            "Enter message to send to " + hostname + ":",
            "Send Message",
            JOptionPane.PLAIN_MESSAGE);
            
        if (message == null || message.trim().isEmpty()) {
            return; // User cancelled or entered empty message
        }
        
        // Send message
        try {
            remoteCommandClient.sendMessage(agentIp, message);
            
            log("Message sent to " + hostname + " (" + agentIp + ")");
            JOptionPane.showMessageDialog(this,
                "Message sent successfully",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            log("Failed to send message to " + hostname + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Failed to send message: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Shutdown the selected agent
     */
    private void shutdownAgent() {
        if (selectedAgentMac == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select an agent first", 
                "No Agent Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get agent info
        Computer computer = computerManager.getComputerByMac(selectedAgentMac);
        if (computer == null) {
            log("ERROR: Cannot find agent with MAC: " + selectedAgentMac);
            return;
        }
        
        String agentIp = computer.getIpAddress();
        String hostname = computer.getHostname();
        
        // Confirm action (double confirmation for safety)
        int confirm1 = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to SHUTDOWN " + hostname + "?\\n" +
            "This will turn off the agent computer!",
            "Confirm Shutdown",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm1 != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Second confirmation
        int confirm2 = JOptionPane.showConfirmDialog(this,
            "FINAL CONFIRMATION: Shutdown " + hostname + " (" + agentIp + ")?",
            "Final Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE);
            
        if (confirm2 != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Prompt for delay
        String delayStr = JOptionPane.showInputDialog(this,
            "Shutdown delay in seconds (default 60):",
            "60");
            
        int delay = 60;
        try {
            if (delayStr != null && !delayStr.trim().isEmpty()) {
                delay = Integer.parseInt(delayStr.trim());
            }
        } catch (NumberFormatException e) {
            delay = 60;
        }
        
        // Send shutdown command
        try {
            remoteCommandClient.shutdown(agentIp, delay);
            
            log("Shutdown scheduled for " + hostname + " in " + delay + " seconds");
            JOptionPane.showMessageDialog(this,
                "Shutdown scheduled successfully in " + delay + " seconds",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            log("Failed to schedule shutdown for " + hostname + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Failed to schedule shutdown: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    /**
     * Delete the selected agent
     */
    private void deleteAgent() {
        if (selectedAgentMac == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select an agent first", 
                "No Agent Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get agent info
        model.Computer computer = computerManager.getComputerByMac(selectedAgentMac);
        String hostname = (computer != null) ? computer.getHostname() : "Unknown";
        String ipAddress = (computer != null) ? computer.getIpAddress() : null;
        
        // Confirm action
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to DELETE agent " + hostname + " (" + selectedAgentMac + ")?\n" +
            "This will remove the agent from the database.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Delete agent
        boolean success = computerManager.deleteComputer(selectedAgentMac);
        
        if (success) {
            log("Agent deleted: " + selectedAgentMac);
            
            // Stop monitoring and remove from scan cache
            sessionRetriever.stopMonitoring(selectedAgentMac);
            if (ipAddress != null) {
                hostScanner.removeHost(ipAddress);
            }
            
            JOptionPane.showMessageDialog(this,
                "Agent deleted successfully",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
            // Clear selection and refresh list
            selectedAgentMac = null;
            clearAgentDetails();
            stopAutoUpdate();
            refreshAgentList();
        } else {
            log("Failed to delete agent: " + selectedAgentMac);
            JOptionPane.showMessageDialog(this,
                "Failed to delete agent",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
