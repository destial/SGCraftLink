package xyz.destiall.sgcraftlink.modules;

import express.Express;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.JSONContentProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DataModule extends Module {
    private final Express express;
    private final HttpClient httpClient;

    public DataModule(SGCraftLinkSpigot plugin) {
        super(plugin, "comms.yml");
        express = new Express();
        httpClient = new HttpClient();
        express.post("/", (request, response) -> {
            JSONObject json = new JSONObject(request.ctx().body());
            plugin.getLogger().info("Received data " + json);

            if (!json.has("data-type")) {
                response.sendStatus(405);
                return;
            }

            String datatype = json.getString("data-type").toUpperCase();

            switch (datatype) {
                case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                    String command = json.getString("command");
                    plugin.executeServerCommand(command);
                    response.sendStatus(200);
                    break;
                }
                default: {
                    plugin.getLogger().warning("Received data " + datatype + " but have no implementation!");
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
        express.listen(config.getInt("port"));

        Map<String, Object> data = new HashMap<>();
        data.put("server", config.getString("server-name", "lobby"));
        data.put("port", config.getInt("port", 1034));
        tryRegisterServer(data);
    }

    private void tryRegisterServer(Map<String, Object> data) {
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_REGISTER, data, (result) -> {
            if (!result) {
                plugin.getScheduler().runTaskLaterAsync(() -> tryRegisterServer(data), 20L * 60L);
            }
        });
    }

    public boolean sendProxyData(String type, Map<?, ?> map, Consumer<Boolean> result) {
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);

        plugin.getScheduler().runTaskAsync(() -> {
            int status = 404;
            try {
                status = httpClient.newRequest("http://localhost").port(config.getInt("proxy-port", 1035)).method("POST").content(new JSONContentProvider(object)).send().getStatus();
                plugin.getLogger().info("Sending " + object + " to proxy");
            } catch (Exception e) {
                plugin.getLogger().severe(e.getMessage());
            }

            if (result != null) {
                if (status == 200) {
                    result.accept(true);
                    return;
                }
                result.accept(false);
            }
        });

        return true;
    }

    @Override
    public void loadDefaultConfig() {
        config.set("server-name", "lobby");
        config.set("port", 1034);
        config.set("proxy-port", 1035);
    }

    @Override
    public void disable() {
        Map<String, String> data = new HashMap<>();
        data.put("server", config.getString("server-name", "lobby"));
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_UNREGISTER, data, (result) -> {
            try {
                httpClient.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        express.stop();
    }
}
