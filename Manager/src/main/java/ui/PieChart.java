package ui;

import javax.swing.*;
import java.awt.*;

/**
 * PieChart - Custom component for displaying percentage data as a pie chart
 * Best for showing current usage vs available resources
 */
public class PieChart extends JPanel {
    
    private double value;
    private double maxValue;
    private String title;
    private String unit;
    private Color usedColor;
    private Color freeColor;
    
    /**
     * Constructor
     * @param title Chart title
     * @param unit Unit of measurement (e.g., "%", "GB")
     * @param usedColor Color for used portion
     * @param maxValue Maximum value (100% equivalent)
     */
    public PieChart(String title, String unit, Color usedColor, double maxValue) {
        this.title = title;
        this.unit = unit;
        this.usedColor = usedColor;
        this.freeColor = new Color(220, 220, 220);
        this.maxValue = maxValue;
        this.value = 0;
        
        setPreferredSize(new Dimension(400, 200));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    }
    
    /**
     * Set the current value
     */
    public void setValue(double value) {
        this.value = value;
        repaint();
    }
    
    /**
     * Clear the chart
     */
    public void clear() {
        this.value = 0;
        repaint();
    }
    
    /**
     * Get the latest value
     */
    public double getLatestValue() {
        return value;
    }
    
    /**
     * Update max value (useful for RAM when switching agents)
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw title
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics titleMetrics = g2.getFontMetrics();
        int titleWidth = titleMetrics.stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 20);
        
        // Calculate pie dimensions
        int diameter = Math.min(width, height - 80);
        int x = (width - diameter) / 2;
        int y = 40;
        
        // Calculate percentage
        double percentage = (maxValue > 0) ? (value / maxValue) * 100 : 0;
        int angle = (int) Math.round((percentage / 100.0) * 360);
        
        // Draw free portion (background)
        g2.setColor(freeColor);
        g2.fillArc(x, y, diameter, diameter, 0, 360);
        
        // Draw used portion
        g2.setColor(usedColor);
        g2.fillArc(x, y, diameter, diameter, 90, -angle);
        
        // Draw border
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(2f));
        g2.drawArc(x, y, diameter, diameter, 0, 360);
        
        // Draw percentage text in center
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        String percentText = String.format("%.1f%%", percentage);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(percentText);
        g2.drawString(percentText, (width - textWidth) / 2, y + diameter / 2 + 8);
        
        // Draw value text below
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        String valueText = String.format("%.2f %s / %.2f %s", value, unit, maxValue, unit);
        int valueWidth = g2.getFontMetrics().stringWidth(valueText);
        g2.drawString(valueText, (width - valueWidth) / 2, y + diameter + 25);
    }
}
