package com.example.client.model;

public class ConnectionInfo {
    private String connectionId;
    private String host;
    private int port;
    private boolean connected;

    public ConnectionInfo() {}

    public ConnectionInfo(String connectionId, String host, int port, boolean connected) {
        this.connectionId = connectionId;
        this.host = host;
        this.port = port;
        this.connected = connected;
    }

    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
}