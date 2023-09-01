package xyz.destiall.sgcraftlink.proxy.misc;

import express.Express;
import express.utils.Status;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.json.JSONArray;
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
    private final Map<String, Pair<Integer, String>> registeredServers;

    public DataManager(PluginProvider plugin) {
        this.plugin = plugin;
        express = new Express();
        httpClient = new HttpClient();

        config = plugin.getConfigProvider().createConfig("api-config.yml");
        registeredServers = new HashMap<>();

        int port = config.getConfig().getInt("listen-port", 1035);
        express.post("/", (request, response) -> {
            try {
                String body = SGCraftLinkCommon.convertStreamToString(request.getBody());
                plugin.getLoggerProvider().info("Received data " + body);
                JSONObject json = new JSONObject(body);

                if (!json.has("data-type")) {
                    response.sendStatus(Status._405);
                    return;
                }

                String datatype = json.getString("data-type").toUpperCase();

                String server = json.getString("server");
                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
                    String auth = registeredServers.get(server).getValue();
                    plugin.getLoggerProvider().info("Headers: [" + String.join(",", request.getHeader("X-Authorization")) + "]");
                    if (!request.getHeader("X-Authorization").contains(auth)) {
                        response.sendStatus(Status._403);
                        return;
                    }
                }

                switch (datatype) {
                    case SGCraftLinkCommon.DATA_SERVER_REGISTER: {
                        int serverport = json.getInt("port");
                        String auth = json.getString("auth");
                        plugin.getLoggerProvider().info("Registered server " + server + " with port " + serverport);
                        registeredServers.put(server, new Pair<>(serverport, auth));
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                        plugin.getLoggerProvider().info("Unregistered server " + server);
                        registeredServers.remove(server);
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                        JSONArray commands = json.getJSONArray("commands");
                        for (int i = 0; i < commands.length(); i++) {
                            String command = commands.getString(i);
                            plugin.dispatchCommand(command);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    default: {
                        plugin.getLoggerProvider().warning("Received data " + datatype + " but have no implementation!");
                        response.sendStatus(Status._200);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        registeredServers.clear();

        express.stop();
    }

    public Collection<String> getServers() {
        return registeredServers.keySet();
    }

    public boolean sendData(String type, String server, Map<?, ?> map, Consumer<Pair<Integer, String>> result) {
        Pair<Integer, String> registration = registeredServers.get(server);
        if (registration == null) {
            return false;
        }
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);

        plugin.getScheduler().schedule(() -> {
            int status = 404;
            String content = "";
            try {
                plugin.getLoggerProvider().info("Sending " + object + " to " + server);
                Request req = httpClient.newRequest("http://" + config.getConfig().getString("ip", "192.168.1.99")).port(registration.getKey()).method("POST").content(new JSONContentProvider(object));
                req.header("X-Authorization", registration.getValue());
                ContentResponse res = req.send();
                status = res.getStatus();
                content = res.getContentAsString();
                plugin.getLoggerProvider().info("Sent " + object + " to " + server);
            } catch (Exception e) {
                plugin.getLoggerProvider().error(e.getMessage());
            }

            plugin.getLoggerProvider().info("Received status " + status);

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
