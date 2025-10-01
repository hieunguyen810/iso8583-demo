const API_URL = () => document.getElementById('apiUrl').value;

function getTimestamp() {
    const now = new Date();
    return now.toLocaleTimeString() + '.' + now.getMilliseconds().toString().padStart(3, '0');
}

function logToConsole(message, type = 'info') {
    const console = document.getElementById('consoleOutput');
    const line = document.createElement('div');
    line.className = `console-line ${type}`;
    line.innerHTML = `<span class="timestamp">[${getTimestamp()}]</span>${message}`;
    console.appendChild(line);
    console.scrollTop = console.scrollHeight;
}

function clearConsole() {
    document.getElementById('consoleOutput').innerHTML = '<div class="console-line info">Console cleared.</div>';
}

async function addConnection() {
    const connectionId = document.getElementById('connectionId').value.trim();
    const host = document.getElementById('host').value.trim();
    const port = document.getElementById('port').value;

    if (!connectionId || !host || !port) {
        logToConsole('❌ Please fill in all fields', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_URL()}/connections`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ connectionId, host, port: parseInt(port) })
        });

        const data = await response.json();
        
        if (data.success) {
            logToConsole(`✅ Connection '${connectionId}' added successfully`, 'success');
            document.getElementById('connectionId').value = '';
            document.getElementById('host').value = '';
            document.getElementById('port').value = '';
            refreshConnections();
        } else {
            logToConsole(`❌ Failed to add connection: ${data.message}`, 'error');
        }
    } catch (error) {
        logToConsole(`❌ Error: ${error.message}`, 'error');
    }
}

async function refreshConnections() {
    try {
        const response = await fetch(`${API_URL()}/connections`);
        const connections = await response.json();
        
        const grid = document.getElementById('connectionsGrid');
        const select = document.getElementById('targetConnection');
        
        if (connections.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: #666; padding: 20px;">No connections yet. Add one above.</div>';
            select.innerHTML = '<option value="">Select a connection</option>';
            return;
        }

        grid.innerHTML = '';
        select.innerHTML = '<option value="">Select a connection</option>';

        connections.forEach(conn => {
            const card = createConnectionCard(conn);
            grid.appendChild(card);
            
            const option = document.createElement('option');
            option.value = conn.connectionId;
            option.textContent = `${conn.connectionId} (${conn.host}:${conn.port})`;
            select.appendChild(option);
        });

        // logToConsole(`📊 Loaded ${connections.length} connection(s)`, 'info');
    } catch (error) {
        logToConsole(`❌ Error refreshing connections: ${error.message}`, 'error');
    }
}

function createConnectionCard(conn) {
    const card = document.createElement('div');
    card.className = `connection-card ${conn.connected ? 'active' : ''}`;
    
    const statusClass = conn.connected ? 'status-connected' : 'status-disconnected';
    const statusText = conn.connected ? 'Connected' : 'Disconnected';
    
    card.innerHTML = `
        <div class="connection-header">
            <div class="connection-id">${conn.connectionId}</div>
            <div class="status-badge ${statusClass}">${statusText}</div>
        </div>
        <div class="connection-details">
            📍 ${conn.host}:${conn.port}
        </div>
        <div class="connection-actions">
            <button onclick="connectTo('${conn.connectionId}')" ${conn.connected ? 'disabled' : ''}>
                Connect
            </button>
            <button onclick="disconnectFrom('${conn.connectionId}')" ${!conn.connected ? 'disabled' : ''}>
                Disconnect
            </button>
            <button class="danger" onclick="removeConnection('${conn.connectionId}')">
                Remove
            </button>
        </div>
    `;
    
    return card;
}

async function connectTo(connectionId) {
    try {
        logToConsole(`🔄 Connecting to ${connectionId}...`, 'info');
        const response = await fetch(`${API_URL()}/connections/${connectionId}/connect`, {
            method: 'POST'
        });

        const data = await response.json();
        
        if (data.success) {
            logToConsole(`✅ Connected to ${connectionId}`, 'success');
            refreshConnections();
        } else {
            logToConsole(`❌ Connection failed: ${data.message}`, 'error');
        }
    } catch (error) {
        logToConsole(`❌ Error: ${error.message}`, 'error');
    }
}

async function disconnectFrom(connectionId) {
    try {
        logToConsole(`🔄 Disconnecting from ${connectionId}...`, 'info');
        const response = await fetch(`${API_URL()}/connections/${connectionId}/disconnect`, {
            method: 'POST'
        });

        const data = await response.json();
        
        if (data.success) {
            logToConsole(`✅ Disconnected from ${connectionId}`, 'success');
            refreshConnections();
        } else {
            logToConsole(`❌ Disconnect failed: ${data.message}`, 'error');
        }
    } catch (error) {
        logToConsole(`❌ Error: ${error.message}`, 'error');
    }
}

async function removeConnection(connectionId) {
    if (!confirm(`Are you sure you want to remove connection '${connectionId}'?`)) {
        return;
    }

    try {
        logToConsole(`🔄 Removing connection ${connectionId}...`, 'info');
        const response = await fetch(`${API_URL()}/connections/${connectionId}`, {
            method: 'DELETE'
        });

        const data = await response.json();
        
        if (data.success) {
            logToConsole(`✅ Connection ${connectionId} removed`, 'success');
            refreshConnections();
        } else {
            logToConsole(`❌ Remove failed: ${data.message}`, 'error');
        }
    } catch (error) {
        logToConsole(`❌ Error: ${error.message}`, 'error');
    }
}

async function sendEcho() {
    const connectionId = document.getElementById('targetConnection').value;
    
    if (!connectionId) {
        logToConsole('❌ Please select a connection', 'error');
        return;
    }

    try {
        logToConsole(`📤 Sending echo to ${connectionId}...`, 'info');
        const response = await fetch(`${API_URL()}/connections/${connectionId}/echo`, {
            method: 'POST'
        });

        const data = await response.json();
        
        if (data.success) {
            logToConsole(`✅ Echo sent successfully`, 'success');
            logToConsole(`📨 Request: ${data.request}`, 'request');
            logToConsole(`📬 Response: ${data.response}`, 'response');
        } else {
            logToConsole(`❌ Echo failed: ${data.message}`, 'error');
        }
    } catch (error) {
        logToConsole(`❌ Error: ${error.message}`, 'error');
    }
}

async function sendMessage() {
    const connectionId = document.getElementById('targetConnection').value;
    const message = document.getElementById('messageContent').value.trim();
    
    if (!connectionId) {
        logToConsole('❌ Please select a connection', 'error');
        return;
    }

    if (!message) {
        logToConsole('❌ Please enter a message', 'error');
        return;
    }

    try {
        logToConsole(`📤 Sending message to ${connectionId}...`, 'info');
        const response = await fetch(`${API_URL()}/connections/${connectionId}/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message })
        });

        const data = await response.json();
        
        if (data.success) {
            logToConsole(`✅ Message sent successfully`, 'success');
            logToConsole(`📨 Request: ${data.request}`, 'request');
            logToConsole(`📬 Response: ${data.response}`, 'response');
        } else {
            logToConsole(`❌ Send failed: ${data.message}`, 'error');
        }
    } catch (error) {
        logToConsole(`❌ Error: ${error.message}`, 'error');
    }
}

// Auto-refresh connections every 5 seconds
setInterval(refreshConnections, 5000);

// Initial load
refreshConnections();