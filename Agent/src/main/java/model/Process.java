package model;

import java.io.Serializable;

public class Process implements Serializable {
    private static final long serialVersionUID = 1L;

    private long pid;
    private String macAddress;
    private long timestamp;  // FOREIGN KEY to Session timestamp.
    private String name;
    private double cpuUsage;
    private long ramUsage;


    public Process() {}

    public Process(long pid, String macAddress, long timestamp, String name, double cpuUsage, long ramUsage) {
        this.pid = pid;
        this.macAddress = macAddress;
        this.timestamp = timestamp;
        this.name = name;
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
    }

    // Getters & Setters

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    public long getTimestamp()  { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public long getRamUsage() { return ramUsage; }
    public void setRamUsage(long ramUsage) { this.ramUsage = ramUsage; }
}
