package xyz.destiall.sgcraftlink.proxy.misc;

import express.Express;
import express.utils.Status;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.util.Collection;
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
            String body = SGCraftLinkCommon.convertStreamToString(request.getBody());
            plugin.getLoggerProvider().info("Received data " + body);
            JSONObject json = new JSONObject(body);

            if (!json.has("data-type")) {
                response.sendStatus(Status._405);
                return;
            }

            String datatype = json.getString("data-type").toUpperCase();
            switch (datatype) {
                case SGCraftLinkCommon.DATA_SERVER_REGISTER: {
                    String server = json.getString("server");
                    int serverport = json.getInt("port");
                    registerServer(server, serverport);
                    response.sendStatus(Status._200);
                    break;
                }
                case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                    String server = json.getString("server");
                    serverPorts.remove(server);
                    response.sendStatus(Status._200);
                    break;
                }
                case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                    String command = json.getString("command");
                    plugin.dispatchCommand(command);
                    response.sendStatus(Status._200);
                    break;
                }
                default: {
                    plugin.getLoggerProvider().warning("Received data " + datatype + " but have no implementation!");
                    response.sendStatus(Status._200);
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

    public Collection<String> getServers() {
        return serverPorts.keySet();
    }

    public void registerServer(String server, int port) {
        plugin.getLoggerProvider().info("Registered server " + server + " with port " + port);
        serverPorts.put(server, port);
    }

    public boolean sendData(String type, String server, Map<?, ?> map, Consumer<Pair<Integer, String>> result) {
        Integer port = serverPorts.get(server);
        if (port == null) {
            return false;
        }
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);

        plugin.getScheduler().schedule(() -> {
            int status = 404;
            String content = "";
            try {
                plugin.getLoggerProvider().info("Sending " + object + " to " + server);
                Request req = httpClient.newRequest("http://" + config.getConfig().getString("ip", "192.168.1.99")).port(port).method("POST").content(new JSONContentProvider(object));
                ContentResponse res = req.send();
                status = res.getStatus();
                content = res.getContentAsString();
                plugin.getLoggerProvider().info("Sent " + object + " to " + server);
            } catch (Exception e) {
                plugin.getLoggerProvider().error(e.getMessage());
            }

            if (result != null) {
                if (status == 200) {
                    result.accept(new Pair<>(status, content));
                    return;
                }
                result.accept(new Pair<>(status, null));
            }
        }, 0L, TimeUnit.MILLISECONDS);

        return true;
    }

    public Express getExpress() {
        return express;
    }
}
