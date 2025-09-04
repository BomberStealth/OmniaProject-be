#!/bin/bash

# Script per configurare i git hooks per auto-deploy Java
# Questo script deve essere eseguito sul Raspberry Pi dopo il setup

PROJECT_DIR="$(pwd)"
SERVICE_NAME="omnia-java-led"

echo "🔧 Configurazione git hooks per auto-deploy Java..."

# Git hook per post-merge (quando fai git pull)
cat > .git/hooks/post-merge <<'EOF'
#!/bin/bash

echo "🔄 [JAVA AUTO-DEPLOY] Rilevato aggiornamento Git..."

PROJECT_DIR="$(pwd)"
LOG_FILE="/tmp/omnia-java-deploy.log"

# Funzione per logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "Inizio auto-deploy Java..."

# 1. Maven build
log "Compilazione Maven in corso..."
mvn clean package -DskipTests -q >> "$LOG_FILE" 2>&1

if [ $? -eq 0 ]; then
    log "✅ Maven build completato con successo"
else
    log "❌ Errore nel build Maven"
    cat "$LOG_FILE" | tail -20
    exit 1
fi

# 2. Verifica JAR generato
if [ -f "target/raspberry-controller-1.0.0.jar" ]; then
    log "✅ JAR generato: target/raspberry-controller-1.0.0.jar"
else
    log "❌ JAR non trovato dopo il build"
    exit 1
fi

# 3. Restart del servizio
log "Riavvio del servizio omnia-java-led..."
sudo systemctl restart omnia-java-led.service

if [ $? -eq 0 ]; then
    log "✅ Servizio riavviato con successo"
else
    log "❌ Errore nel riavvio del servizio"
    exit 1
fi

# 4. Attesa avvio servizio
log "Attesa avvio servizio Java..."
sleep 8

# 5. Verifica che il servizio sia attivo
if sudo systemctl is-active --quiet omnia-java-led.service; then
    log "✅ Servizio Java attivo e funzionante"
    
    # Test rapido del server
    if curl -f http://localhost:3000/ > /dev/null 2>&1; then
        log "✅ Server Java risponde correttamente"
        curl -s http://localhost:3000/ | jq -r '.service' >> "$LOG_FILE" 2>/dev/null || echo "Java Backend" >> "$LOG_FILE"
    else
        log "⚠️  Warning: Server Java non risponde al test"
    fi
else
    log "❌ Servizio Java non attivo dopo il riavvio"
    sudo systemctl status omnia-java-led.service >> "$LOG_FILE" 2>&1
fi

log "🎉 Auto-deploy Java completato!"
EOF

chmod +x .git/hooks/post-merge

echo "✅ Git hooks Java configurati"

# Crea script per monitoring dei deployments Java
cat > monitor-java-deploy.sh <<'EOF'
#!/bin/bash

# Script per monitorare i log di deployment Java

LOG_FILE="/tmp/omnia-java-deploy.log"
SERVICE_LOG="omnia-java-led.service"

echo "📊 Monitor dei deployments Java - CTRL+C per uscire"
echo "================================================="

# Mostra info servizio
echo "🔧 Servizio: $SERVICE_LOG"
sudo systemctl is-active --quiet $SERVICE_LOG && echo "📱 Status: 🟢 ATTIVO" || echo "📱 Status: 🔴 SPENTO"
echo ""

# Mostra gli ultimi deploy se esistenti
if [ -f "$LOG_FILE" ]; then
    echo "📋 Ultimi deployments Java:"
    tail -20 "$LOG_FILE"
    echo "================================================="
fi

echo "📡 Monitoraggio in tempo reale..."
echo ""

# Segui i nuovi log in tempo reale
tail -f "$LOG_FILE" 2>/dev/null || {
    echo "⏳ In attesa del primo deployment Java..."
    while [ ! -f "$LOG_FILE" ]; do
        sleep 1
    done
    tail -f "$LOG_FILE"
}
EOF

chmod +x monitor-java-deploy.sh

# Script per rebuild manuale
cat > rebuild-java.sh <<'EOF'
#!/bin/bash

echo "🔨 Rebuild manuale Java..."

# Build Maven
echo "📦 Maven clean package..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build completato"
    
    # Restart servizio
    echo "🔄 Riavvio servizio..."
    sudo systemctl restart omnia-java-led.service
    
    # Test
    sleep 3
    if curl -f http://localhost:3000/ > /dev/null 2>&1; then
        echo "✅ Server Java funzionante!"
        curl -s http://localhost:3000/ | jq '.' 2>/dev/null || curl -s http://localhost:3000/
    else
        echo "❌ Server non risponde"
    fi
else
    echo "❌ Errore nel build"
fi
EOF

chmod +x rebuild-java.sh

echo ""
echo "🚀 Script Java creati:"
echo "  📊 ./monitor-java-deploy.sh - Monitor deployments"
echo "  🔨 ./rebuild-java.sh - Rebuild manuale"
echo ""
echo "🎯 Per testare l'auto-deploy Java:"
echo "  1. Fai modifiche al codice Java dal PC"
echo "  2. Commit e push su GitHub"
echo "  3. Sul Raspberry Pi: git pull"
echo "  4. Maven compilerà automaticamente e riavvierà il servizio!"