package com.omnia.raspberry.service;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class GpioService {

    private Context pi4j;
    private DigitalOutput led;
    private boolean ledState = false;
    
    // Pin GPIO per il LED (BCM 18, Physical Pin 12)
    private static final int LED_PIN = 18;

    @PostConstruct
    public void initialize() {
        try {
            // Inizializza Pi4J context
            pi4j = Pi4J.newAutoContext();
            
            // Configura il pin del LED come output
            led = pi4j.dout().create(LED_PIN);
            
            // Inizializza LED spento
            led.low();
            ledState = false;
            
            System.out.println("GPIO Service inizializzato. LED pin: " + LED_PIN);
        } catch (Exception e) {
            System.err.println("Errore nell'inizializzazione GPIO: " + e.getMessage());
            // Su sistemi non-Raspberry Pi, continua senza GPIO reale
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (led != null) {
                led.low();
            }
            if (pi4j != null) {
                pi4j.shutdown();
            }
            System.out.println("GPIO Service terminato correttamente");
        } catch (Exception e) {
            System.err.println("Errore nella chiusura GPIO: " + e.getMessage());
        }
    }

    public boolean toggleLed() {
        try {
            if (led != null) {
                ledState = !ledState;
                if (ledState) {
                    led.high();
                } else {
                    led.low();
                }
            } else {
                // Modalità simulazione per test su PC
                ledState = !ledState;
                System.out.println("LED simulato: " + (ledState ? "ON" : "OFF"));
            }
            return ledState;
        } catch (Exception e) {
            System.err.println("Errore nel toggle LED: " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO", e);
        }
    }

    public void turnOnLed() {
        try {
            if (led != null) {
                led.high();
            } else {
                System.out.println("LED simulato: ON");
            }
            ledState = true;
        } catch (Exception e) {
            System.err.println("Errore nell'accensione LED: " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO", e);
        }
    }

    public void turnOffLed() {
        try {
            if (led != null) {
                led.low();
            } else {
                System.out.println("LED simulato: OFF");
            }
            ledState = false;
        } catch (Exception e) {
            System.err.println("Errore nello spegnimento LED: " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO", e);
        }
    }

    public boolean isLedOn() {
        return ledState;
    }
}