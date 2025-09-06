# 📋 OMNIA PROJECT - GUIDA COMPLETA

Sistema completo per controllo LED Raspberry Pi con auto-deploy da VSCode.

## 🎯 **RIAVVIO RAPIDO DOPO SPEGNIMENTO**

**Se spegni e riaccendi il Raspberry Pi, tutto dovrebbe ripartire automaticamente!**

### **Verifica Sistema dopo Riavvio:**
```bash
# 1. Connettiti via SSH
ssh omniaproject@192.168.1.100
# Password: Dcd776c2

# 2. Controlla che i servizi siano attivi
sudo docker ps
# Dovresti vedere: portainer + omniaproject-be-omnia-backend

# 3. Se i container non sono attivi, riavviali:
sudo docker start portainer
sudo docker start omniaproject-be  # (o il nome del container backend)

# 4. Testa che il LED funzioni
curl -X POST http://localhost:3000/api/led/toggle
```

### **Servizi Configurati per Auto-Start:**
✅ **Docker** - Si avvia automaticamente  
✅ **Portainer** - Container con `--restart=always`  
✅ **Backend Stack** - Gestito da Portainer con restart policy  

## 🚀 **SETUP COMPLETO DA ZERO**

Se il Raspberry Pi si rompe o devi rifare tutto da capo:

### 1. **Installazione Ubuntu Server 24.04**
- Scarica Ubuntu Server 24.04 LTS per Raspberry Pi
- Flash su microSD con Raspberry Pi Imager
- Configura SSH, WiFi e password durante il flash

### 2. **Configurazione Iniziale Sistema**
```bash
# Connessione SSH
ssh omniaproject@192.168.1.100
# Password: Dcd776c2

# Aggiorna sistema
sudo apt update && sudo apt upgrade -y

# Installa Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker omniaproject

# Installa Portainer con auto-restart
sudo docker run -d -p 9000:9000 --name=portainer --restart=always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest

# Installa strumenti GPIO
sudo apt install gpiod git -y
```

### 3. **Deploy Backend OMNIA con Portainer**
1. **Vai su**: http://192.168.1.100:9000
2. **Login** e vai su **Stacks** → **Add Stack**
3. **Configurazione:**
   - **Name**: `omnia-backend`
   - **Build method**: Repository
   - **Repository URL**: `https://github.com/BomberStealth/OmniaProject-be`
   - **Repository reference**: `refs/heads/main`
   - **Compose path**: `docker-compose.yml`
   - **GitOps updates**: ✅ Enable (ogni 120 secondi)
4. **Deploy the stack**

## 🔄 **RIAVVIO DOPO SPEGNIMENTO**

Comandi per riavviare tutto dopo un riavvio del sistema:

### **Avvio Automatico (Già Configurato)**
I container sono configurati con `--restart unless-stopped`, quindi si riavviano automaticamente.

### **Controllo Stato Sistema**
```bash
# Controlla tutti i container
sudo docker ps -a

# Se non sono attivi, riavviali
sudo docker start portainer
sudo docker start omniaproject-be

# Verifica funzionamento
curl http://localhost:3000/api/led/status
```

### **In Caso di Problemi**
```bash
# Riavvia tutto da zero
sudo docker restart omniaproject-be
sleep 10

# Testa il LED
curl -X POST http://localhost:3000/api/led/toggle

# Se non funziona, redeploy
./deploy.sh
```

## 🛠️ **COMANDI UTILI QUOTIDIANI**

### **Controllo Sistema**
```bash
# Stato container
sudo docker ps

# Log backend in tempo reale
sudo docker logs omniaproject-be -f

# Stato sistema
htop
df -h
```

### **Controllo LED**
```bash
# Test manuale GPIO
sudo gpioset gpiochip0 18=1  # Accendi
sudo gpioset gpiochip0 18=0  # Spegni

# Test API
curl http://localhost:3000/
curl -X POST http://localhost:3000/api/led/toggle
curl http://localhost:3000/api/led/status
```

### **Gestione Container**
```bash
# Fermare backend
sudo docker stop omniaproject-be

# Riavviare backend  
sudo docker restart omniaproject-be

# Rimuovere e ricreare (con nuove modifiche)
sudo docker stop omniaproject-be
sudo docker rm omniaproject-be
./deploy.sh
```

## 🔄 **WORKFLOW COMPLETO - AUTO-DEPLOY ATTIVO!**

### **Come Modificare il Sistema da VSCode:**

#### **1. Sul PC (VSCode):**
```bash
# Modifica i file nel progetto OmniaProject-be
# Es: src/main/java/com/omnia/raspberry/service/GpioService.java

# In VSCode:
# 1. Vai su Source Control (Ctrl+Shift+G)
# 2. Scrivi commit message
# 3. Clicca "Commit & Push"
```

#### **2. Auto-Deploy (Portainer fa tutto):**
- ✅ **Portainer** rileva modifiche GitHub ogni 2 minuti
- ✅ **Pull** automatico del codice aggiornato  
- ✅ **Build** automatica nuova immagine Docker
- ✅ **Deploy** automatico container aggiornato
- ✅ **Restart** automatico servizi

#### **3. Verifica modifiche:**
```bash
# Dopo 2-3 minuti dal push:
curl http://192.168.1.100:3000/
curl -X POST http://192.168.1.100:3000/api/led/toggle
```

## 🌐 **ACCESSI E INDIRIZZI**

### **Rete e Connessioni**
- **IP Raspberry Pi**: 192.168.1.100
- **SSH**: ssh omniaproject@192.168.1.100 (password: Dcd776c2)
- **Hostname**: omniaproject.local

### **Servizi Web**
- **Backend API**: http://192.168.1.100:3000
- **Portainer GUI**: http://192.168.1.100:9000
- **Frontend React**: http://localhost:5173 (avviato da PC)

### **Frontend React - Avvio:**
```bash
# Sul PC Windows
cd "C:\Users\edoar\Desktop\Omnia Project\OmniaProject"
npm run dev
# Apri browser su: http://localhost:5173
```

### **Endpoint API Backend**
| Endpoint | Metodo | Descrizione |
|----------|--------|-------------|
| `/` | GET | Status del server + stato LED |
| `/api/led/status` | GET | Stato attuale LED |
| `/api/led/toggle` | POST | Cambia stato LED (ON/OFF) |
| `/api/led/on` | POST | Accendi LED |
| `/api/led/off` | POST | Spegni LED |

### **Test API Rapidi**
```bash
# Status completo sistema
curl http://192.168.1.100:3000/

# Toggle LED fisico
curl -X POST http://192.168.1.100:3000/api/led/toggle

# Solo stato LED
curl http://192.168.1.100:3000/api/led/status
```

## 📁 **STRUTTURA FILE IMPORTANTI**

```
~/OmniaProject-be/
├── deploy.sh              # Script deploy automatico
├── Dockerfile              # Configurazione container
├── src/                    # Codice sorgente Java
├── pom.xml                 # Configurazione Maven
└── INFO-UTILI.md          # Questo file
```

## 🚨 **TROUBLESHOOTING**

### **LED Non Funziona**
```bash
# Test manuale GPIO
sudo gpioset gpiochip0 18=1
sudo gpioset gpiochip0 18=0

# Se funziona manualmente ma non via API:
sudo docker restart omniaproject-be
```

### **Container Non Si Avvia**
```bash
# Controlla log errori
sudo docker logs omniaproject-be

# Ricostruisci immagine
sudo docker build -t omnia-backend:latest .
./deploy.sh
```

### **Problemi di Rete**
```bash
# Verifica IP
hostname -I

# Test connettività
ping google.com
curl http://localhost:3000/
```

## 📞 **SUPPORTO**

In caso di problemi gravi:
1. Salva i log: `sudo docker logs omniaproject-be > logs.txt`
2. Controlla i file di configurazione  
3. Rifai il deploy completo con `./deploy.sh`
4. Se necessario, ricomincia da "Setup Completo da Zero"

## 🎯 **RISOLUZIONE PROBLEMI RAPIDI**

### **LED Non Risponde:**
```bash
# Test GPIO manuale
sudo gpioset gpiochip0 18=1  # Accendi
sudo gpioset gpiochip0 18=0  # Spegni

# Se GPIO funziona ma API no:
sudo docker restart omniaproject-be
curl -X POST http://localhost:3000/api/led/toggle
```

### **Container Non Si Avvia:**
```bash
# Controlla log errori
sudo docker logs omniaproject-be

# Ricostruisci se necessario
cd ~/OmniaProject-be
git pull origin main
sudo docker compose build --no-cache
sudo docker compose up -d
```

### **Frontend Non Vede Backend:**
- ✅ Verifica IP in `ledService.js`: `192.168.1.100:3000`
- ✅ Testa backend: `curl http://192.168.1.100:3000/`
- ✅ Riavvia frontend: `npm run dev`

### **Auto-Deploy Non Funziona:**
1. **Verifica Portainer Stack**: http://192.168.1.100:9000/#!/stacks
2. **Controlla GitOps**: deve essere abilitato ogni 120 secondi
3. **Test manuale**: `git pull origin main` + `sudo docker compose up -d`

---

## 📚 **GUIDA RAPIDA - 3 STEP:**

### **🚀 Per Iniziare:**
1. **SSH**: `ssh omniaproject@192.168.1.100` (password: Dcd776c2)
2. **Frontend**: `cd "C:\Users\edoar\Desktop\Omnia Project\OmniaProject" && npm run dev`
3. **Browser**: http://localhost:5173 → Clicca bottone LED

### **⚡ Per Modificare:**
1. **VSCode**: Modifica file in `OmniaProject-be`
2. **Source Control**: Commit & Push
3. **Attendi 2 min**: Portainer fa auto-deploy

### **🔧 Per Riavviare:**
1. **Raspberry Pi**: Si avvia tutto automaticamente  
2. **Se serve**: `sudo docker ps` → `sudo docker restart omniaproject-be`
3. **Test**: `curl -X POST http://192.168.1.100:3000/api/led/toggle`

---
**Ultimo aggiornamento: 2025-09-06**  
**Sistema: Ubuntu 24.04 + Docker + Portainer + Spring Boot + GPIO + React Frontend**  
**Status: ✅ SISTEMA COMPLETO E FUNZIONANTE**