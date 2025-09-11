package com.omnia.raspberry.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Service per comunicazione con ESP-01S WS2812B Controller
 * 
 * Questo servizio sostituisce il controllo GPIO diretto per WS2812B
 * inviando comandi HTTP all'ESP-01S che gestisce il timing preciso
 * 
 * Author: Claude Code + Omnia Project  
 * Date: 2025-09-11
 */
@Service
public class ESP01Service {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // ESP-01S Configuration
    private static final String ESP01_BASE_URL = "http://192.168.1.101";
    private static final int ESP01_PORT = 80;
    private static final String LED_ENDPOINT = "/led";
    private static final String RELAY_ENDPOINT = "/relay";
    private static final int CONNECTION_TIMEOUT = 2000; // 2 secondi
    
    // Status tracking
    private boolean esp01Available = false;
    private long lastSuccessfulConnection = 0;
    
    public ESP01Service() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        System.out.println("🌐 ESP-01S Service inizializzato - Target: " + ESP01_BASE_URL);
    }
    
    /**
     * Test connessione con ESP-01S
     */
    public boolean testConnection() {
        try {
            String url = ESP01_BASE_URL + "/";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                esp01Available = true;
                lastSuccessfulConnection = System.currentTimeMillis();
                System.out.println("✅ ESP-01S connesso: " + response.getBody());
                return true;
            }
        } catch (RestClientException e) {
            esp01Available = false;
            System.out.println("❌ ESP-01S non disponibile: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Controllo singolo LED WS2812B
     */
    public boolean setLedColor(int ledIndex, int red, int green, int blue) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("led", ledIndex);
            payload.put("r", red);
            payload.put("g", green);
            payload.put("b", blue);
            payload.put("power", true); // Accendi strip se non è già accesa
            
            return sendLedCommand(payload);
        } catch (Exception e) {
            System.err.println("❌ Errore controllo LED " + ledIndex + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Accensione/Spegnimento completo LED Strip
     */
    public boolean setLedStripPower(boolean isOn) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("power", isOn);
            
            boolean success = sendLedCommand(payload);
            if (success) {
                System.out.println("🌈 LED Strip: " + (isOn ? "ACCESA" : "SPENTA"));
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ Errore controllo power LED Strip: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Controllo luminosità LED Strip
     */
    public boolean setLedStripBrightness(int brightness) {
        try {
            // Clamp brightness 0-255
            brightness = Math.max(0, Math.min(255, brightness));
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("brightness", brightness);
            
            boolean success = sendLedCommand(payload);
            if (success) {
                System.out.println("🔆 Brightness LED Strip: " + brightness);
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ Errore controllo brightness: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Controllo effetti LED Strip
     */
    public boolean setLedStripEffect(String effect, int speed) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("effect", effect);
            payload.put("speed", speed);
            payload.put("power", true); // Accendi per mostrare effetto
            
            boolean success = sendLedCommand(payload);
            if (success) {
                System.out.println("✨ Effetto LED Strip: " + effect + " (speed: " + speed + ")");
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ Errore controllo effetto: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Controllo relè ESP-01S
     */
    public boolean setRelayState(boolean isOn) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("state", isOn);
            
            String url = ESP01_BASE_URL + RELAY_ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                esp01Available = true;
                lastSuccessfulConnection = System.currentTimeMillis();
                System.out.println("🔌 Relè ESP-01S: " + (isOn ? "ON" : "OFF"));
                return true;
            }
        } catch (RestClientException e) {
            System.err.println("❌ Errore controllo relè ESP-01S: " + e.getMessage());
            esp01Available = false;
        }
        return false;
    }
    
    /**
     * Invio comando LED generico
     */
    private boolean sendLedCommand(Map<String, Object> payload) {
        try {
            String url = ESP01_BASE_URL + LED_ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                esp01Available = true;
                lastSuccessfulConnection = System.currentTimeMillis();
                System.out.println("📡 Comando LED inviato: " + jsonPayload);
                return true;
            }
        } catch (RestClientException e) {
            System.err.println("❌ Errore invio comando LED: " + e.getMessage());
            esp01Available = false;
        }
        return false;
    }
    
    /**
     * Status ESP-01S per monitoring
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("available", esp01Available);
        status.put("url", ESP01_BASE_URL);
        status.put("lastConnection", lastSuccessfulConnection);
        
        // Test connessione live se non disponibile
        if (!esp01Available) {
            status.put("available", testConnection());
        }
        
        return status;
    }
    
    /**
     * Utility per controllo disponibilità
     */
    public boolean isAvailable() {
        return esp01Available || testConnection();
    }
}