package xyz.destiall.sgcraftlink.proxy.misc;

import express.Express;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class DataManager {
    private final PluginProvider plugin;
    private final Express express;
    private final ConfigProvider<?> config;
    private final HttpClient httpClient;
    private final Map<String, Integer> serverPorts;

    public DataManager(PluginProvider plugin) {
        this.plugin = plugin;
        express = new Express();
        httpClient = new HttpClient();
        config = plugin.getConfigProvider().createConfig("api-config.yml");
        serverPorts = new HashMap<>();

        int port = config.getConfig().getInt("listen-port", 1015);

        express.get("/player", (request, response) -> {
            JSONObject body = new JSONObject(request.body());
            String type = body.getString("data-type");

            int status = 200;
            switch (type) {
                case SGCraftLinkCommon.DATA_BIRTHDAY: {
                    long date = body.getLong("birthday");
                    break;
                }

                default: {
                    status = 500;
                    break;
                }
            }

            response.sendStatus(status);
        });

        express.listen(port);
    }

    public void registerServer(String server, int port) {
        serverPorts.put(server, port);
    }

    public boolean sendData(String type, String server, Map<Object, Object> map) {
        Integer port = serverPorts.get(server);
        if (port == null) {
            return false;
        }
        JSONObject object = new JSONObject(map);

        plugin.getScheduler().schedule(() -> {
            try {
                httpClient.newRequest("http://localhost").port(port).method("POST").content(new JSONContentProvider(object)).send();
                plugin.getLoggerProvider().info("Sending data to " + server);
            } catch (Exception e) {
                plugin.getLoggerProvider().error(e.getMessage());
            }
        }, 0L, TimeUnit.MILLISECONDS);

        return true;
    }

    public Express getExpress() {
        return express;
    }
}
