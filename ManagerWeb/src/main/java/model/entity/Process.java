package model.entity;

import java.io.Serializable;

public class Process implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;  // Primary key.
    private long pid;
    private long sessionId;
    private String name;
    private double cpuUsage;
    private long ramUsage;

    public Process() {}

    public Process(long pid, long sessionId, String name, double cpuUsage, long ramUsage) {
        this.pid = pid;
        this.sessionId = sessionId;
        this.name = name;
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    public long getSessionId()  { return sessionId; }

    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public long getRamUsage() { return ramUsage; }
    public void setRamUsage(long ramUsage) { this.ramUsage = ramUsage; }
}
