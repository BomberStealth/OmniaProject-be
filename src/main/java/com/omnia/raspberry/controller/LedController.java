package com.omnia.raspberry.controller;

import com.omnia.raspberry.service.GpioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class LedController {

    @Autowired
    private GpioService gpioService;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "🚀 OMNIA PROJECT - Auto-deploy ATTIVO!");
        response.put("ledState", gpioService.isLedOn());
        response.put("service", "Java Spring Boot + Pi4J");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/led/toggle")
    public ResponseEntity<Map<String, Object>> toggleLed() {
        try {
            boolean newState = gpioService.toggleLed();
            Map<String, Object> response = new HashMap<>();
            response.put("isOn", newState);
            response.put("message", "LED " + (newState ? "acceso" : "spento"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore nel controllo del LED: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/api/led/status")
    public ResponseEntity<Map<String, Object>> getLedStatus() {
        try {
            boolean isOn = gpioService.isLedOn();
            Map<String, Object> response = new HashMap<>();
            response.put("isOn", isOn);
            response.put("message", "LED " + (isOn ? "acceso" : "spento"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore nel recupero stato LED: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/api/led/on")
    public ResponseEntity<Map<String, Object>> turnOnLed() {
        try {
            gpioService.turnOnLed();
            Map<String, Object> response = new HashMap<>();
            response.put("isOn", true);
            response.put("message", "LED acceso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore nell'accensione del LED: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/api/led/off")
    public ResponseEntity<Map<String, Object>> turnOffLed() {
        try {
            gpioService.turnOffLed();
            Map<String, Object> response = new HashMap<>();
            response.put("isOn", false);
            response.put("message", "LED spento");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore nello spegnimento del LED: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}