#!/bin/bash

# ========================================
# SETUP COMPLETO RASPBERRY PI - BACKEND JAVA
# ========================================
# Questo script installa e configura TUTTO automaticamente
# Non serve più toccare la console del Raspberry Pi
# ========================================

echo "🚀 SETUP COMPLETO RASPBERRY PI - BACKEND JAVA"
echo "=============================================="

# Colori
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() { echo -e "${BLUE}[STEP]${NC} $1"; }
print_ok() { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

PROJECT_DIR="$HOME/OmniaProject-Java"
REPO_URL="https://github.com/BomberStealth/OmniaProject-be.git"
SERVICE_NAME="omnia-java-led"

# ========================================
# STEP 1: SISTEMA BASE
# ========================================
print_step "Aggiornamento sistema..."
sudo apt update && sudo apt upgrade -y
print_ok "Sistema aggiornato"

# ========================================
# STEP 2: INSTALLAZIONE JAVA 17
# ========================================
print_step "Installazione OpenJDK 17..."
sudo apt install -y openjdk-17-jdk
print_ok "Java 17 installato"

# ========================================
# STEP 3: INSTALLAZIONE MAVEN
# ========================================
print_step "Installazione Maven..."
sudo apt install -y maven
print_ok "Maven installato"

# ========================================
# STEP 4: INSTALLAZIONE GIT
# ========================================
print_step "Verifica Git..."
sudo apt install -y git
print_ok "Git verificato"

# ========================================
# STEP 5: PIGPIO (NECESSARIO PER PI4J)
# ========================================
print_step "Installazione pigpio per Pi4J..."
sudo apt install -y pigpio
sudo systemctl enable pigpiod
sudo systemctl start pigpiod
print_ok "Pigpio configurato e avviato"

# ========================================
# STEP 6: CLONE E BUILD PROGETTO
# ========================================
print_step "Setup progetto Java..."

# Rimuovi installazioni precedenti
if [ -d "$PROJECT_DIR" ]; then
    print_warn "Rimuovo installazione precedente..."
    sudo systemctl stop $SERVICE_NAME 2>/dev/null || true
    sudo systemctl disable $SERVICE_NAME 2>/dev/null || true
    rm -rf "$PROJECT_DIR"
fi

# Clone fresh
git clone "$REPO_URL" "$PROJECT_DIR"
cd "$PROJECT_DIR"

print_step "Build progetto Maven..."
mvn clean package -DskipTests -q

if [ -f "target/raspberry-controller-1.0.0.jar" ]; then
    print_ok "JAR compilato: target/raspberry-controller-1.0.0.jar"
else
    print_error "Build fallito!"
    exit 1
fi

# ========================================
# STEP 7: SERVIZIO SYSTEMD
# ========================================
print_step "Creazione servizio systemd..."

sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null <<EOF
[Unit]
Description=Omnia LED Controller Java Backend
Documentation=https://github.com/BomberStealth/OmniaProject-be
After=network-online.target pigpiod.service
Wants=network-online.target
Requires=pigpiod.service

[Service]
Type=simple
User=$USER
Group=$USER
WorkingDirectory=$PROJECT_DIR
ExecStart=/usr/bin/java -jar target/raspberry-controller-1.0.0.jar
ExecReload=/bin/kill -HUP \$MAINPID
Restart=always
RestartSec=10
TimeoutStopSec=20
KillMode=mixed

# Variabili ambiente
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
Environment=PATH=/usr/lib/jvm/java-17-openjdk-arm64/bin:/usr/bin:/bin
Environment=SERVER_PORT=3000
Environment=SPRING_PROFILES_ACTIVE=raspberry

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=$SERVICE_NAME

# Sicurezza
NoNewPrivileges=yes
ProtectSystem=strict
ProtectHome=read-only
ReadWritePaths=$PROJECT_DIR
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
EOF

print_ok "Servizio systemd creato"

# ========================================
# STEP 8: AUTO-DEPLOY CONFIGURATION
# ========================================
print_step "Configurazione auto-deploy..."

# Git hooks directory
mkdir -p .git/hooks

# Post-merge hook (si attiva dopo git pull)
cat > .git/hooks/post-merge <<EOF
#!/bin/bash
echo "🔄 [AUTO-DEPLOY] Rilevato aggiornamento Git..."

PROJECT_DIR="$PROJECT_DIR"
LOG_FILE="/tmp/omnia-java-deploy.log"

log() {
    echo "[\$(date '+%Y-%m-%d %H:%M:%S')] \$1" | tee -a "\$LOG_FILE"
}

log "=== INIZIO AUTO-DEPLOY ==="

# Build Maven
log "📦 Build Maven in corso..."
cd "\$PROJECT_DIR"
mvn clean package -DskipTests -q >> "\$LOG_FILE" 2>&1

if [ \$? -eq 0 ]; then
    log "✅ Build Maven completato"
else
    log "❌ Build Maven fallito"
    exit 1
fi

# Verifica JAR
if [ -f "target/raspberry-controller-1.0.0.jar" ]; then
    log "✅ JAR verificato"
else
    log "❌ JAR non trovato"
    exit 1
fi

# Restart servizio
log "🔄 Riavvio servizio $SERVICE_NAME..."
sudo systemctl restart $SERVICE_NAME

if [ \$? -eq 0 ]; then
    log "✅ Servizio riavviato"
else
    log "❌ Errore riavvio servizio"
    exit 1
fi

# Verifica servizio attivo
sleep 5
if sudo systemctl is-active --quiet $SERVICE_NAME; then
    log "✅ Servizio attivo e funzionante"
    
    # Test API
    if curl -f http://localhost:3000/ > /dev/null 2>&1; then
        log "✅ API Java risponde correttamente"
    else
        log "⚠️  API non risponde al test"
    fi
else
    log "❌ Servizio non attivo"
    sudo systemctl status $SERVICE_NAME >> "\$LOG_FILE" 2>&1
fi

log "🎉 AUTO-DEPLOY COMPLETATO"
log "==========================="
EOF

chmod +x .git/hooks/post-merge

# ========================================
# STEP 9: CRON JOB AUTO-PULL
# ========================================
print_step "Configurazione auto-pull..."

# Script per check automatico ogni minuto
CRON_JOB="* * * * * cd $PROJECT_DIR && git fetch >/dev/null 2>&1 && [ \$(git rev-list HEAD...origin/main --count 2>/dev/null || echo 0) != 0 ] && git pull origin main >/dev/null 2>&1"

# Aggiungi al crontab se non esiste
(crontab -l 2>/dev/null | grep -v "OmniaProject-Java"; echo "$CRON_JOB") | crontab -

print_ok "Cron job configurato (controllo ogni minuto)"

# ========================================
# STEP 10: SCRIPT UTILI
# ========================================
print_step "Creazione script utility..."

# Monitor deploy
cat > monitor-deploy.sh <<'EOF'
#!/bin/bash
echo "📊 Monitor Auto-Deploy Java"
echo "============================="
tail -f /tmp/omnia-java-deploy.log 2>/dev/null || echo "In attesa primo deploy..."
EOF
chmod +x monitor-deploy.sh

# Manual rebuild
cat > rebuild-manual.sh <<'EOF'
#!/bin/bash
echo "🔨 Rebuild Manuale"
mvn clean package -DskipTests
sudo systemctl restart omnia-java-led
echo "✅ Rebuild completato"
EOF
chmod +x rebuild-manual.sh

# Status check
cat > status-check.sh <<'EOF'
#!/bin/bash
echo "📊 STATUS OMNIA JAVA BACKEND"
echo "============================="
echo "🖥️  Sistema: $(hostname)"
echo "📅 Data: $(date)"
echo "📱 IP: $(hostname -I | cut -d' ' -f1)"
echo ""
echo "🔧 Servizio:"
sudo systemctl status omnia-java-led --no-pager -l
echo ""
echo "🌐 API Test:"
curl -s http://localhost:3000/ | python3 -m json.tool 2>/dev/null || echo "API non risponde"
EOF
chmod +x status-check.sh

print_ok "Script utility creati"

# ========================================
# STEP 11: AVVIO SERVIZIO
# ========================================
print_step "Avvio servizio finale..."

sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME
sudo systemctl start $SERVICE_NAME

sleep 5

# ========================================
# STEP 12: VERIFICA FINALE
# ========================================
print_step "Verifica finale..."

# Check servizio
if sudo systemctl is-active --quiet $SERVICE_NAME; then
    print_ok "✅ Servizio $SERVICE_NAME attivo"
else
    print_error "❌ Servizio non attivo"
    sudo systemctl status $SERVICE_NAME --no-pager -l
    exit 1
fi

# Check API
if curl -f http://localhost:3000/ > /dev/null 2>&1; then
    print_ok "✅ API Java risponde su porta 3000"
    IP_ADDRESS=$(hostname -I | cut -d' ' -f1)
    print_ok "🌐 Server accessibile: http://$IP_ADDRESS:3000"
else
    print_warn "⚠️  API non risponde (normale nei primi secondi)"
fi

# ========================================
# STEP 13: SOMMARIO FINALE
# ========================================
echo ""
echo "🎉 SETUP COMPLETATO CON SUCCESSO!"
echo "=================================="
echo ""
echo "📊 SOMMARIO:"
echo "  🔧 Backend Java: Spring Boot + Pi4J"
echo "  📍 GPIO Pin: 18 (Pin 12 fisico)"
echo "  🌐 Server: http://$(hostname -I | cut -d' ' -f1):3000"
echo "  🔄 Auto-deploy: ATTIVO (ogni minuto)"
echo ""
echo "🛠️  SCRIPT DISPONIBILI:"
echo "  ./status-check.sh      - Verifica stato sistema"
echo "  ./monitor-deploy.sh    - Monitor auto-deploy"
echo "  ./rebuild-manual.sh    - Rebuild manuale"
echo ""
echo "📝 LOG SERVIZIO:"
echo "  sudo journalctl -u $SERVICE_NAME -f"
echo ""
echo "🔌 COLLEGAMENTO LED:"
echo "  Pin 12 (GPIO 18) → LED Anodo (+)"
echo "  LED Catodo (-)   → Resistenza 220Ω → Pin 6 (GND)"
echo ""
echo "🚀 WORKFLOW:"
echo "  1. Modifichi codice Java dal PC"
echo "  2. Fai: git push"
echo "  3. Raspberry Pi si aggiorna automaticamente!"
echo ""
print_ok "Setup completato - Sistema pronto!"