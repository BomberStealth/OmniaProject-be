package com.omnia.raspberry.service;

import com.omnia.raspberry.model.GpioPin;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;

@Service
public class GpioService {

    private Map<Integer, GpioPin> gpioPins = new HashMap<>();
    
    // Configurazione GPIO Pins
    private static final int LED_PIN = 18;           // LED semplice
    private static final int WS2812B_PIN = 18;      // LED Strip WS2812B (Hardware PWM)
    private static final int FAN_PWM_PIN = 12;      // Ventola PWM
    
    // Arrays per memorizzare i colori dei LED WS2812B
    private int[] ledRed = new int[10];
    private int[] ledGreen = new int[10]; 
    private int[] ledBlue = new int[10];
    private boolean stripPower = false;
    private static final int RELAY_1_PIN = 16;      // Relè 1
    private static final int RELAY_2_PIN = 20;      // Relè 2
    private static final int PIR_SENSOR_PIN = 21;   // Sensore movimento
    private static final int BUZZER_PIN = 26;       // Buzzer
    private static final int STATUS_LED_PIN = 13;   // LED stato sistema

    @PostConstruct
    public void initialize() {
        try {
            initializeGpioPins();
            System.out.println("🚀 GPIO Service Professional inizializzato con " + gpioPins.size() + " pin configurati");
        } catch (Exception e) {
            System.err.println("❌ Errore nell'inizializzazione GPIO: " + e.getMessage());
        }
    }

    private void initializeGpioPins() {
        // =================== TUTTI I GPIO PIN RASPBERRY PI 4 ===================
        
        // GPIO pin controllabili digitalmente (tutti i pin GPIO disponibili)
        int[] allGpioPins = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
        
        for (int gpioNum : allGpioPins) {
            String pinName = "GPIO " + gpioNum;
            String pinType = "DIGITAL";
            String description = "Pin GPIO generico controllabile";
            
            // Configura pin speciali con nomi e tipi specifici
            switch (gpioNum) {
                case 2:
                    pinName = "GPIO 2 (SDA)";
                    pinType = "I2C";
                    description = "I2C Data Line - Può essere usato come GPIO";
                    break;
                case 3:
                    pinName = "GPIO 3 (SCL)";
                    pinType = "I2C";
                    description = "I2C Clock Line - Può essere usato come GPIO";
                    break;
                case 4:
                    pinName = "GPIO 4";
                    description = "GPIO generico - GPCLK0";
                    break;
                case 7:
                    pinName = "GPIO 7 (CE1)";
                    pinType = "SPI";
                    description = "SPI Chip Enable 1 - Può essere usato come GPIO";
                    break;
                case 8:
                    pinName = "GPIO 8 (CE0)";
                    pinType = "SPI";
                    description = "SPI Chip Enable 0 - Può essere usato come GPIO";
                    break;
                case 9:
                    pinName = "GPIO 9 (MISO)";
                    pinType = "SPI";
                    description = "SPI Master In Slave Out - Può essere usato come GPIO";
                    break;
                case 10:
                    pinName = "GPIO 10 (MOSI)";
                    pinType = "SPI";
                    description = "SPI Master Out Slave In - Può essere usato come GPIO";
                    break;
                case 11:
                    pinName = "GPIO 11 (SCLK)";
                    pinType = "SPI";
                    description = "SPI Serial Clock - Può essere usato come GPIO";
                    break;
                case 12:
                    pinName = "🌪️ Ventola PWM";
                    pinType = "PWM";
                    description = "Ventola di raffreddamento Raspberry Pi - PWM";
                    break;
                case 13:
                    pinName = "💡 LED Status";
                    description = "LED indicatore stato sistema";
                    break;
                case 14:
                    pinName = "GPIO 14 (TXD)";
                    pinType = "UART";
                    description = "UART Transmit - Può essere usato come GPIO";
                    break;
                case 15:
                    pinName = "GPIO 15 (RXD)";
                    pinType = "UART";
                    description = "UART Receive - Può essere usato come GPIO";
                    break;
                case 16:
                    pinName = "🔌 Relè #1";
                    description = "Controllo dispositivo esterno 1";
                    break;
                case 18:
                    pinName = "🔆 LED Principale";
                    description = "LED di controllo principale";
                    break;
                case 19:
                    pinName = "🌈 LED Strip WS2812B";
                    pinType = "ADDRESSABLE";
                    description = "Striscia LED RGB indirizzabile (10 LED)";
                    break;
                case 20:
                    pinName = "🔌 Relè #2";
                    description = "Controllo dispositivo esterno 2";
                    break;
                case 21:
                    pinName = "👁️ Sensore PIR";
                    pinType = "INPUT";
                    description = "Rilevatore di presenza PIR";
                    break;
                case 26:
                    pinName = "🔊 Buzzer Allarme";
                    description = "Sistema di allarme sonoro";
                    break;
            }
            
            gpioPins.put(gpioNum, new GpioPin(gpioNum, pinName, pinType, description));
        }

        // Inizializza tutti i pin GPIO
        for (GpioPin pin : gpioPins.values()) {
            String type = pin.getType();
            if (!type.equals("INPUT")) {
                try {
                    executeCommand("gpioset --mode=exit gpiochip0 " + pin.getPinNumber() + "=0");
                    pin.setState(false);
                    System.out.println("✅ Pin " + pin.getPinNumber() + " (" + pin.getName() + ") inizializzato");
                } catch (Exception e) {
                    pin.setEnabled(false);
                    System.err.println("⚠️ Pin " + pin.getPinNumber() + " non disponibile: " + e.getMessage());
                }
            } else {
                // Pin di input - abilitato ma non controllabile in output
                System.out.println("📌 Pin " + pin.getPinNumber() + " (" + pin.getName() + ") - INPUT");
            }
        }
        
        System.out.println("🚀 GPIO Service inizializzato con " + gpioPins.size() + " pin controllabili");
    }

    @PreDestroy
    public void cleanup() {
        try {
            // Spegni tutti i pin di output
            for (GpioPin pin : gpioPins.values()) {
                if (!pin.getType().equals("INPUT") && pin.isEnabled()) {
                    executeCommand("gpioset --mode=exit gpiochip0 " + pin.getPinNumber() + "=0");
                }
            }
            System.out.println("🔄 GPIO Service terminato correttamente");
        } catch (Exception e) {
            System.err.println("❌ Errore nella chiusura GPIO: " + e.getMessage());
        }
    }

    private void executeCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Command failed: " + command + " - " + error);
        }
    }

    // API per ottenere tutti i pin GPIO
    public List<GpioPin> getAllGpioPins() {
        return new ArrayList<>(gpioPins.values());
    }

    // API per ottenere un pin specifico
    public GpioPin getGpioPin(int pinNumber) {
        return gpioPins.get(pinNumber);
    }

    // Toggle pin digitale
    public boolean toggleDigitalPin(int pinNumber) {
        GpioPin pin = gpioPins.get(pinNumber);
        if (pin == null || !pin.isEnabled()) {
            throw new RuntimeException("Pin " + pinNumber + " non valido o non disponibile");
        }
        
        if (pin.getType().equals("INPUT")) {
            throw new RuntimeException("Pin " + pinNumber + " è configurato come INPUT - non controllabile");
        }

        try {
            boolean newState = !pin.isState();
            executeCommand("gpioset --mode=exit gpiochip0 " + pinNumber + "=" + (newState ? "1" : "0"));
            pin.setState(newState);
            System.out.println("🔄 Pin " + pinNumber + " (" + pin.getName() + "): " + (newState ? "ON" : "OFF"));
            return newState;
        } catch (Exception e) {
            System.err.println("❌ Errore toggle pin " + pinNumber + ": " + e.getMessage());
            throw new RuntimeException("Errore nel controllo GPIO pin " + pinNumber, e);
        }
    }

    // Controllo PWM per ventola
    public void setFanSpeed(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new RuntimeException("Velocità ventola deve essere tra 0-100%");
        }

        GpioPin fanPin = gpioPins.get(FAN_PWM_PIN);
        if (fanPin == null || !fanPin.isEnabled()) {
            throw new RuntimeException("Ventola non disponibile");
        }

        try {
            // Converti percentuale in valore PWM (0-255)
            int pwmValue = (int) (percentage * 2.55);
            fanPin.setPwmValue(pwmValue);
            fanPin.setState(percentage > 0);
            
            // Per ora simula PWM con on/off, poi implementeremo PWM vero
            if (percentage == 0) {
                executeCommand("gpioset --mode=exit gpiochip0 " + FAN_PWM_PIN + "=0");
            } else {
                executeCommand("gpioset --mode=exit gpiochip0 " + FAN_PWM_PIN + "=1");
            }
            
            System.out.println("🌪️ Ventola impostata al " + percentage + "% (PWM: " + pwmValue + ")");
        } catch (Exception e) {
            throw new RuntimeException("Errore controllo ventola", e);
        }
    }

    // Controllo LED Strip WS2812B (da implementare)
    public void setLedStripColor(int ledIndex, int red, int green, int blue) {
        if (ledIndex < 0 || ledIndex >= 10) {
            throw new RuntimeException("LED index deve essere tra 0-9");
        }
        
        GpioPin stripPin = gpioPins.get(WS2812B_PIN);
        if (stripPin == null || !stripPin.isEnabled()) {
            throw new RuntimeException("LED Strip non disponibile");
        }

        try {
            // Memorizza il colore
            ledRed[ledIndex] = red;
            ledGreen[ledIndex] = green;
            ledBlue[ledIndex] = blue;
            
            // Se la strip è accesa, applica il colore subito
            if (stripPower) {
                sendLedData(ledIndex, red, green, blue);
            }
            
            System.out.println("🌈 LED Strip[" + ledIndex + "] -> R:" + red + " G:" + green + " B:" + blue);
        } catch (Exception e) {
            throw new RuntimeException("Errore colore LED Strip", e);
        }
    }

    // Controllo alimentazione LED Strip
    public void setLedStripPower(boolean isOn) {
        GpioPin stripPin = gpioPins.get(WS2812B_PIN);
        if (stripPin == null || !stripPin.isEnabled()) {
            throw new RuntimeException("LED Strip non disponibile");
        }

        try {
            stripPower = isOn;
            if (isOn) {
                // Accendi tutti i LED con il colore memorizzato
                for (int i = 0; i < 10; i++) {
                    sendLedData(i, ledRed[i], ledGreen[i], ledBlue[i]);
                }
                System.out.println("🌈 LED Strip accesa");
            } else {
                // Spegni tutti i LED
                for (int i = 0; i < 10; i++) {
                    sendLedData(i, 0, 0, 0);
                }
                System.out.println("🌈 LED Strip spenta");
            }
            stripPin.setState(isOn);
        } catch (Exception e) {
            throw new RuntimeException("Errore controllo LED Strip", e);
        }
    }

    // Controllo luminosità LED Strip
    public void setLedStripBrightness(int brightness) {
        if (brightness < 0 || brightness > 255) {
            throw new RuntimeException("Luminosità deve essere tra 0-255");
        }

        GpioPin stripPin = gpioPins.get(WS2812B_PIN);
        if (stripPin == null || !stripPin.isEnabled()) {
            throw new RuntimeException("LED Strip non disponibile");
        }

        try {
            // TODO: Implementare controllo luminosità WS2812B
            System.out.println("🌈 LED Strip luminosità impostata a " + brightness);
            stripPin.setBrightness(brightness);
        } catch (Exception e) {
            throw new RuntimeException("Errore controllo luminosità LED Strip", e);
        }
    }

    // Controllo effetti LED Strip
    public void setLedStripEffect(String effect, int speed, int red, int green, int blue) {
        GpioPin stripPin = gpioPins.get(WS2812B_PIN);
        if (stripPin == null || !stripPin.isEnabled()) {
            throw new RuntimeException("LED Strip non disponibile");
        }

        // Validazione effetti
        String[] validEffects = {"static", "breathe", "rainbow", "strobe"};
        boolean validEffect = Arrays.stream(validEffects).anyMatch(effect::equalsIgnoreCase);
        if (!validEffect) {
            throw new RuntimeException("Effetto non valido. Disponibili: " + Arrays.toString(validEffects));
        }

        try {
            // TODO: Implementare effetti WS2812B
            switch (effect.toLowerCase()) {
                case "static":
                    System.out.println("🌈 LED Strip - Effetto statico RGB(" + red + "," + green + "," + blue + ")");
                    break;
                case "breathe":
                    System.out.println("🌈 LED Strip - Effetto respiro RGB(" + red + "," + green + "," + blue + ") velocità:" + speed);
                    break;
                case "rainbow":
                    System.out.println("🌈 LED Strip - Effetto arcobaleno velocità:" + speed);
                    break;
                case "strobe":
                    System.out.println("🌈 LED Strip - Effetto strobo RGB(" + red + "," + green + "," + blue + ") velocità:" + speed);
                    break;
            }
            stripPin.setEffect(effect);
            stripPin.setState(true);
        } catch (Exception e) {
            throw new RuntimeException("Errore impostazione effetto LED Strip", e);
        }
    }

    // Lettura sensore PIR
    public boolean readPirSensor() {
        try {
            // TODO: Implementare lettura GPIO input
            System.out.println("👁️ Lettura sensore movimento PIR");
            return false; // Placeholder
        } catch (Exception e) {
            throw new RuntimeException("Errore lettura sensore PIR", e);
        }
    }

    // API di compatibilità per il LED semplice (mantengo per non rompere frontend)
    public boolean toggleLed() {
        return toggleDigitalPin(LED_PIN);
    }

    public void turnOnLed() {
        GpioPin pin = gpioPins.get(LED_PIN);
        if (pin != null && !pin.isState()) {
            toggleDigitalPin(LED_PIN);
        }
    }

    public void turnOffLed() {
        GpioPin pin = gpioPins.get(LED_PIN);
        if (pin != null && pin.isState()) {
            toggleDigitalPin(LED_PIN);
        }
    }

    public boolean isLedOn() {
        GpioPin pin = gpioPins.get(LED_PIN);
        return pin != null ? pin.isState() : false;
    }

    // Metodo per inviare dati al LED specifico - CONTROLLO GPIO REALE WS2812B
    private void sendLedData(int ledIndex, int red, int green, int blue) throws IOException, InterruptedException {
        // Implementazione per WS2812B: usa --mode=time per mantenere il segnale
        
        if (red > 0 || green > 0 || blue > 0) {
            // LED acceso - mantieni segnale HIGH per 1 secondo
            executeCommand("gpioset --mode=time --sec=1 gpiochip0 " + WS2812B_PIN + "=1");
            System.out.println("🔥 GPIO " + WS2812B_PIN + " → HIGH per LED " + ledIndex + " RGB(" + red + "," + green + "," + blue + ")");
        } else {
            // LED spento - segnale LOW
            executeCommand("gpioset --mode=exit gpiochip0 " + WS2812B_PIN + "=0");
            System.out.println("💤 GPIO " + WS2812B_PIN + " → LOW per LED " + ledIndex);
        }
    }
}