package com.omnia.raspberry.model;

public class SystemInfo {
    private double cpuTemperature;
    private double cpuUsage;
    private double memoryUsage;
    private double diskUsage;
    private long uptime;
    private String kernelVersion;
    private boolean fanStatus;
    private int fanSpeed; // Percentuale 0-100

    // Getters and Setters
    public double getCpuTemperature() { return cpuTemperature; }
    public void setCpuTemperature(double cpuTemperature) { this.cpuTemperature = cpuTemperature; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }

    public double getDiskUsage() { return diskUsage; }
    public void setDiskUsage(double diskUsage) { this.diskUsage = diskUsage; }

    public long getUptime() { return uptime; }
    public void setUptime(long uptime) { this.uptime = uptime; }

    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }

    public boolean isFanStatus() { return fanStatus; }
    public void setFanStatus(boolean fanStatus) { this.fanStatus = fanStatus; }

    public int getFanSpeed() { return fanSpeed; }
    public void setFanSpeed(int fanSpeed) { this.fanSpeed = fanSpeed; }
}