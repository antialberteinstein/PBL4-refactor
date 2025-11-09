package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import model.Process;
import util.Messages;

/**
 * ProcessesList - Component for displaying process information in a table
 * Shows PID, Name, CPU%, Memory%, and Thread Count
 */
public class ProcessesList extends JPanel {
    
    private JTable processTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel statusLabel;
    
    /**
     * Constructor - Initialize the process list table
     */
    public ProcessesList() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(Messages.get("window.process.list")));
        
        // Create table model with column names
        String[] columnNames = {"PID", "Name", "CPU %", "RAM (MB)"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Define column types for proper sorting
                switch (columnIndex) {
                    case 0: return Long.class;     // PID
                    case 1: return String.class;   // Name
                    case 2: return Double.class;   // CPU %
                    case 3: return Double.class;   // RAM (MB)
                    default: return String.class;
                }
            }
        };
        
        // Create table
        processTable = new JTable(tableModel);
        processTable.setFillsViewportHeight(true);
        processTable.setAutoCreateRowSorter(true);
        processTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        processTable.getColumnModel().getColumn(0).setPreferredWidth(100); // PID
        processTable.getColumnModel().getColumn(1).setPreferredWidth(300); // Name
        processTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // CPU %
        processTable.getColumnModel().getColumn(3).setPreferredWidth(100); // RAM (MB)
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(processTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Status label at bottom
        statusLabel = new JLabel(Messages.get("window.no.processes"));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);
        
        // Search/filter panel at top
        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.NORTH);
    }
    
    /**
     * Create filter panel with search functionality
     */
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel searchLabel = new JLabel("Filter:");
        JTextField searchField = new JTextField(20);
        
        // Add filter functionality
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
                updateStatusLabel();
            }
        });
        
        // Create sorter
        sorter = new TableRowSorter<>(tableModel);
        processTable.setRowSorter(sorter);
        
        panel.add(searchLabel);
        panel.add(searchField);
        
        return panel;
    }
    
    /**
     * Update the process list with new data
     * @param processes List of processes to display
     */
    public void updateProcesses(List<Process> processes) {
        // Clear existing rows
        tableModel.setRowCount(0);
        
        // Add new rows
        if (processes != null) {
            for (Process process : processes) {
                // Convert RAM from bytes to MB for display
                double ramMB = process.getRamUsage() / (1024.0 * 1024.0);
                
                Object[] row = {
                    process.getPid(),
                    process.getName(),
                    process.getCpuUsage(),
                    ramMB
                };
                tableModel.addRow(row);
            }
        }
        
        updateStatusLabel();
    }
    
    /**
     * Clear all processes from the list
     */
    public void clear() {
        tableModel.setRowCount(0);
        updateStatusLabel();
    }
    
    /**
     * Update the status label with current count
     */
    private void updateStatusLabel() {
        int visibleRows = processTable.getRowCount();
        int totalRows = tableModel.getRowCount();
        
        if (totalRows == 0) {
            statusLabel.setText("No processes loaded");
        } else if (visibleRows == totalRows) {
            statusLabel.setText("Showing " + totalRows + " process(es)");
        } else {
            statusLabel.setText("Showing " + visibleRows + " of " + totalRows + " process(es)");
        }
    }
    
    /**
     * Get the currently selected process PID
     * @return Selected PID or -1 if none selected
     */
    public long getSelectedProcessPid() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            return -1;
        }
        return (Long) processTable.getValueAt(selectedRow, 0);
    }
}
