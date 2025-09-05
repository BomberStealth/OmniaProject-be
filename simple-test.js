// Test server semplice per verificare connettività
const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());

app.get('/', (req, res) => {
    res.json({ 
        message: 'Test Server Attivo',
        service: 'Node.js Test Server',
        timestamp: new Date().toISOString()
    });
});

app.get('/api/led/status', (req, res) => {
    res.json({ 
        isOn: false,
        message: 'LED spento (simulato)'
    });
});

app.post('/api/led/toggle', (req, res) => {
    res.json({ 
        isOn: true,
        message: 'LED toggle simulato',
        success: true
    });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Test server attivo su porta ${PORT}`);
    console.log('Questo è solo un test per verificare la connettività');
});