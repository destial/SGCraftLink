package xyz.destiall.sgcraftlink.proxy.misc;

import org.json.JSONObject;

public class RegisteredServer {
    private final int port;
    private final String token;
    private final String panelId;
    private String ip = "192.168.1.99";
    private long maxMemory = -1;
    private final boolean whitelisted;
    private final boolean announce;

    public RegisteredServer(JSONObject json) {
        port = json.getInt("port");
        token = json.getString("auth");
        String panelId = "null";
        if (json.has("panel_id"))
            panelId = json.getString("panel_id");

        whitelisted = json.has("whitelisted") && json.getBoolean("whitelisted");
        announce = json.has("announce") && json.getBoolean("announce");
        this.panelId = panelId;
        if (json.has("ip"))
            ip = json.getString("ip");
    }

    public boolean isAnnounce() {
        return announce;
    }

    public boolean isWhitelisted() {
        return whitelisted;
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
