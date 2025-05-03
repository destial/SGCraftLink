package xyz.destiall.sgcraftlink.proxy.misc;

import express.Express;
import express.utils.Status;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.ConfigSectionProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.Punishment;
import xyz.destiall.sgcraftlink.proxy.litebans.PunishmentType;
import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsManager;
import xyz.destiall.sgcraftlink.utils.DataResult;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.convertStreamToString;

public final class DataManager {
    private static DataManager instance;
    private final PluginProvider plugin;
    private final Express express;
    private final HTTPManager manager;
    private final PanelManager panelManager;
    private final Map<String, RegisteredServer> registeredServers;
    private final List<LuckPermsManager.ProxyRestMessenger> messengers;

    public static DataManager get() {
        return instance;
    }

    public DataManager(PluginProvider plugin) {
        this.plugin = plugin;
        instance = this;
        express = new Express();
        manager = new HTTPManager();
        panelManager = new PanelManager(plugin, manager);
        ConfigProvider<?> config = plugin.getConfigProvider().createConfig("api-config.yml");
        registeredServers = new HashMap<>();
        messengers = new ArrayList<>();

        int port = config.getConfig().getInt("listen-port", 1035);
        express.post("/", (request, response) -> {
            try {
                String body = convertStreamToString(request.getBody()).trim();
                plugin.getLoggerProvider().warning("Received data " + body);
                JSONObject json = new JSONObject(body);
                if (!json.has("data-type")) {
                    response.sendStatus(Status._405);
                    return;
                }
                String datatype = json.getString("data-type");
                String server = json.getString("server");
                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER) || registeredServers.containsKey(server)) {
                    RegisteredServer registeredServer = registeredServers.get(server);
                    if (registeredServer == null) {
                        response.sendStatus(Status._403);
                        return;
                    }

                    String auth = registeredServer.getToken();
                    if (!request.getHeader("X-Authorization").contains(auth)) {
                        response.sendStatus(Status._403);
                        return;
                    }
                }

                String serverName = server.substring(0, 1).toUpperCase() + server.substring(1);
                switch (datatype) {
                    case SGCraftLinkCommon.DATA_SERVER_REGISTER: {
                        RegisteredServer registration = new RegisteredServer(json);
                        if (registeredServers.containsKey(server)) {
                            RegisteredServer existing = registeredServers.get(server);
                            if (!existing.getToken().equalsIgnoreCase(registration.getToken())) {
                                response.sendStatus(Status._403);
                                break;
                            }
                        }
                        registeredServers.put(server, registration);
                        plugin.getLoggerProvider().info("Registered server " + server + " to " + registration.getIp() + ":" + registration.getPort());
                        plugin.getDiscordProvider().getDiscordCommands().addPanelServer(server);
                        response.sendStatus(Status._200);
                        panelManager.getServerDetails(registration).whenComplete((details, e) -> {
                            if (e != null) {
                                e.printStackTrace();
                                return;
                            }
                            long maxMemory = details.getJSON().getJSONObject("attributes").getJSONObject("limits").getInt("memory") * 1000000L;
                            registration.setMaxMemory(maxMemory);
                        });

                        sendDataToAll(server, SGCraftLinkCommon.DATA_SERVER_REGISTER, json.toMap());

                        if (!registration.isAnnounce())
                            break;

                        ComponentProvider<?> component = ColorProvider.ofComponent("&b&lꜱᴇʀᴠᴇʀ &8&l» &a" + serverName + " is now online! " + (registration.isWhitelisted() ? "(Whitelisted)" : ""));
                        component.setHover(ComponentHoverProvider.of(ComponentHoverProvider.Action.SHOW_TEXT, ColorProvider.ofComponent("&eClick to join " + server)));
                        component.setClick(ComponentClickProvider.of(ComponentClickProvider.Action.RUN_COMMAND, "/server " + server));
                        plugin.getPlayers().forEach(p -> p.sendMessageComponent(component));
                        break;
                    }
                    case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                        plugin.getLoggerProvider().info("Unregistered server " + server);
                        plugin.getDiscordProvider().getDiscordCommands().removePanelServer(server);
                        RegisteredServer registration = registeredServers.remove(server);
                        response.sendStatus(Status._200);

                        sendDataToAll(server, SGCraftLinkCommon.DATA_SERVER_UNREGISTER, json.toMap());

                        if (registration != null && registration.isAnnounce()) {
                            ComponentProvider<?> component = ColorProvider.ofComponent("&b&lꜱᴇʀᴠᴇʀ &8&l» &c" + serverName + " is now offline!");
                            component.setHover(ComponentHoverProvider.of(ComponentHoverProvider.Action.SHOW_TEXT, ColorProvider.ofComponent("&cJoin back later!")));
                            plugin.getPlayers().forEach(p -> p.sendMessageComponent(component));
                            plugin.getConsole().sendMessageComponent(component);
                        }
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
                    case SGCraftLinkCommon.DATA_JAILED: {
                        String username = json.getString("username");
                        String executor = json.getString("executor");
                        String reason = json.getString("reason");
                        long duration = json.getLong("duration");
                        long until = json.getLong("until");
                        boolean active = json.getBoolean("jailed");

                        Punishment punishment = new Punishment(PunishmentType.JAIL, username, executor, duration, until, reason, active, server);
                        plugin.getDiscordProvider().getPunishmentListener().broadcastToDiscord(punishment, false);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_GET_HEADHUNT: {
                        sendDataToAll(server, SGCraftLinkCommon.DATA_GET_HEADHUNT).thenAccept((result) -> {
                            int remaining = 0;
                            JSONObject res = new JSONObject();
                            JSONObject score = new JSONObject();
                            JSONObject servers = new JSONObject();

                            for (Map.Entry<String, DataResult> entry : result.entrySet()) {
                                JSONObject obj = entry.getValue().getJSON();
                                if (obj == null)
                                    continue;

                                int r = obj.getInt("remaining");
                                remaining += r;

                                JSONObject players = obj.getJSONObject("score");
                                for (String name : players.keySet()) {
                                    score.put(name, score.has(name) ? score.getInt(name) + players.getInt(name) : players.getInt(name));
                                }
                            }

                            res.put("servers", servers);
                            res.put("total", remaining);
                            res.put("score", score);
                            plugin.getLoggerProvider().warning("Responding to " + server + " with " + res);
                            response.setStatus(Status._200).send(res.toString());
                        });
                        break;
                    }
                    case SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE: {
                        for (LuckPermsManager.ProxyRestMessenger messenger : messengers) {
                            messenger.handleIncomingMessage(json);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_SECURITY_CHECK: {
                        UUID uuid = UUID.fromString(json.getString("uuid"));
                        PlayerProvider player = plugin.getPlayer(uuid);
                        if (player != null) {
                            plugin.getLoggerProvider().info(player.getName() + " is currently connected to: " + player.getServer());
                            response.sendStatus(Status._200);
                        } else {
                            response.sendStatus(Status._203);
                        }
                        break;
                    }
                    case SGCraftLinkCommon.DATA_GET_WHITELISTED_PERMS: {
                        String username = json.getString("username");
                        ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(username);
                        JSONObject obj = new JSONObject();
                        JSONArray perms = new JSONArray();
                        if (data == null || !data.contains("perms")) {
                            response.setStatus(Status._200).send(obj.put("perms", perms).toString());
                            break;
                        }

                        for (String perm : data.getStringList("perms")) {
                            perms.put(perm);
                        }

                        plugin.getLoggerProvider().info(username + " has whitelisted perms " + perms);
                        response.setStatus(Status._200).send(obj.put("perms", perms).toString());
                        break;
                    }
                    case SGCraftLinkCommon.DATA_SET_WHITELISTED_PERMS: {
                        String username = json.getString("username");
                        ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(username);
                        if (data == null) {
                            response.sendStatus(Status._203);
                            break;
                        }
                        List<String> perms = data.contains("perms") ? new ArrayList<>(data.getStringList("perms")) : new ArrayList<>();
                        perms.addAll(json.getJSONArray("perms").toList().stream().map(Object::toString).collect(Collectors.toList()));
                        data.set("perms", perms);
                        plugin.getLinkProvider().updateUser(username, data);
                        response.sendStatus(Status._202);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_GET_RESOURCES: {
                        RegisteredServer regServer = registeredServers.get(server);
                        response.setStatus(Status._200).send(new JSONObject().put("used", regServer.getUsedMemory()).put("max", regServer.getMaxMemory()).toString());
                        break;
                    }
                    case SGCraftLinkCommon.DATA_PLAYER_MIGRATE: {
                        Birthday birthday = plugin.getBirthdayManager().getBirthday(json.getString("old_name"));
                        if (birthday != null) {
                            plugin.getBirthdayManager().saveBirthday(json.getString("new_name"), birthday);
                        }

                        PlayerProvider sourcePlayer = plugin.getPlayer(json.getString("old_name"));
                        User sourceUser = plugin.getLinkProvider().getUser(json.getString("old_name"));
                        if (sourceUser != null) {
                            plugin.getLinkProvider().link(json.getString("new_name"), sourceUser);
                        }

                        if (sourcePlayer != null) {
                            plugin.getLinkProvider().unlink(sourcePlayer);
                        } else {
                            plugin.getLinkProvider().unlink(json.getString("old_name"));
                        }
                        // Do not break here!
                    }
                    default: {
                        sendDataToAll(server, datatype, json).thenAccept((r) -> {
                            JSONObject obj = new JSONObject();
                            for (String key : r.keySet()) {
                                DataResult res = r.get(key);
                                JSONObject a = res.getJSON();
                                if (a == null)
                                    continue;

                                obj.put(key, a);
                            }

                            getPlugin().getLoggerProvider().warning("Reply: " + obj);
                            response.setStatus(Status._200).send(obj.toString());
                        });
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        JSONObject obj = new JSONObject();
        obj.put("from_proxy", true);
        sendDataToAll(null, SGCraftLinkCommon.DATA_SERVER_UNREGISTER, obj).thenAccept((result) -> {
            try {
                manager.shutdown();
                registeredServers.clear();
                express.stop();
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return future;
    }

    public Collection<String> getServers() {
        return registeredServers.keySet();
    }

    public RegisteredServer getRegisteredServer(String server) {
        return registeredServers.get(server);
    }

    public Collection<RegisteredServer> getRegisteredServers() {
        return registeredServers.values();
    }

    public CompletableFuture<Map<String, DataResult>> sendDataToAll(@Nullable String origin, String type) {
        return sendDataToAll(origin, type, new JSONObject());
    }

    public CompletableFuture<Map<String, DataResult>> sendDataToAll(@Nullable String origin, String type, @Nullable Map<?, ?> map) {
        return sendDataToAll(origin, type, map != null ? new JSONObject(map) : new JSONObject());
    }

    public CompletableFuture<Map<String, DataResult>> sendDataToAll(@Nullable String origin, String type, JSONObject object) {
        CompletableFuture<Map<String, DataResult>> future = new CompletableFuture<>();
        Map<String, RegisteredServer> servers = new HashMap<>(registeredServers);

        AtomicInteger i = new AtomicInteger(servers.size() - (origin != null ? 1 : 0));
        Map<String, DataResult> results = new ConcurrentHashMap<>();
        AtomicBoolean called = new AtomicBoolean();
        for (String server : servers.keySet()) {
            if (server.equalsIgnoreCase(origin))
                continue;

            if (object.has("from_proxy")) {
                object.remove("from_proxy");
                object.put("server", server);
            }

            sendData(type, server, object, (result) -> {
                results.put(server, result);
                if (i.decrementAndGet() <= 0 && !called.get()) {
                    called.set(true);
                    future.complete(new HashMap<>(results));
                }
            });
        }
        return future;
    }

    public boolean sendData(String type, String server) {
        return sendData(type, server, (Map<?, ?>) null, null);
    }

    public boolean sendData(String type, String server, @Nullable Map<?, ?> map) {
        return sendData(type, server, map != null ? new JSONObject(map) : new JSONObject(), null);
    }

    public boolean sendData(String type, String server, @Nullable Map<?, ?> map, @Nullable Consumer<DataResult> callback) {
        return sendData(type, server, map != null ? new JSONObject(map) : new JSONObject(), callback);
    }

    public boolean sendData(String type, String server, @Nullable Consumer<DataResult> callback) {
        return sendData(type, server, new JSONObject(), callback);
    }

    public boolean sendData(String type, String server, @NotNull JSONObject object, @Nullable Consumer<DataResult> callback) {
        RegisteredServer registration = registeredServers.get(server);
        if (registration == null) {
            return false;
        }
        object.put("data-type", type);
        Map<String, String> headers = SGCraftLinkCommon.createStringMap("X-Authorization", registration.getToken());
        Runnable runnable = () -> {
            try {
                plugin.getLoggerProvider().warning("Sending " + type + " to " + server);
                CompletableFuture<DataResult> response = manager.fetch("http://" + registration.getIp(), registration.getPort(), HTTPManager.Method.POST, headers, object);
                response.whenComplete((res, err) -> {
                    if (err != null) {
                        plugin.getLoggerProvider().error(err.getLocalizedMessage());
                        if (callback != null)
                            callback.accept(new DataResult(404, null));
                        return;
                    }
                    plugin.getLoggerProvider().warning("Responded back with " + res.getStatus());
                    if (callback != null)
                        callback.accept(res);
                });
            } catch (Throwable e) {
                e.printStackTrace();
                if (callback != null)
                    callback.accept(new DataResult(404, null));
            }
        };

        if (plugin.getScheduler().isNotAccepting()) {
            runnable.run();
        } else {
            plugin.getScheduler().schedule(runnable, 1L, TimeUnit.MILLISECONDS);
        }

        return true;
    }

    public HTTPManager getHttpManager() {
        return manager;
    }

    public PanelManager getPanelManager() {
        return panelManager;
    }

    public PluginProvider getPlugin() {
        return plugin;
    }
}
