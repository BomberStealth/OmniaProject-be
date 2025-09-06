#!/bin/bash

# Script per deploy automatico del backend su Raspberry Pi
# Da eseguire dal Raspberry Pi nella directory OmniaProject-be

echo "🚀 OMNIA PROJECT - Deploy Backend Definitivo"
echo "============================================="

# Ferma il container di test esistente
echo "⏹️  Fermando container di test..."
sudo docker stop omniaproject-be || echo "Container non trovato"
sudo docker rm omniaproject-be || echo "Container non trovato"

# Rimuovi immagine precedente
echo "🗑️  Rimuovendo immagine precedente..."
sudo docker rmi omnia-backend:latest || echo "Immagine non trovata"

# Builda la nuova immagine
echo "🔨 Buildando nuova immagine Docker..."
sudo docker build -t omnia-backend:latest .

if [ $? -ne 0 ]; then
    echo "❌ Errore durante la build dell'immagine"
    exit 1
fi

# Avvia il nuovo container con privilegi GPIO
echo "🔧 Avviando nuovo container..."
sudo docker run -d \
  --name omniaproject-be \
  --restart unless-stopped \
  --privileged \
  -p 3000:3000 \
  -v /dev:/dev \
  omnia-backend:latest

if [ $? -eq 0 ]; then
    echo "✅ Deploy completato con successo!"
    echo "🌐 Backend disponibile su: http://192.168.1.100:3000"
    echo "🔗 API LED: http://192.168.1.100:3000/api/led/toggle"
    
    # Attendi qualche secondo per l'avvio
    sleep 5
    
    # Testa il servizio
    echo "🧪 Test del servizio..."
    curl -s http://localhost:3000/ | grep -q "Server LED Strip" && echo "✅ Servizio risponde correttamente" || echo "❌ Servizio non risponde"
    
    # Mostra i log
    echo "📋 Log container:"
    sudo docker logs omniaproject-be --tail 20
else
    echo "❌ Errore durante l'avvio del container"
    exit 1
fi