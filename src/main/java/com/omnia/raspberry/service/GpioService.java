package com.omnia.raspberry.service;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Service
public class GpioService {

    private boolean ledState = false;
    private static final int LED_PIN = 18;

    @PostConstruct
    public void initialize() {
        try {
            // Inizializza LED spento
            executeCommand("gpioset gpiochip0 " + LED_PIN + "=0");
            ledState = false;
            System.out.println("GPIO Service inizializzato con gpioset. LED pin: " + LED_PIN);
        } catch (Exception e) {
            System.err.println("Errore nell'inizializzazione GPIO: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            executeCommand("gpioset gpiochip0 " + LED_PIN + "=0");
            System.out.println("GPIO Service terminato correttamente");
        } catch (Exception e) {
            System.err.println("Errore nella chiusura GPIO: " + e.getMessage());
        }
    }

    private void executeCommand(String command) throws IOException, InterruptedException {
        System.out.println("Executing GPIO command: " + command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        System.out.println("Command exit code: " + exitCode);
        if (exitCode != 0) {
            // Leggi stderr per debug
            String error = new String(process.getErrorStream().readAllBytes());
            System.err.println("Command error output: " + error);
            throw new IOException("Command failed with exit code: " + exitCode + ", error: " + error);
        }
        System.out.println("GPIO command executed successfully");
    }

    public boolean toggleLed() {
        try {
            ledState = !ledState;
            executeCommand("gpioset gpiochip0 " + LED_PIN + "=" + (ledState ? "1" : "0"));
            System.out.println("LED: " + (ledState ? "ON" : "OFF"));
            return ledState;
        } catch (Exception e) {
            System.err.println("Errore nel toggle LED: " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO", e);
        }
    }

    public void turnOnLed() {
        try {
            executeCommand("gpioset gpiochip0 " + LED_PIN + "=1");
            ledState = true;
            System.out.println("LED: ON");
        } catch (Exception e) {
            System.err.println("Errore nell'accensione LED: " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO", e);
        }
    }

    public void turnOffLed() {
        try {
            executeCommand("gpioset gpiochip0 " + LED_PIN + "=0");
            ledState = false;
            System.out.println("LED: OFF");
        } catch (Exception e) {
            System.err.println("Errore nello spegnimento LED: " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO", e);
        }
    }

    public boolean isLedOn() {
        return ledState;
    }
}