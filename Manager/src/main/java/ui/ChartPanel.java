package ui;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

/**
 * ChartPanel - Wrapper around JFreeChart Pie Chart for consistency with Agent
 * Supports single (percentage) and ratio (used/free) modes
 */
public class ChartPanel extends JPanel {
    private final Color chartColor;
    private double maxValue; // For ratio mode (e.g., total RAM in GB) or 100 for percentage
    private final String valueFormat; // "single" or "ratio"

    private DefaultPieDataset<String> dataset;
    private JFreeChart chart;
    private double latestValue;

    /**
     * Constructor
     */
    public ChartPanel(String title, String unit, Color chartColor, double maxValue, int maxDataPoints) {
        this(title, unit, chartColor, maxValue, maxDataPoints, "single");
    }

    /**
     * Constructor with value format
     */
    public ChartPanel(String title, String unit, Color chartColor, double maxValue, int maxDataPoints, String valueFormat) {
        setLayout(new BorderLayout());
        this.chartColor = chartColor;
        this.maxValue = maxValue;
        this.valueFormat = valueFormat != null ? valueFormat : "single";

        dataset = new DefaultPieDataset<>();
        initializeDataset();

        chart = createPieChart(title, dataset, true);

        // Avoid import collision with this class name by using FQCN
        org.jfree.chart.ChartPanel chartPanel = new org.jfree.chart.ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);
    }

    private void initializeDataset() {
        if (isPercentageMode()) {
            dataset.setValue("Used", 0);
            dataset.setValue("Idle", 100);
        } else {
            dataset.setValue("Used", 0);
            dataset.setValue("Free", Math.max(0, maxValue));
        }
    }

    private boolean isPercentageMode() {
        return "single".equalsIgnoreCase(valueFormat);
    }

    private JFreeChart createPieChart(String title, PieDataset<String> dataset, boolean showLabels) {
        JFreeChart ch = ChartFactory.createPieChart(title, dataset, true, true, false);
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) ch.getPlot();
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
        // Set color for 'Used' slice
        plot.setSectionPaint("Used", chartColor);
        return ch;
    }

    /**
     * Add a data point (keeps only the latest value)
     */
    public void addDataPoint(double value) {
        this.latestValue = value;
        if (isPercentageMode()) {
            double used = Math.max(0, Math.min(100.0, value));
            dataset.setValue("Used", used);
            dataset.setValue("Idle", 100.0 - used);
        } else {
            double used = Math.max(0, Math.min(maxValue, value));
            dataset.setValue("Used", used);
            dataset.setValue("Free", Math.max(0, maxValue - used));
        }
    }

    /**
     * Add a data point with timestamp (timestamp ignored for pie chart)
     */
    public void addDataPoint(double value, long timestamp) {
        addDataPoint(value);
    }

    /**
     * Set all data points (uses only the latest value)
     */
    public void setAllDataPoints(java.util.List<?> points) {
        // Not used for pie chart
    }

    /**
     * Clear the chart
     */
    public void clear() {
        this.latestValue = 0;
        initializeDataset();
    }

    /**
     * Get the latest value
     */
    public double getLatestValue() {
        return latestValue;
    }

    /**
     * Update max value for pie chart (ratio mode)
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        if (!isPercentageMode()) {
            // Recompute free with new max
            double used = dataset.getValue("Used").doubleValue();
            dataset.setValue("Free", Math.max(0, maxValue - used));
        }
    }
}
