# 🚀 OMNIA PROJECT - Backend LED Controller

Backend Java Spring Boot per controllo LED su Raspberry Pi tramite GPIO.

## 🏗️ Tecnologie

- **Java 17** + **Spring Boot 3.2.0**
- **Pi4J 2.4.0** per controllo GPIO
- **Docker** per containerizzazione
- **Maven** per build e gestione dipendenze

## 📋 API Endpoints

| Endpoint | Metodo | Descrizione |
|----------|--------|-------------|
| `/` | GET | Status del server e stato LED |
| `/api/led/toggle` | POST | Cambia stato LED (on/off) |
| `/api/led/on` | POST | Accende LED |
| `/api/led/off` | POST | Spegne LED |
| `/api/led/status` | GET | Stato attuale LED |

## 🔌 Configurazione GPIO

- **Pin GPIO**: 18 (BCM) / Pin fisico 12
- **Tipo**: Digital Output
- **Libreria**: Pi4J 2.4.0

**Schema connessioni:**
```
LED Anodo → GPIO 18 (Pin fisico 12)
LED Catodo → Resistenza 220Ω → GND (Pin fisico 14)
```

## 🐳 Deploy con Docker

### 1. Deploy Automatico (Consigliato)

Sul Raspberry Pi, nella directory del progetto:

```bash
chmod +x deploy.sh
./deploy.sh
```

### 2. Deploy Manuale

```bash
# Build dell'immagine
sudo docker build -t omnia-backend:latest .

# Avvio container
sudo docker run -d \
  --name omniaproject-be \
  --restart unless-stopped \
  --privileged \
  -p 3000:3000 \
  -v /dev:/dev \
  omnia-backend:latest
```

## 🧪 Test API

```bash
# Status del server
curl http://192.168.1.100:3000/

# Toggle LED
curl -X POST http://192.168.1.100:3000/api/led/toggle

# Stato LED
curl http://192.168.1.100:3000/api/led/status
```

## 📦 Struttura del Progetto

```
src/main/java/com/omnia/raspberry/
├── RaspberryControllerApplication.java  # Main Spring Boot
├── controller/
│   └── LedController.java               # REST API Controller
└── service/
    └── GpioService.java                 # Servizio GPIO/Pi4J
```

## 🔧 Requisiti Raspberry Pi

- Ubuntu Server 24.04
- Docker installato
- Accesso GPIO (container con `--privileged`)
- pigpiod daemon per Pi4J

## 🌐 Accesso

- **Backend API**: http://192.168.1.100:3000
- **Portainer**: http://192.168.1.100:9000
- **SSH**: ssh omniaproject@192.168.1.100