package com.omnia.raspberry.service;

import com.omnia.raspberry.model.SystemInfo;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class SystemMonitorService {

    public SystemInfo getSystemInfo() {
        SystemInfo info = new SystemInfo();
        
        try {
            // Temperatura CPU
            info.setCpuTemperature(getCpuTemperature());
            
            // Utilizzo CPU
            info.setCpuUsage(getCpuUsage());
            
            // Utilizzo memoria
            info.setMemoryUsage(getMemoryUsage());
            
            // Utilizzo disco
            info.setDiskUsage(getDiskUsage());
            
            // Uptime
            info.setUptime(getUptime());
            
            // Versione kernel
            info.setKernelVersion(getKernelVersion());
            
            // Stato ventola (placeholder)
            info.setFanStatus(true);
            info.setFanSpeed(50); // 50% default
            
        } catch (Exception e) {
            System.err.println("❌ Errore nel recupero informazioni sistema: " + e.getMessage());
        }
        
        return info;
    }

    private double getCpuTemperature() {
        try {
            String result = executeCommand("cat /sys/class/thermal/thermal_zone0/temp");
            if (result != null && !result.trim().isEmpty()) {
                // Temperatura in milligradi, converti in gradi Celsius
                double temp = Double.parseDouble(result.trim()) / 1000.0;
                return Math.round(temp * 10.0) / 10.0; // Arrotonda a 1 decimale
            }
        } catch (Exception e) {
            System.err.println("Errore lettura temperatura: " + e.getMessage());
        }
        return 0.0;
    }

    private double getCpuUsage() {
        try {
            // Comando per ottenere utilizzo CPU
            String result = executeCommand("top -bn1 | grep load | awk '{printf \"%.2f\", $(NF-2)}'");
            if (result != null && !result.trim().isEmpty()) {
                return Double.parseDouble(result.trim());
            }
        } catch (Exception e) {
            System.err.println("Errore lettura CPU usage: " + e.getMessage());
        }
        return 0.0;
    }

    private double getMemoryUsage() {
        try {
            String result = executeCommand("free | grep Mem | awk '{printf(\"%.1f\", $3/$2 * 100.0)}'");
            if (result != null && !result.trim().isEmpty()) {
                return Double.parseDouble(result.trim());
            }
        } catch (Exception e) {
            System.err.println("Errore lettura memoria: " + e.getMessage());
        }
        return 0.0;
    }

    private double getDiskUsage() {
        try {
            String result = executeCommand("df -h / | awk 'FNR==2{print $5}' | sed 's/%//g'");
            if (result != null && !result.trim().isEmpty()) {
                return Double.parseDouble(result.trim());
            }
        } catch (Exception e) {
            System.err.println("Errore lettura disco: " + e.getMessage());
        }
        return 0.0;
    }

    private long getUptime() {
        try {
            String result = executeCommand("cat /proc/uptime | awk '{print $1}'");
            if (result != null && !result.trim().isEmpty()) {
                return (long) Double.parseDouble(result.trim());
            }
        } catch (Exception e) {
            System.err.println("Errore lettura uptime: " + e.getMessage());
        }
        return 0L;
    }

    private String getKernelVersion() {
        try {
            String result = executeCommand("uname -r");
            return result != null ? result.trim() : "Unknown";
        } catch (Exception e) {
            System.err.println("Errore lettura kernel version: " + e.getMessage());
            return "Unknown";
        }
    }

    private String executeCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        process.waitFor();
        
        byte[] output = process.getInputStream().readAllBytes();
        return new String(output);
    }

    // Formatta uptime in formato leggibile
    public String formatUptime(long uptimeSeconds) {
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}