package com.omnia.raspberry.controller;

import com.omnia.raspberry.service.GpioService;
import com.omnia.raspberry.service.SystemMonitorService;
import com.omnia.raspberry.model.GpioPin;
import com.omnia.raspberry.model.SystemInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class RaspberryPiController {

    @Autowired
    private GpioService gpioService;

    @Autowired
    private SystemMonitorService systemMonitorService;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "🚀 OMNIA PROJECT - Sistema GPIO Professionale");
        response.put("version", "2.0.0");
        response.put("service", "Spring Boot + GPIO Professional");
        response.put("totalPins", gpioService.getAllGpioPins().size());
        response.put("ledState", gpioService.isLedOn()); // Compatibilità
        return ResponseEntity.ok(response);
    }

    // =================== NUOVE API GPIO ===================

    @GetMapping("/api/gpio/pins")
    public ResponseEntity<List<GpioPin>> getAllGpioPins() {
        try {
            List<GpioPin> pins = gpioService.getAllGpioPins();
            return ResponseEntity.ok(pins);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/gpio/pin/{pinNumber}")
    public ResponseEntity<GpioPin> getGpioPin(@PathVariable int pinNumber) {
        try {
            GpioPin pin = gpioService.getGpioPin(pinNumber);
            if (pin != null) {
                return ResponseEntity.ok(pin);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/gpio/pin/{pinNumber}/toggle")
    public ResponseEntity<Map<String, Object>> toggleGpioPin(@PathVariable int pinNumber) {
        try {
            boolean newState = gpioService.toggleDigitalPin(pinNumber);
            GpioPin pin = gpioService.getGpioPin(pinNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("pinNumber", pinNumber);
            response.put("name", pin != null ? pin.getName() : "Pin " + pinNumber);
            response.put("isOn", newState);
            response.put("message", "Pin " + pinNumber + " " + (newState ? "attivato" : "disattivato"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore controllo pin " + pinNumber + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // =================== CONTROLLO VENTOLA ===================

    @PostMapping("/api/fan/speed")
    public ResponseEntity<Map<String, Object>> setFanSpeed(@RequestParam int percentage) {
        try {
            gpioService.setFanSpeed(percentage);
            Map<String, Object> response = new HashMap<>();
            response.put("fanSpeed", percentage);
            response.put("message", "Ventola impostata al " + percentage + "%");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore controllo ventola: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // =================== LED STRIP WS2812B ===================

    @PostMapping("/api/ledstrip/color")
    public ResponseEntity<Map<String, Object>> setLedStripColor(
            @RequestParam int ledIndex,
            @RequestParam int red,
            @RequestParam int green,
            @RequestParam int blue) {
        try {
            gpioService.setLedStripColor(ledIndex, red, green, blue);
            Map<String, Object> response = new HashMap<>();
            response.put("ledIndex", ledIndex);
            response.put("color", Map.of("red", red, "green", green, "blue", blue));
            response.put("message", "LED " + ledIndex + " impostato a RGB(" + red + "," + green + "," + blue + ")");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore controllo LED strip: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/api/ledstrip/power")
    public ResponseEntity<Map<String, Object>> setLedStripPower(@RequestParam boolean isOn) {
        try {
            gpioService.setLedStripPower(isOn);
            Map<String, Object> response = new HashMap<>();
            response.put("isOn", isOn);
            response.put("message", "LED Strip " + (isOn ? "accesa" : "spenta"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore controllo alimentazione LED strip: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/api/ledstrip/brightness")
    public ResponseEntity<Map<String, Object>> setLedStripBrightness(@RequestParam int brightness) {
        try {
            gpioService.setLedStripBrightness(brightness);
            Map<String, Object> response = new HashMap<>();
            response.put("brightness", brightness);
            response.put("message", "Luminosità LED Strip impostata a " + brightness);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore controllo luminosità LED strip: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/api/ledstrip/effect")
    public ResponseEntity<Map<String, Object>> setLedStripEffect(
            @RequestParam String effect,
            @RequestParam(defaultValue = "50") int speed,
            @RequestParam(defaultValue = "255") int red,
            @RequestParam(defaultValue = "255") int green,
            @RequestParam(defaultValue = "255") int blue) {
        try {
            gpioService.setLedStripEffect(effect, speed, red, green, blue);
            Map<String, Object> response = new HashMap<>();
            response.put("effect", effect);
            response.put("speed", speed);
            response.put("color", Map.of("red", red, "green", green, "blue", blue));
            response.put("message", "Effetto LED Strip '" + effect + "' attivato");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore effetto LED strip: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // =================== MONITORAGGIO SISTEMA ===================

    @GetMapping("/api/system/info")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        try {
            SystemInfo systemInfo = systemMonitorService.getSystemInfo();
            return ResponseEntity.ok(systemInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/system/temperature")
    public ResponseEntity<Map<String, Object>> getSystemTemperature() {
        try {
            SystemInfo systemInfo = systemMonitorService.getSystemInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("temperature", systemInfo.getCpuTemperature());
            response.put("unit", "°C");
            response.put("status", systemInfo.getCpuTemperature() > 70 ? "HOT" : "OK");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Errore lettura temperatura: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // =================== API COMPATIBILITÀ (per frontend esistente) ===================

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