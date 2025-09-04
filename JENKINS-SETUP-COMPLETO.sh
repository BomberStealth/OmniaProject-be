#!/bin/bash

# ========================================
# JENKINS SETUP COMPLETO PER RASPBERRY PI
# ========================================
# Installa Jenkins con interfaccia grafica web
# Auto-deploy completo per Java LED Controller
# ========================================

echo "🚀 JENKINS SETUP COMPLETO - RASPBERRY PI"
echo "========================================"

# Colori
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

print_step() { echo -e "${BLUE}[STEP]${NC} $1"; }
print_ok() { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_info() { echo -e "${PURPLE}[INFO]${NC} $1"; }

PROJECT_DIR="$HOME/OmniaProject-Java"
REPO_URL="https://github.com/BomberStealth/OmniaProject-be.git"
JENKINS_HOME="/var/lib/jenkins"

# ========================================
# STEP 1: SISTEMA BASE
# ========================================
print_step "Preparazione sistema..."
sudo apt update && sudo apt upgrade -y
print_ok "Sistema aggiornato"

# ========================================
# STEP 2: JAVA 17 (NECESSARIO PER JENKINS)
# ========================================
print_step "Installazione Java 17..."
sudo apt install -y openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64' >> ~/.bashrc
print_ok "Java 17 installato"

# ========================================
# STEP 3: INSTALLAZIONE JENKINS
# ========================================
print_step "Installazione Jenkins..."

# Aggiungi repository Jenkins
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install -y jenkins

print_ok "Jenkins installato"

# ========================================
# STEP 4: CONFIGURAZIONE JENKINS
# ========================================
print_step "Configurazione Jenkins..."

# Avvia Jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Aspetta che Jenkins si avvii
print_info "Attendo avvio Jenkins (può richiedere 2-3 minuti)..."
sleep 60

# Verifica Jenkins attivo
if sudo systemctl is-active --quiet jenkins; then
    print_ok "Jenkins attivo"
else
    print_error "Jenkins non si è avviato correttamente"
    exit 1
fi

# ========================================
# STEP 5: INSTALLAZIONE MAVEN E GIT
# ========================================
print_step "Installazione Maven e Git..."
sudo apt install -y maven git
print_ok "Maven e Git installati"

# ========================================
# STEP 6: CONFIGURAZIONE AUTOMATICA JENKINS
# ========================================
print_step "Configurazione automatica Jenkins..."

# Crea directory per script Jenkins
sudo mkdir -p /var/lib/jenkins/init.groovy.d

# Script per configurazione automatica Jenkins
sudo tee /var/lib/jenkins/init.groovy.d/basic-security.groovy > /dev/null <<'EOF'
#!groovy

import jenkins.model.*
import hudson.security.*
import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.CLI
import hudson.model.User

def instance = Jenkins.getInstance()

// Disabilita la sicurezza per il setup iniziale (cambieremo dopo)
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

// Abilita CSRF protection
instance.setCrumbIssuer(new DefaultCrumbIssuer(true))

// Disabilita CLI remoto per sicurezza
CLI.get().setEnabled(false)

// Crea utente admin di default
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("admin", "omnia2024")
instance.setSecurityRealm(hudsonRealm)

// Salva configurazione
instance.save()

println("Jenkins configurato con utente admin/omnia2024")
EOF

# ========================================
# STEP 7: PLUGIN JENKINS ESSENZIALI
# ========================================
print_step "Installazione plugin Jenkins..."

# Lista plugin essenziali
PLUGINS="git maven-plugin build-pipeline-plugin github github-oauth workflow-aggregator ws-cleanup"

# Script per installazione plugin
sudo tee /var/lib/jenkins/init.groovy.d/install-plugins.groovy > /dev/null <<EOF
#!groovy

import jenkins.model.Jenkins
import hudson.model.UpdateSite
import hudson.PluginWrapper

def plugins = [
    "git",
    "maven-plugin", 
    "build-pipeline-plugin",
    "github",
    "workflow-aggregator",
    "ws-cleanup"
]

def instance = Jenkins.getInstance()
def updateCenter = instance.getUpdateCenter()

plugins.each { pluginName ->
    if (!instance.pluginManager.getPlugin(pluginName)) {
        println("Installing plugin: \${pluginName}")
        def plugin = updateCenter.getPlugin(pluginName)
        if (plugin) {
            plugin.deploy(true)
        }
    }
}

instance.save()
EOF

print_ok "Script plugin configurato"

# ========================================
# STEP 8: RIAVVIO JENKINS
# ========================================
print_step "Riavvio Jenkins per applicare configurazioni..."
sudo systemctl restart jenkins

print_info "Attendo riavvio Jenkins..."
sleep 90

# ========================================
# STEP 9: SETUP PROGETTO JAVA
# ========================================
print_step "Setup progetto Java..."

# Clone progetto se non esiste
if [ ! -d "$PROJECT_DIR" ]; then
    git clone "$REPO_URL" "$PROJECT_DIR"
fi

cd "$PROJECT_DIR"

# Test build
mvn clean package -DskipTests -q
if [ $? -eq 0 ]; then
    print_ok "Progetto Java compilato correttamente"
else
    print_warn "Errore build Java (normale al primo avvio)"
fi

# ========================================
# STEP 10: JENKINS JOB CONFIGURATION
# ========================================
print_step "Creazione job Jenkins..."

# Job configuration XML
sudo mkdir -p /var/lib/jenkins/jobs/OmniaLED-Deploy

sudo tee /var/lib/jenkins/jobs/OmniaLED-Deploy/config.xml > /dev/null <<EOF
<?xml version='1.1' encoding='UTF-8'?>
<project>
  <actions/>
  <description>Auto-deploy Omnia LED Controller Java Backend</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <com.coravy.hudson.plugins.github.GithubProjectProperty plugin="github@1.37.3.1">
      <projectUrl>https://github.com/BomberStealth/OmniaProject-be/</projectUrl>
    </com.coravy.hudson.plugins.github.GithubProjectProperty>
  </properties>
  <scm class="hudson.plugins.git.GitSCM" plugin="git@4.8.3">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <url>https://github.com/BomberStealth/OmniaProject-be.git</url>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/main</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <submoduleCfg class="empty-list"/>
    <extensions/>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <triggers>
    <hudson.triggers.SCMTrigger>
      <spec>* * * * *</spec>
      <ignorePostCommitHooks>false</ignorePostCommitHooks>
    </hudson.triggers.SCMTrigger>
  </triggers>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.tasks.Shell>
      <command>#!/bin/bash

echo "🔄 JENKINS AUTO-DEPLOY AVVIATO"
echo "================================"

# Build Maven
echo "📦 Build Maven..."
mvn clean package -DskipTests

if [ \$? -eq 0 ]; then
    echo "✅ Build Maven completato"
else
    echo "❌ Build Maven fallito"
    exit 1
fi

# Ferma servizio precedente
echo "🛑 Fermo servizio precedente..."
sudo systemctl stop omnia-java-led 2>/dev/null || true

# Copia nuovo JAR
echo "📋 Deploy nuovo JAR..."
sudo cp target/raspberry-controller-1.0.0.jar /opt/omnia/ 2>/dev/null || {
    sudo mkdir -p /opt/omnia
    sudo cp target/raspberry-controller-1.0.0.jar /opt/omnia/
}

# Riavvia servizio
echo "🚀 Riavvio servizio..."
sudo systemctl start omnia-java-led

# Verifica
sleep 5
if sudo systemctl is-active --quiet omnia-java-led; then
    echo "✅ Servizio riavviato con successo"
    
    # Test API
    if curl -f http://localhost:3000/ >/dev/null 2>&1; then
        echo "✅ API risponde correttamente"
    else
        echo "⚠️  API non risponde (normale nei primi secondi)"
    fi
else
    echo "❌ Errore riavvio servizio"
    exit 1
fi

echo "🎉 DEPLOY COMPLETATO CON SUCCESSO"
</command>
    </hudson.tasks.Shell>
  </builders>
  <publishers/>
  <buildWrappers/>
</project>
EOF

sudo chown -R jenkins:jenkins /var/lib/jenkins/jobs/

print_ok "Job Jenkins creato"

# ========================================
# STEP 11: SERVIZIO SYSTEMD PER PROGETTO
# ========================================
print_step "Creazione servizio systemd finale..."

sudo mkdir -p /opt/omnia
sudo cp "$PROJECT_DIR/target/raspberry-controller-1.0.0.jar" /opt/omnia/ 2>/dev/null || true

sudo tee /etc/systemd/system/omnia-java-led.service > /dev/null <<EOF
[Unit]
Description=Omnia LED Controller Java Backend (Jenkins Managed)
After=network-online.target jenkins.service
Wants=network-online.target

[Service]
Type=simple
User=pi
Group=pi
WorkingDirectory=/opt/omnia
ExecStart=/usr/bin/java -jar raspberry-controller-1.0.0.jar
Restart=always
RestartSec=10

Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
Environment=SERVER_PORT=3000

StandardOutput=journal
StandardError=journal
SyslogIdentifier=omnia-java-led

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable omnia-java-led
sudo systemctl start omnia-java-led

print_ok "Servizio systemd creato e avviato"

# ========================================
# STEP 12: CONFIGURAZIONE FIREWALL
# ========================================
print_step "Configurazione accesso Jenkins..."

# Abilita porta Jenkins (8080)
sudo ufw allow 8080/tcp 2>/dev/null || true
sudo ufw allow 3000/tcp 2>/dev/null || true

print_ok "Porte configurate (8080 Jenkins, 3000 API)"

# ========================================
# STEP 13: VERIFICA FINALE
# ========================================
print_step "Verifica finale..."

# Check Jenkins
if sudo systemctl is-active --quiet jenkins; then
    print_ok "✅ Jenkins attivo"
else
    print_error "❌ Jenkins non attivo"
fi

# Check API
sleep 5
if curl -f http://localhost:3000/ > /dev/null 2>&1; then
    print_ok "✅ API Java attiva"
else
    print_warn "⚠️  API non ancora attiva"
fi

# Get IP
IP_ADDRESS=$(hostname -I | cut -d' ' -f1)

# ========================================
# STEP 14: INFORMAZIONI FINALI
# ========================================
echo ""
echo "🎉 JENKINS SETUP COMPLETATO!"
echo "============================"
echo ""
print_info "🌐 ACCESSO JENKINS:"
echo "  URL: http://$IP_ADDRESS:8080"
echo "  Username: admin"
echo "  Password: omnia2024"
echo ""
print_info "🚀 API BACKEND:"
echo "  URL: http://$IP_ADDRESS:3000"
echo ""
print_info "💼 JOB JENKINS:"
echo "  Nome: OmniaLED-Deploy"
echo "  Trigger: Ogni minuto (controlla GitHub)"
echo "  Deploy: Automatico su push"
echo ""
print_info "🔌 COLLEGAMENTO LED:"
echo "  Pin 12 (GPIO 18) → LED Anodo (+)"
echo "  LED Catodo (-)   → Resistenza 220Ω → Pin 6 (GND)"
echo ""
print_info "📝 WORKFLOW:"
echo "  1. Accedi a Jenkins: http://$IP_ADDRESS:8080"
echo "  2. Login: admin/omnia2024"
echo "  3. Il job 'OmniaLED-Deploy' controlla GitHub ogni minuto"
echo "  4. Ad ogni push, scarica e deploya automaticamente"
echo ""
echo "🎯 PROSSIMI PASSI:"
echo "  1. Apri Jenkins nel browser"
echo "  2. Vai su 'OmniaLED-Deploy' job"
echo "  3. Clicca 'Build Now' per primo test"
echo "  4. Controlla 'Console Output' per vedere i log"
echo ""
print_ok "Jenkins pronto! Interfaccia grafica completa! 🚀"