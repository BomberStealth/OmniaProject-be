#!/bin/bash

# Script di inizializzazione per Raspberry Pi - Backend Java
# Configura automaticamente il backend Java LED controller

echo "🚀 Inizializzazione del backend Java LED controller sul Raspberry Pi..."

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_DIR="$HOME/OmniaProject-Java"
REPO_URL="https://github.com/BomberStealth/OmniaProject-be.git"

# Funzione per stampare messaggi colorati
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 1. Aggiornamento sistema
print_status "Aggiornamento del sistema..."
sudo apt update && sudo apt upgrade -y

# 2. Installazione OpenJDK 17
print_status "Installazione OpenJDK 17..."
sudo apt install -y openjdk-17-jdk

# 3. Installazione Maven
print_status "Installazione Maven..."
sudo apt install -y maven

# 4. Installazione Git (se non presente)
print_status "Verifica installazione Git..."
sudo apt install -y git

# 5. Clonazione del repository (se non esiste)
if [ -d "$PROJECT_DIR" ]; then
    print_warning "Directory $PROJECT_DIR già esistente. Aggiornamento..."
    cd "$PROJECT_DIR"
    git pull origin main
else
    print_status "Clonazione del repository Java..."
    git clone "$REPO_URL" "$PROJECT_DIR"
    cd "$PROJECT_DIR"
fi

# 6. Build del progetto Maven
print_status "Build del progetto Maven..."
mvn clean package -DskipTests

# 7. Verifica jar generato
if [ -f "target/raspberry-controller-1.0.0.jar" ]; then
    print_status "✅ JAR compilato con successo"
else
    print_error "❌ Errore nella compilazione del JAR"
    exit 1
fi

# 8. Creazione del servizio systemd
print_status "Creazione servizio systemd..."
sudo tee /etc/systemd/system/omnia-java-led.service > /dev/null <<EOF
[Unit]
Description=Omnia LED Controller Java Backend
After=network-online.target
Wants=network-online.target

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

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=omnia-java-led

# Sicurezza
NoNewPrivileges=yes
ProtectSystem=strict
ProtectHome=read-only
ReadWritePaths=$PROJECT_DIR
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
EOF

# 9. Abilitazione e avvio del servizio
print_status "Abilitazione servizio Java..."
sudo systemctl daemon-reload
sudo systemctl enable omnia-java-led.service
sudo systemctl start omnia-java-led.service

# 10. Setup del git hook per auto-deploy Java
print_status "Configurazione auto-deploy Java..."
mkdir -p .git/hooks

cat > .git/hooks/post-merge <<EOF
#!/bin/bash
echo "🔄 Auto-deploy Java attivato..."
cd $PROJECT_DIR

# Build del progetto
mvn clean package -DskipTests -q
if [ \$? -eq 0 ]; then
    echo "✅ Build completato con successo"
    sudo systemctl restart omnia-java-led.service
    echo "✅ Servizio Java riavviato!"
else
    echo "❌ Errore nel build Maven"
    exit 1
fi
EOF

chmod +x .git/hooks/post-merge

# 11. Creazione cron job per controllo automatico
print_status "Configurazione cron job per auto-pull Java..."
(crontab -l 2>/dev/null; echo "* * * * * cd $PROJECT_DIR && git fetch && [ \$(git rev-list HEAD...origin/main --count) != 0 ] && git pull origin main") | crontab -

# 12. Verifica stato servizio
print_status "Verifica stato servizio Java..."
sleep 5
sudo systemctl status omnia-java-led.service --no-pager -l

# 13. Test connessione
print_status "Test connessione al server Java..."
sleep 3
if curl -f http://localhost:3000/ > /dev/null 2>&1; then
    print_status "✅ Server Java funzionante! Accessibile su http://$(hostname -I | cut -d' ' -f1):3000"
else
    print_error "❌ Errore: il server Java non risponde"
fi

print_status "🎉 Setup Java completato!"
print_status "Server: Java Spring Boot + Pi4J"
print_status "GPIO: Pin 18 (BCM) - Pin 12 fisico"
print_status "Il server si riavvierà automaticamente ad ogni push su Git"
print_status "Per controllare i log: sudo journalctl -u omnia-java-led.service -f"
print_status "Per riavviare: sudo systemctl restart omnia-java-led.service"