package xyz.destiall.sgcraftlink.modules;

import express.Express;
import express.utils.MediaType;
import express.utils.Status;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.hooks.CoreProtectHook;
import xyz.destiall.sgcraftlink.items.JSONContentProvider;
import xyz.destiall.sgcraftlink.items.Pair;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DataModule extends Module {
    private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

    public static String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }


    private final Express express;
    private final HttpClient httpClient;

    public DataModule(SGCraftLinkSpigot plugin) {
        super(plugin, "comms.yml");
        express = new Express();
        httpClient = new HttpClient();
        express.post("/", (request, response) -> {
            try {
                String body = SGCraftLinkCommon.convertStreamToString(request.getBody());
                plugin.getLogger().info("Received data " + body);
                JSONObject json = new JSONObject(body);

                if (!json.has("data-type")) {
                    plugin.getLogger().warning("Request does not have any data-type!");
                    response.sendStatus(Status._405);
                    return;
                }

                String datatype = json.getString("data-type").toUpperCase();

                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
                    String auth = config.getString("authentication-key");
                    List<String> headers = request.getHeader("X-Authorization");
                    if (headers.isEmpty() || !headers.get(0).equals(auth)) {
                        response.sendStatus(Status._403);
                        return;
                    }
                }

                switch (datatype) {
                    case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                        JSONArray commands = json.getJSONArray("commands");
                        for (int i = 0; i < commands.length(); i++) {
                            String command = commands.getString(i);
                            plugin.dispatchCommand(command);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_CHATS: {
                        String player = json.getString("player");
                        int limit = json.getInt("limit");

                        CoreProtectHook hook = plugin.getModule(CoreProtectHook.class);
                        JSONArray array = new JSONArray();
                        JSONObject res = new JSONObject();
                        if (hook == null) {
                            res.put("messages", array);
                            plugin.getLogger().warning("CoreProtect Hook not found!");
                            plugin.getLogger().info("Sending back to proxy");
                            response.setContentType(MediaType._str);
                            response.setStatus(Status._200).send(res.toString());
                            break;
                        }

                        List<Pair<Long, String>> messages = hook.getChats(player, limit);
                        if (messages == null) {
                            res.put("messages", array);
                            plugin.getLogger().warning("No messages found!");
                            plugin.getLogger().info("Sending back to proxy");
                            response.setContentType(MediaType._str);
                            response.setStatus(Status._200).send(res.toString());
                            break;
                        }

                        for (int i = 0; i < messages.size() && i < limit; i++) {
                            Pair<Long, String> m = messages.get(i);
                            JSONObject obj = new JSONObject();
                            obj.put("time", m.getKey());
                            obj.put("msg", m.getValue());
                            array.put(obj);
                        }

                        res.put("messages", array);
                        plugin.getLogger().info("Sending back " + array.length() + " messages to proxy");
                        response.setContentType(MediaType._str);
                        response.setStatus(Status._200).send(res.toString());
                        break;
                    }
                    default: {
                        plugin.getLogger().warning("Received data " + datatype + " but have no implementation!");
                        response.sendStatus(Status._201);
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
        express.listen(config.getInt("port"));

        String authToken;
        if (config.contains("authentication-key")) {
            authToken = config.getString("authentication-key");
        } else {
            authToken = generateNewToken();
            config.set("authentication-key", authToken);
            save();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("port", config.getInt("port", 1034));
        data.put("auth", authToken);
        tryRegisterServer(data);
    }

    private void tryRegisterServer(Map<String, Object> data) {
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_REGISTER, data, (result) -> {
            if (result.getKey() != 200) {
                plugin.getLogger().warning("Failed! Trying to register server with proxy again...");
                plugin.getScheduler().runTaskLaterAsync(() -> tryRegisterServer(data), 20L * 60L);
                return;
            }
            plugin.getLogger().info("Registered server with proxy");
        });
    }

    public void sendProxyData(String type, Map<?, ?> map, Consumer<Pair<Integer, String>> result) {
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);
        object.put("server", config.getString("server-name", "lobby"));

        plugin.getScheduler().runTaskAsync(() -> {
            int status = 404;
            String content = null;
            try {
                plugin.getLogger().info("Sending " + object + " to proxy");
                Request req = httpClient.newRequest("http://" + config.getString("proxy-ip", "192.168.1.99")).port(config.getInt("proxy-port", 1035)).method("POST").content(new JSONContentProvider(object));
                req.header("X-Authorization", config.getString("authentication-key"));
                ContentResponse res = req.send();
                status = res.getStatus();
                content = res.getContentAsString();
                plugin.getLogger().info("Sent to proxy");
            } catch (Exception e) {
                plugin.getLogger().severe(e.getMessage());
            }

            plugin.getLogger().info("Received status " + status);

            if (result != null) {
                if (status == 200) {
                    result.accept(new Pair<>(status, content));
                    return;
                }
                result.accept(new Pair<>(status, null));
            }
        });
    }

    @Override
    public void loadDefaultConfig() {
        config.set("server-name", "lobby");
        config.set("port", 1034);
        config.set("proxy-port", 1035);
        config.set("proxy-ip", "192.168.1.99");
        config.set("authentication-key", generateNewToken());
    }

    @Override
    public void disable() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        express.stop();
    }
}
