package xyz.destiall.sgcraftlink.proxy.misc;

import express.Express;
import express.utils.Status;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import org.eclipse.jetty.client.api.ContentResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsManager;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public final class DataManager {
    private final PluginProvider plugin;
    private final Express express;
    private final ConfigProvider<?> config;
    private final HTTPManager manager;
    private final Map<String, Pair<Integer, String>> registeredServers;
    private final List<LuckPermsManager.ProxyRestMessenger> messengers;

    public DataManager(PluginProvider plugin) {
        this.plugin = plugin;
        express = new Express();
        manager = new HTTPManager();

        config = plugin.getConfigProvider().createConfig("api-config.yml");
        registeredServers = new HashMap<>();
        messengers = new ArrayList<>();

        int port = config.getConfig().getInt("listen-port", 1035);
        express.post("/", (request, response) -> {
            try {
                String body = SGCraftLinkCommon.convertStreamToString(request.getBody()).trim();
                plugin.getLoggerProvider().warning("Received data " + body);
                JSONObject json = new JSONObject(body);

                if (!json.has("data-type")) {
                    response.sendStatus(Status._405);
                    return;
                }

                String datatype = json.getString("data-type").toUpperCase();

                String server = json.getString("server");
                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
                    String auth = registeredServers.get(server).getValue();
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
                    case SGCraftLinkCommon.DATA_GET_HEADHUNT: {
                        AtomicInteger i = new AtomicInteger(registeredServers.size() - 1);
                        AtomicInteger remaining = new AtomicInteger(0);
                        JSONObject res = new JSONObject();
                        JSONObject score = new JSONObject();
                        JSONObject servers = new JSONObject();
                        for (String svr : registeredServers.keySet()) {
                            if (svr.equalsIgnoreCase(server))
                                continue;

                            sendData(SGCraftLinkCommon.DATA_GET_HEADHUNT, svr, new HashMap<>(), (status, content) -> {
                                if (status == 200) {
                                    JSONObject obj = new JSONObject(content);
                                    int r = obj.getInt("remaining");
                                    remaining.addAndGet(r);
                                    servers.put(svr, r);

                                    JSONObject players = obj.getJSONObject("score");
                                    for (String name : players.keySet()) {
                                        score.put(name, score.has(name) ? score.getInt(name) + players.getInt(name) : players.getInt(name));
                                    }
                                }
                                if (i.decrementAndGet() == 0) {
                                    res.put("servers", servers);
                                    res.put("total", remaining.get());
                                    res.put("score", score);
                                    plugin.getLoggerProvider().warning("Responding to " + server + " with " + res);
                                    response.setStatus(Status._200).send(res.toString());
                                }
                            });
                        }
                        break;
                    }
                    case SGCraftLinkCommon.DATA_START_HEADHUNT: {
                        for (String svr : registeredServers.keySet()) {
                            if (svr.equalsIgnoreCase(server))
                                continue;

                            sendData(SGCraftLinkCommon.DATA_START_HEADHUNT, svr, new HashMap<>(), (status, content) -> {});
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_END_HEADHUNT: {
                        for (String svr : registeredServers.keySet()) {
                            if (svr.equalsIgnoreCase(server))
                                continue;

                            sendData(SGCraftLinkCommon.DATA_END_HEADHUNT, svr, new HashMap<>(), (status, content) -> {});
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_CLEAR_SCORE_HEADHUNT: {
                        for (String svr : registeredServers.keySet()) {
                            if (svr.equalsIgnoreCase(server))
                                continue;

                            sendData(SGCraftLinkCommon.DATA_CLEAR_SCORE_HEADHUNT, svr, new HashMap<>(), (status, content) -> {});
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_REMOVE_HEAD_HEADHUNT: {
                        for (String svr : registeredServers.keySet()) {
                            if (svr.equalsIgnoreCase(server))
                                continue;

                            sendData(SGCraftLinkCommon.DATA_REMOVE_HEAD_HEADHUNT, svr, new HashMap<>(), (status, content) -> {});
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE: {
                        for (LuckPermsManager.ProxyRestMessenger messenger : messengers) {
                            messenger.handleIncomingMessage(json);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    default: {
                        plugin.getLoggerProvider().warning("Received data " + datatype + " but have no implementation!");
                        response.sendStatus(Status._201);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendStatus(Status._201);
            }
        });

        express.listen(port);
    }

    public List<LuckPermsManager.ProxyRestMessenger> getMessengers() {
        return messengers;
    }

    public CompletableFuture<Void> shutdown() {
        AtomicInteger i = new AtomicInteger(registeredServers.size());
        CompletableFuture<Void> future = new CompletableFuture<>();
        for (String server : registeredServers.keySet()) {
            sendData(SGCraftLinkCommon.DATA_SERVER_UNREGISTER, server, new HashMap<>(), (status, content) -> {
                if (i.decrementAndGet() == 0) {
                    try {
                        manager.shutdown();
                        registeredServers.clear();
                        express.stop();
                        future.complete(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return future;
    }

    public Collection<String> getServers() {
        return registeredServers.keySet();
    }

    public void sendDataToAll(String origin, String type, Map<?, ?> map) {
        int i = 0;
        for (String server : getServers()) {
            if (server.equalsIgnoreCase(origin))
                continue;

            sendData(type, server, map, (s, c) -> {});
        }
    }

    public boolean sendData(String type, String server, Map<?, ?> map, BiConsumer<Integer, String> result) {
        Pair<Integer, String> registration = registeredServers.get(server);
        if (registration == null) {
            return false;
        }
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);
        Map<String, String> headers = HTTPManager.headers("X-Authorization", registration.getValue());
        plugin.getScheduler().schedule(() -> {
            try {
                plugin.getLoggerProvider().warning("Sending " + type + " to " + server);
                CompletableFuture<ContentResponse> response = manager.fetch("http://" + config.getConfig().getString("ip", "192.168.1.99"), registration.getKey(), HTTPManager.Method.POST, headers, object);
                response.whenComplete((res, err) -> {
                    if (err != null) {
                        plugin.getLoggerProvider().warning("Responded back with 404");
                        plugin.getLoggerProvider().error(err.getLocalizedMessage());
                        result.accept(404, null);
                        return;
                    }
                    String content = res.getContentAsString().trim();
                    content = content.isEmpty() ? "{}" : content;
                    plugin.getLoggerProvider().warning("Responded back with " + res.getStatus());
                    int status = res.getStatus();
                    result.accept(status, content);
                });
            } catch (Exception e) {
               e.printStackTrace();
            }
            result.accept(404, null);
        }, 0L, TimeUnit.MILLISECONDS);


        return true;
    }

    public Express getExpress() {
        return express;
    }

    public PluginProvider getPlugin() {
        return plugin;
    }
}
