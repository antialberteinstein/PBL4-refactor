package ui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Comparator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import model.Session;
import model.Process;
import model.Computer;
import service.SessionRetriever;
import service.ComputerRetriever;
import config.AppConfig;

public class AgentWindow extends JFrame{

    public static interface AgentStartListener {
        void onAgentStarted();
    }

    public static interface AgentStopListener {
        void onAgentStopped();
    }

    private AgentStartListener startListener;
    private AgentStopListener stopListener;
    private final SessionRetriever sessionRetriever;
    private final ComputerRetriever computerRetriever;
    private final AppConfig appConfig;

    private boolean agentStatus = false; // false: stopped, true: running

    // Charts state
    private DefaultPieDataset<String> cpuDataset;
    private DefaultPieDataset<String> ramDataset;
    private Timer uiRefreshTimer;
    private ExecutorService dataExecutor;

    // UI for processes
    private JTable processTable;
    private DefaultTableModel processTableModel;

    // UI for computer info
    private JPanel computerInfoPanel;

    public AgentWindow(AgentStartListener startListener, AgentStopListener stopListener, SessionRetriever sessionRetriever, ComputerRetriever computerRetriever, AppConfig appConfig) {
        this.startListener = startListener;
        this.stopListener = stopListener;
        this.sessionRetriever = sessionRetriever;
        this.computerRetriever = computerRetriever;
        this.appConfig = appConfig;
        setTitle("Agent Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        JPanel controlPanel = new JPanel(new FlowLayout());

        // Add start button
        // When clicked, notify listener
        JButton startButton = new JButton("Start Agent");
        startButton.addActionListener(e -> {
            if (startListener != null && agentStatus == false) {
                startListener.onAgentStarted();
                agentStatus = true;
                startButton.setText("Stop Agent");
                startUiRefresh();
            } else if (stopListener != null && agentStatus == true) {
                stopListener.onAgentStopped();
                agentStatus = false;
                startButton.setText("Start Agent");
                stopUiRefresh();
            }
        });

        controlPanel.add(startButton);

        // Main content uses tabs: Usage (charts), Processes, Computer
        JTabbedPane tabs = new JTabbedPane();

        // Usage tab
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        initCharts(chartsPanel);
        tabs.addTab("Usage", chartsPanel);

        // Processes tab
        JPanel processesPanel = initProcessesPanel();
        tabs.addTab("Processes", processesPanel);

        // Computer info tab
        computerInfoPanel = initComputerInfoPanel();
        tabs.addTab("Computer", computerInfoPanel);

        this.add(controlPanel, BorderLayout.NORTH);
        this.add(tabs, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (stopListener != null && agentStatus == true) {
                    stopListener.onAgentStopped();
                }
                stopUiRefresh();
                super.windowClosing(e);
            }
        });

        setVisible(true);

        // Preload computer info into panel
        updateComputerInfo();
    }

    private void initCharts(JPanel chartsPanel) {
        // CPU dataset and chart
    cpuDataset = new DefaultPieDataset<>();
        cpuDataset.setValue("Used", 0);
        cpuDataset.setValue("Idle", 100);
        JFreeChart cpuChart = createPieChart("CPU Usage", cpuDataset, true);
        ChartPanel cpuChartPanel = new ChartPanel(cpuChart);

        // RAM dataset and chart
    ramDataset = new DefaultPieDataset<>();
        ramDataset.setValue("Used", 0);
        ramDataset.setValue("Free", 1); // placeholder, will be updated
        JFreeChart ramChart = createPieChart("RAM Usage", ramDataset, true);
        ChartPanel ramChartPanel = new ChartPanel(ramChart);

        chartsPanel.add(cpuChartPanel);
        chartsPanel.add(ramChartPanel);
    }

    private JFreeChart createPieChart(String title, PieDataset<String> dataset, boolean showLabels) {
    JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
    @SuppressWarnings("unchecked")
    PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setNoDataMessage("No data available");
        plot.setCircular(true);
        plot.setLabelGap(0.02);
        if (showLabels) {
            PieSectionLabelGenerator gen = new org.jfree.chart.labels.StandardPieSectionLabelGenerator(
                "{0}: {2} ({1})",
                NumberFormat.getNumberInstance(),
                new DecimalFormat("0.0%")
            );
            plot.setLabelGenerator(gen);
        } else {
            plot.setLabelGenerator(null);
        }
        return chart;
    }

    // UI refresh directly calls retrieve (off-EDT) then updates charts
    private void startUiRefresh() {
        if (uiRefreshTimer != null && uiRefreshTimer.isRunning()) return;
        if (dataExecutor == null) dataExecutor = Executors.newSingleThreadExecutor();
        uiRefreshTimer = new Timer(appConfig != null ? appConfig.CHART_REFRESH_MILLIS : 1000, e -> {
            dataExecutor.submit(() -> {
                Session session = sessionRetriever.retrieveAndSaveSession();
                if (session != null) {
                    SwingUtilities.invokeLater(() -> {
                        updateCharts(session);
                        updateProcessTable(session);
                    });
                }
            });
        });
        uiRefreshTimer.setRepeats(true);
        uiRefreshTimer.start();
    }

    private void stopUiRefresh() {
        if (uiRefreshTimer != null) {
            uiRefreshTimer.stop();
            uiRefreshTimer = null;
        }
        if (dataExecutor != null) {
            dataExecutor.shutdownNow();
            dataExecutor = null;
        }
    }

    private void updateCharts(Session session) {
        // CPU
        double cpuUsed = Math.max(0, Math.min(100.0, session.getCpuUsage()));
        double cpuIdle = 100.0 - cpuUsed;
        cpuDataset.setValue("Used", cpuUsed);
        cpuDataset.setValue("Idle", cpuIdle);

        // RAM
        long total = Math.max(1, session.getTotalRam());
        long used = Math.max(0, Math.min(total, session.getRamUsage()));
        long free = Math.max(0, total - used);
        // Using absolute values lets JFreeChart compute percents automatically
        ramDataset.setValue("Used", used);
        ramDataset.setValue("Free", free);
    }

    private JPanel initProcessesPanel() {
        String[] columns = { "PID", "Name", "CPU %", "RAM (MB)" };
        processTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        processTable = new JTable(processTableModel);
        processTable.setAutoCreateRowSorter(true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(processTable), BorderLayout.CENTER);
        return panel;
    }

    private void updateProcessTable(Session session) {
        List<Process> procs = session.getProcesses();
        if (procs == null) {
            processTableModel.setRowCount(0);
            return;
        }

        // Show top 20 by RAM usage
        procs = procs.stream()
            .sorted(Comparator.comparingLong(Process::getRamUsage).reversed())
            .limit(20)
            .toList();

        processTableModel.setRowCount(0);
        for (Process p : procs) {
            double cpu = Math.max(0.0, p.getCpuUsage());
            double ramMb = p.getRamUsage() / (1024.0 * 1024.0);
            processTableModel.addRow(new Object[] { p.getPid(), p.getName(), String.format("%.1f", cpu), String.format("%.1f", ramMb) });
        }
    }

    private JPanel initComputerInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // placeholders; will be filled by updateComputerInfo()
        panel.add(new JLabel("Hostname:")); panel.add(new JLabel(""));
        panel.add(new JLabel("IP Address:")); panel.add(new JLabel(""));
        panel.add(new JLabel("MAC Address:")); panel.add(new JLabel(""));
        panel.add(new JLabel("OS:")); panel.add(new JLabel(""));
        panel.add(new JLabel("Architecture:")); panel.add(new JLabel(""));
        panel.add(new JLabel("Manufacturer:")); panel.add(new JLabel(""));
        panel.add(new JLabel("Model:")); panel.add(new JLabel(""));
        panel.add(new JLabel("Serial Number:")); panel.add(new JLabel(""));
        panel.add(new JLabel("CPU Name:")); panel.add(new JLabel(""));
        panel.add(new JLabel("CPU Vendor:")); panel.add(new JLabel(""));
        panel.add(new JLabel("Physical Cores:")); panel.add(new JLabel(""));
        panel.add(new JLabel("Logical Cores:")); panel.add(new JLabel(""));
        panel.add(new JLabel("CPU Max Freq (GHz):")); panel.add(new JLabel(""));
        return panel;
    }

    private void updateComputerInfo() {
        // Ensure we have computer data
        Computer c = computerRetriever.getCurrentComputer();
        if (c == null) return;

        // Update labels pairwise
        // Assumes labels are added as alternating name/value pairs
        int idx = 1; // value label index (skip first name at 0)
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getHostname())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getIpAddress())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getMacAddress())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getOs())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getArchitecture())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getManufacturer())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getModel())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getSerialNumber())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getCpuName())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(nullToEmpty(c.getCpuVendor())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(String.valueOf(c.getPhysicalCores())); idx += 2;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(String.valueOf(c.getLogicalCores())); idx += 2;
        double ghz = c.getCpuMaxFreq() > 0 ? c.getCpuMaxFreq() / 1_000_000_000.0 : 0.0;
        ((JLabel) computerInfoPanel.getComponent(idx)).setText(String.format("%.2f", ghz));
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }



}
