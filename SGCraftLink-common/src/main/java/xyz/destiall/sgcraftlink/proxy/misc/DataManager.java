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
import java.util.function.Consumer;

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

        int port = config.getConfig().getInt("listen-port", 1035);

        express.post("/", (request, response) -> {
            JSONObject json = new JSONObject(request.ctx().body());
            plugin.getLoggerProvider().info("Received data " + json);

            if (!json.has("data-type")) {
                response.sendStatus(405);
                return;
            }

            String datatype = json.getString("data-type").toUpperCase();
            switch (datatype) {
                case SGCraftLinkCommon.DATA_SERVER_REGISTER: {
                    String server = json.getString("server");
                    int serverport = json.getInt("port");
                    registerServer(server, serverport);
                    response.sendStatus(200);
                    break;
                }
                case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                    String server = json.getString("server");
                    serverPorts.remove(server);
                    response.sendStatus(200);
                    break;
                }
                case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                    String command = json.getString("command");
                    plugin.dispatchCommand(command);
                    response.sendStatus(200);
                    break;
                }
                default: {
                    plugin.getLoggerProvider().warning("Received data " + datatype + " but have no implementation!");
                    response.sendStatus(200);
                    break;
                }
            }
        });

        try {
            httpClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        express.listen(port);
    }

    public void shutdown() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverPorts.clear();

        express.stop();
    }

    public void registerServer(String server, int port) {
        serverPorts.put(server, port);
    }

    public boolean sendData(String type, String server, Map<?, ?> map, Consumer<Boolean> result) {
        Integer port = serverPorts.get(server);
        if (port == null) {
            return false;
        }
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);

        plugin.getScheduler().schedule(() -> {
            int status = 404;
            try {
                status = httpClient.newRequest("http://localhost").port(port).method("POST").content(new JSONContentProvider(object)).send().getStatus();
                plugin.getLoggerProvider().info("Sending " + object + " to " + server);
            } catch (Exception e) {
                plugin.getLoggerProvider().error(e.getMessage());
            }

            if (result != null) {
                if (status == 200) {
                    result.accept(true);
                    return;
                }
                result.accept(false);
            }
        }, 0L, TimeUnit.MILLISECONDS);

        return true;
    }

    public Express getExpress() {
        return express;
    }
}
