package xyz.destiall.sgcraftlink.proxy.misc;

public class RegisteredServer {
    private final int port;
    private final String token;
    private final String panelId;
    private String ip = "192.168.1.99";
    private long maxMemory = -1;

    public RegisteredServer(int port, String token, String panelId) {
        this.port = port;
        this.token = token;
        this.panelId = panelId;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }

    public String getIp() {
        return ip;
    }
}
