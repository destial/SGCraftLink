package xyz.destiall.sgcraftlink.proxy.misc;

public class RegisteredServer {
    private final int port;
    private final String token;
    private String ip = "192.168.1.99";

    public RegisteredServer(int port, String token) {
        this.port = port;
        this.token = token;
    }

    public RegisteredServer(int port, String token, String ip) {
        this.port = port;
        this.token = token;
        this.ip = ip;
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
