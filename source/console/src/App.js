import React, { useState, useEffect } from 'react';

const API_URL = window.APP_CONFIG?.API_BASE_URL || 'http://localhost:8081/api/iso8583';

function App() {
  const [connections, setConnections] = useState([]);
  const [connectionForm, setConnectionForm] = useState({ connectionId: '', host: '', port: '' });
  const [selectedConnection, setSelectedConnection] = useState('');
  const [messageContent, setMessageContent] = useState('');
  const [showSamples, setShowSamples] = useState(false);
  const [consoleOutput, setConsoleOutput] = useState([{ message: 'Console ready. Waiting for actions...', type: 'info', timestamp: new Date() }]);

  const sampleMessages = {
    '0100': 'MTI=0100|F2=4000123456789012|F3=000000|F4=000000001000|F7=0101120000|F11=000001|F12=120000|F13=0101|F18=5999|F22=012|F25=00|F37=000000000001|F41=TERM001 |F42=MERCHANT001    |F49=840',
    '0200': 'MTI=0200|F2=4000123456789012|F3=000000|F4=000000001000|F7=0101120000|F11=000001|F12=120000|F13=0101|F18=5999|F22=012|F25=00|F37=000000000001|F41=TERM001 |F42=MERCHANT001    |F49=840',
    '0800': 'MTI=0800|F7=0101120000|F11=000001|F12=120000|F13=0101|F70=301',
    '0900': 'MTI=0900|F7=0101120000|F11=000001|F12=120000|F13=0101|F70=301'
  };

  const logToConsole = (message, type = 'info') => {
    setConsoleOutput(prev => [...prev, { message, type, timestamp: new Date() }]);
  };

  const clearConsole = () => {
    setConsoleOutput([{ message: 'Console cleared.', type: 'info', timestamp: new Date() }]);
  };

  const refreshConnections = async () => {
    try {
      const response = await fetch(`${API_URL}/connections`);
      const data = await response.json();
      setConnections(data);
    } catch (error) {
      logToConsole(`❌ : ${error.message}`, 'error');
    }
  };

  const addConnection = async () => {
    const { connectionId, host, port } = connectionForm;
    
    if (!connectionId || !host || !port) {
      logToConsole('❌ Please fill in all fields', 'error');
      return;
    }

    try {
      const response = await fetch(`${API_URL}/connections`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ connectionId, host, port: parseInt(port) })
      });

      const data = await response.json();
      
      if (data.success) {
        logToConsole(`✅ Connection '${connectionId}' added successfully`, 'success');
        setConnectionForm({ connectionId: '', host: '', port: '' });
        refreshConnections();
      } else {
        logToConsole(`❌ Failed to add connection: ${data.message}`, 'error');
      }
    } catch (error) {
      logToConsole(`❌ Error: ${error.message}`, 'error');
    }
  };

  const connectTo = async (connectionId) => {
    try {
      logToConsole(`🔄 Connecting to ${connectionId}...`, 'info');
      const response = await fetch(`${API_URL}/connections/${connectionId}/connect`, {
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
  };

  const disconnectFrom = async (connectionId) => {
    try {
      logToConsole(`🔄 Disconnecting from ${connectionId}...`, 'info');
      const response = await fetch(`${API_URL}/connections/${connectionId}/disconnect`, {
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
  };

  const removeConnection = async (connectionId) => {
    if (!window.confirm(`Are you sure you want to remove connection '${connectionId}'?`)) {
      return;
    }

    try {
      logToConsole(`🔄 Removing connection ${connectionId}...`, 'info');
      const response = await fetch(`${API_URL}/connections/${connectionId}`, {
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
  };

  const sendEcho = async () => {
    if (!selectedConnection) {
      logToConsole('❌ Please select a connection', 'error');
      return;
    }

    try {
      logToConsole(`📤 Sending echo to ${selectedConnection}...`, 'info');
      const response = await fetch(`${API_URL}/connections/${selectedConnection}/echo`, {
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
  };

  const sendMessage = async () => {
    if (!selectedConnection) {
      logToConsole('❌ Please select a connection', 'error');
      return;
    }

    if (!messageContent.trim()) {
      logToConsole('❌ Please enter a message', 'error');
      return;
    }

    try {
      logToConsole(`📤 Sending message to ${selectedConnection}...`, 'info');
      const response = await fetch(`${API_URL}/connections/${selectedConnection}/send`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: messageContent })
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
  };

  useEffect(() => {
    refreshConnections();
    const interval = setInterval(refreshConnections, 10000);
    return () => clearInterval(interval);
  }, []);

  const formatTimestamp = (date) => {
    return date.toLocaleTimeString() + '.' + date.getMilliseconds().toString().padStart(3, '0');
  };

  return (
    <div className="container">
      <h1>🔌 ISO 8583 Remote Console</h1>

      <div className="two-column">
        <div>
          <div className="panel">
            <div className="panel-title">➕ Add New Connection</div>
            <div className="form-group">
              <label>Connection ID</label>
              <input
                type="text"
                value={connectionForm.connectionId}
                onChange={(e) => setConnectionForm({...connectionForm, connectionId: e.target.value})}
                placeholder="server1"
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Host</label>
                <input
                  type="text"
                  value={connectionForm.host}
                  onChange={(e) => setConnectionForm({...connectionForm, host: e.target.value})}
                  placeholder="192.168.1.100"
                />
              </div>
              <div className="form-group">
                <label>Port</label>
                <input
                  type="number"
                  value={connectionForm.port}
                  onChange={(e) => setConnectionForm({...connectionForm, port: e.target.value})}
                  placeholder="5000"
                />
              </div>
            </div>
            <button onClick={addConnection}>Add Connection</button>
            <button onClick={refreshConnections}>Refresh List</button>
          </div>

          <div className="panel">
            <div className="panel-title">📡 Active Connections</div>
            <div className="connections-grid">
              {connections.length === 0 ? (
                <div style={{gridColumn: '1/-1', textAlign: 'center', color: '#666', padding: '20px'}}>
                  No connections yet. Add one above.
                </div>
              ) : (
                connections.map(conn => (
                  <div key={conn.connectionId} className={`connection-card ${conn.connected ? 'active' : ''}`}>
                    <div className="connection-header">
                      <div className="connection-id">{conn.connectionId}</div>
                      <div className={`status-badge ${conn.connected ? 'status-connected' : 'status-disconnected'}`}>
                        {conn.connected ? 'Connected' : 'Disconnected'}
                      </div>
                    </div>
                    <div className="connection-details">
                      📍 {conn.host}:{conn.port}
                    </div>
                    <div className="connection-actions">
                      <button onClick={() => connectTo(conn.connectionId)} disabled={conn.connected}>
                        Connect
                      </button>
                      <button onClick={() => disconnectFrom(conn.connectionId)} disabled={!conn.connected}>
                        Disconnect
                      </button>
                      <button className="danger" onClick={() => removeConnection(conn.connectionId)}>
                        Remove
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        <div>
          <div className="panel">
            <div className="panel-title">📤 Send Message</div>
            <div className="form-group">
              <label>Target Connection</label>
              <select value={selectedConnection} onChange={(e) => setSelectedConnection(e.target.value)}>
                <option value="">Select a connection</option>
                {connections.map(conn => (
                  <option key={conn.connectionId} value={conn.connectionId}>
                    {conn.connectionId} ({conn.host}:{conn.port})
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Message Content</label>
              <textarea
                value={messageContent}
                onChange={(e) => setMessageContent(e.target.value)}
                placeholder="Enter ISO 8583 message..."
              />
            </div>
            <button className="success" onClick={sendMessage}>Send Message</button>
            <button onClick={sendEcho}>Send Echo</button>
            <button onClick={() => setShowSamples(!showSamples)}>Sample Messages</button>
            <button onClick={clearConsole}>Clear Console</button>
            {showSamples && (
              <div className="sample-messages">
                <div className="sample-title">📋 Sample ISO 8583 Messages</div>
                {Object.entries(sampleMessages).map(([mti, message]) => (
                  <div key={mti} className="sample-item">
                    <span className="sample-mti">{mti}</span>
                    <button className="sample-btn" onClick={() => setMessageContent(message)}>Use</button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="panel">
            <div className="panel-title">📋 Console Output</div>
            <div className="console-output">
              {consoleOutput.map((log, index) => (
                <div key={index} className={`console-line ${log.type}`}>
                  <span className="timestamp">[{formatTimestamp(log.timestamp)}]</span>
                  {log.message}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;