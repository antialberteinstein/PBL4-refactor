package model;

import java.io.Serializable;

public class Computer implements Serializable {
    private static final long serialVersionUID = 1L;

    // Thông tin cơ bản
    private String hostname;
    private String ipAddress;
    private String macAddress;  // primary key.
    private String os;
    private String architecture;

    // Hardware info
    private String manufacturer;  // Hãng sản xuất.
    private String model;  // Dòng máy.
    private String serialNumber;  // Số series.

    // CPU
    private String cpuName;
    private String cpuVendor;
    private int physicalCores;
    private int logicalCores;
    private long cpuMaxFreq;

    // ===== Getters & Setters =====
    // (Chỉ viết vài cái mẫu, bạn có thể gen bằng IDE)
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCpuName() { return cpuName; }
    public void setCpuName(String cpuName) { this.cpuName = cpuName; }

    public String getCpuVendor() { return cpuVendor; }
    public void setCpuVendor(String cpuVendor) { this.cpuVendor = cpuVendor; }

    public int getPhysicalCores() { return physicalCores; }
    public void setPhysicalCores(int physicalCores) { this.physicalCores = physicalCores; }

    public int getLogicalCores() { return logicalCores; }
    public void setLogicalCores(int logicalCores) { this.logicalCores = logicalCores; }

    public long getCpuMaxFreq() { return cpuMaxFreq; }
    public void setCpuMaxFreq(long cpuMaxFreq) { this.cpuMaxFreq = cpuMaxFreq; }
}
