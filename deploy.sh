#!/bin/bash

# Script di deployment automatico per Raspberry Pi
# Questo script deve essere eseguito sul Raspberry Pi

PROJECT_DIR="/home/pi/OmniaProject-be"
SERVICE_NAME="raspberry-controller"

echo "=== Avvio deployment automatico ==="

# Naviga nella directory del progetto
cd $PROJECT_DIR || exit 1

# Esegui git pull per ottenere le ultime modifiche
echo "Eseguo git pull..."
git pull origin main

# Controlla se ci sono stati cambiamenti
if [ $? -eq 0 ]; then
    echo "Git pull completato con successo"
else
    echo "Errore durante git pull"
    exit 1
fi

# Compila il progetto con Maven
echo "Compilazione del progetto..."
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "Compilazione completata con successo"
else
    echo "Errore durante la compilazione"
    exit 1
fi

# Ferma il servizio esistente (se in esecuzione)
echo "Arresto servizio esistente..."
sudo systemctl stop $SERVICE_NAME 2>/dev/null || echo "Servizio non in esecuzione"

# Aspetta un momento
sleep 2

# Avvia il nuovo servizio
echo "Avvio nuovo servizio..."
sudo systemctl start $SERVICE_NAME

# Controlla lo stato del servizio
sleep 3
if sudo systemctl is-active --quiet $SERVICE_NAME; then
    echo "✅ Deployment completato con successo!"
    echo "Servizio $SERVICE_NAME è in esecuzione"
else
    echo "❌ Errore: il servizio non si è avviato correttamente"
    sudo systemctl status $SERVICE_NAME
    exit 1
fi

echo "=== Deployment completato ==="