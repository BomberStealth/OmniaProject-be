# 📋 OMNIA PROJECT - INFO UTILI

Guida completa per gestire il sistema OMNIA su Raspberry Pi.

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

# Installa Portainer
sudo docker run -d -p 9000:9000 --name=portainer --restart=always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest

# Installa strumenti GPIO
sudo apt install gpiod git -y
```

### 3. **Deploy Backend OMNIA**
```bash
# Clona repository GitHub esistente
git clone https://github.com/BomberStealth/OmniaProject-be.git
cd OmniaProject-be

# Esegui deploy automatico
chmod +x deploy.sh
./deploy.sh
```

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

## 🔄 **DEPLOY AUTOMATICO CON GIT**

### **Setup Git Hooks (Da Fare)**

Per avere deploy automatico quando fai push:

#### **Sul PC (Repository Locale)**
```bash
# Assicurati che il repository abbia origin configurato
git remote -v

# Push delle modifiche
git add .
git commit -m "Aggiornamenti backend"
git push origin main
```

#### **Sul Raspberry Pi (Setup Hook)**
```bash
# Naviga nella directory del progetto
cd ~/OmniaProject-be

# Crea script di pull automatico
cat > git-pull-deploy.sh << 'EOF'
#!/bin/bash
echo "🔄 Git Pull e Deploy Automatico"
cd ~/OmniaProject-be
git pull origin main
./deploy.sh
EOF

chmod +x git-pull-deploy.sh

# Configura cron per controllo ogni 5 minuti (opzionale)
crontab -e
# Aggiungi: */5 * * * * /home/omniaproject/OmniaProject-be/git-pull-deploy.sh >> /tmp/git-pull.log 2>&1
```

#### **Setup GitHub Webhook (Avanzato)**
1. Installa webhook receiver sul Raspberry Pi
2. Configura endpoint su GitHub
3. Deploy automatico ad ogni push

## 🌐 **ACCESSI E INDIRIZZI**

### **Rete e Connessioni**
- **IP Raspberry Pi**: 192.168.1.100
- **SSH**: ssh omniaproject@192.168.1.100 (password: Dcd776c2)
- **Hostname**: omniaproject.local

### **Servizi Web**
- **Backend API**: http://192.168.1.100:3000
- **Portainer GUI**: http://192.168.1.100:9000
- **Health Check**: http://192.168.1.100:3000/api/led/status

### **Endpoint API**
| Endpoint | Metodo | Descrizione |
|----------|--------|-------------|
| `/` | GET | Status del server |
| `/api/led/status` | GET | Stato attuale LED |
| `/api/led/toggle` | POST | Cambia stato LED |
| `/api/led/on` | POST | Accendi LED |
| `/api/led/off` | POST | Spegni LED |

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

---
**Ultimo aggiornamento: 2025-09-06**  
**Sistema: Ubuntu 24.04 + Docker + Spring Boot + GPIO**