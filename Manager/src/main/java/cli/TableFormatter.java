package cli;

import java.util.List;
import java.util.ArrayList;

/**
 * TableFormatter - Creates MySQL-style ASCII tables
 * 
 * Example output:
 * +-------------------+----------------+----------+
 * | MAC Address       | IP Address     | Status   |
 * +-------------------+----------------+----------+
 * | AA:BB:CC:DD:EE:FF | 192.168.1.100  | Active   |
 * +-------------------+----------------+----------+
 */
public class TableFormatter {
    
    private final List<String> headers;
    private final List<List<String>> rows;
    private final List<Integer> columnWidths;
    
    public TableFormatter(String... headers) {
        this.headers = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.columnWidths = new ArrayList<>();
        
        // Initialize headers and column widths
        for (String header : headers) {
            this.headers.add(header);
            this.columnWidths.add(header.length());
        }
    }
    
    /**
     * Add a row to the table
     * @param values Values for each column (must match header count)
     */
    public void addRow(String... values) {
        if (values.length != headers.size()) {
            throw new IllegalArgumentException("Row must have " + headers.size() + " columns");
        }
        
        List<String> row = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "NULL" : values[i];
            row.add(value);
            
            // Update column width if this value is longer
            if (value.length() > columnWidths.get(i)) {
                columnWidths.set(i, value.length());
            }
        }
        rows.add(row);
    }
    
    /**
     * Build and return the formatted table as a string
     */
    public String build() {
        if (rows.isEmpty()) {
            return "Empty set";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Top border
        sb.append(buildBorder()).append("\n");
        
        // Header row
        sb.append(buildRow(headers)).append("\n");
        
        // Header separator
        sb.append(buildBorder()).append("\n");
        
        // Data rows
        for (List<String> row : rows) {
            sb.append(buildRow(row)).append("\n");
        }
        
        // Bottom border
        sb.append(buildBorder()).append("\n");
        
        // Row count (like MySQL)
        sb.append(rows.size()).append(rows.size() == 1 ? " row" : " rows").append(" in set\n");
        
        return sb.toString();
    }
    
    /**
     * Build a border line: +-------+-------+-------+
     */
    private String buildBorder() {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int width : columnWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        return sb.toString();
    }
    
    /**
     * Build a data row: | Value1 | Value2 | Value3 |
     */
    private String buildRow(List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            int width = columnWidths.get(i);
            sb.append(" ").append(padRight(value, width)).append(" |");
        }
        return sb.toString();
    }
    
    /**
     * Pad string with spaces on the right to reach desired width
     */
    private String padRight(String str, int width) {
        if (str.length() >= width) {
            return str;
        }
        return str + " ".repeat(width - str.length());
    }
    
    /**
     * Static helper to quickly create and build a table
     */
    public static String format(String[] headers, List<String[]> rows) {
        TableFormatter table = new TableFormatter(headers);
        for (String[] row : rows) {
            table.addRow(row);
        }
        return table.build();
    }
}
