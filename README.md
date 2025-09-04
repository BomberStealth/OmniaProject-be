# Omnia Project Backend

Backend Java Spring Boot per il controllo GPIO del Raspberry Pi.

## Requisiti

- Java 17+
- Maven 3.6+
- Raspberry Pi con Raspberry Pi OS
- Pi4J 2.4.0 per il controllo GPIO

## API Endpoints

### LED Control
- `GET /api/led/status` - Ottieni stato attuale del LED
- `POST /api/led/toggle` - Toggle dello stato del LED
- `POST /api/led/on` - Accendi LED
- `POST /api/led/off` - Spegni LED

## Configurazione GPIO

Il backend utilizza il pin GPIO 18 (BCM) del Raspberry Pi per controllare un LED.

**Schema connessioni:**
```
LED Anodo → GPIO 18 (Pin fisico 12)
LED Catodo → Resistenza 220Ω → GND (Pin fisico 14)
```

## Sviluppo Locale

```bash
# Compila e testa
./mvnw clean compile

# Esegui in modalità sviluppo
./mvnw spring-boot:run

# Package
./mvnw clean package
```

Il backend funziona in modalità simulazione su PC per lo sviluppo.

## Deployment su Raspberry Pi

### Setup iniziale

1. Clona il repository:
```bash
git clone https://github.com/BomberStealth/OmniaProject-be.git
cd OmniaProject-be
```

2. Installa Java 17:
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

3. Compila il progetto:
```bash
./mvnw clean package -DskipTests
```

4. Configura il servizio systemd:
```bash
sudo cp raspberry-controller.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable raspberry-controller
sudo systemctl start raspberry-controller
```

### Deployment automatico

Per il deployment automatico dopo ogni push:

```bash
# Rendi eseguibile lo script
chmod +x deploy.sh

# Esegui deployment manuale
./deploy.sh
```

## Comandi Utili

```bash
# Stato del servizio
sudo systemctl status raspberry-controller

# Log del servizio
sudo journalctl -u raspberry-controller -f

# Riavvia servizio
sudo systemctl restart raspberry-controller

# Test API
curl http://localhost:3000/api/led/status
curl -X POST http://localhost:3000/api/led/toggle
```

## Architettura

```
src/
├── main/
│   ├── java/com/omnia/raspberry/
│   │   ├── RaspberryControllerApplication.java  # Main class + CORS config
│   │   ├── controller/
│   │   │   └── LedController.java               # REST Controller
│   │   └── service/
│   │       └── GpioService.java                 # GPIO management
│   └── resources/
│       └── application.properties               # Configurazione
├── deploy.sh                                    # Script deployment
├── raspberry-controller.service                 # Systemd service
└── pom.xml                                      # Maven configuration
```