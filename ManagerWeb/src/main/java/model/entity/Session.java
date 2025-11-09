package model.entity;

import java.io.Serializable;
import java.util.List;

public class Session implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;               // ID log (primary key)
    private String macAddress;     // mapping to Computer MAC address  
    private double cpuUsage;  // percentage
    private long totalRam;
    private long ramUsage;  // number of bytes
    private List<Process> processes;
    private long timestamp;

    public Session() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public long getTotalRam() { return totalRam; }
    public void setTotalRam(long totalRam) { this.totalRam = totalRam; }

    public long getRamUsage() { return ramUsage; }
    public void setRamUsage(long ramUsage) { this.ramUsage = ramUsage; }

    public List<Process> getProcesses() { return processes; }
    public void setProcesses(List<Process> processes) { this.processes = processes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
