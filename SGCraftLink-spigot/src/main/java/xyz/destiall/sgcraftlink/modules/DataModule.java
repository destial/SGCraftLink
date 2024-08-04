package xyz.destiall.sgcraftlink.modules;

import com.Zrips.CMI.CMI;
import express.Express;
import express.utils.Status;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.hooks.LuckPermsHook;
import xyz.destiall.sgcraftlink.utils.DataResult;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;
import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class DataModule extends Module implements CommandExecutor {
    private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

    public static String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    private final Express express;
    private final HTTPManager manager;

    private final List<LuckPermsHook.BackendRestMessenger> messengers;

    public DataModule(SGCraftLinkSpigot plugin) {
        super(plugin, "comms.yml");
        express = new Express();
        manager = new HTTPManager();
        messengers = new ArrayList<>();
        plugin.getCommand("datacomm").setExecutor(this);
    }

    public void registerServer() {
        String authToken = getAuthToken();
        boolean whitelisted = plugin.getServer().hasWhitelist();
        if (!whitelisted) {
            // Method is not available in 1.8 :(
            try {
                whitelisted = plugin.getServer().isWhitelistEnforced();
            } catch (NoSuchMethodError e) {}
        }
        if (!whitelisted) {
            // try CMI
            try {
                whitelisted = CMI.getInstance().getConfigManager().isMaintenance();
            } catch (NoClassDefFoundError e) {}
        }
        Map<String, Object> data = createDataMap(
                "port", config.getInt("port", 1034),
                "auth", authToken,
                "panel_id", config.getString("panel_id", "null"),
                "whitelisted", whitelisted
        );
        tryRegisterServer(data);
    }

    private void unregisterServer() {
        String authToken = getAuthToken();
        Map<String, Object> data = createDataMap("auth", authToken);
        tryUnregisterServer(data);
    }

    private Scheduler.Task currentRegistrationTask = null;

    private void tryRegisterServer(Map<String, Object> data) {
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_REGISTER, data, (result) -> {
            if (!result.isSuccess()) {
                plugin.getLogger().warning("Failed! Trying to register server with proxy again...");
                if (currentRegistrationTask != null) {
                    currentRegistrationTask.cancel();
                }
                currentRegistrationTask = plugin.getScheduler().runTaskLaterAsync(() -> tryRegisterServer(data), 20L * 15L);
                return;
            }
            if (currentRegistrationTask != null) {
                currentRegistrationTask.cancel();
            }
            plugin.getLogger().info("Registered server with proxy");
        });
    }

    private void tryUnregisterServer(Map<String, Object> data) {
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_UNREGISTER, data, (result) -> {
            if (!result.isSuccess()) {
                plugin.getLogger().warning("Failed trying to unregister with proxy!");
                return;
            }
            plugin.getLogger().info("Unregistered server with proxy");
        }, true);
    }

    public void sendProxyData(String type, Map<?, ?> map, Consumer<DataResult> result) {
        sendProxyData(type, map, result, false);
    }

    public void sendProxyData(String type, Map<?, ?> map, Consumer<DataResult> result, boolean now) {
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);
        object.put("server", config.getString("server-name", "lobby"));
        Map<String, String> headers = HTTPManager.asMap("X-Authorization", config.getString("authentication-key"));
        Runnable runnable = () -> {
            try {
                plugin.getLogger().warning("Sending " + type + " to proxy");
                CompletableFuture<DataResult> response = manager.fetch("http://" + config.getString("proxy-ip", "192.168.1.99"), config.getInt("proxy-port", 1035), HTTPManager.Method.POST, headers, object);
                response.whenComplete((res, err) -> {
                    if (err != null) {
                        plugin.getLogger().warning("Responded back with 404");
                        plugin.getLogger().warning(err.getLocalizedMessage());
                        result.accept(new DataResult(404, null));
                        return;
                    }
                    plugin.getLogger().warning("Responded back with " + res.getStatus());
                    result.accept(res);
                });
            } catch (Exception e) {
                e.printStackTrace();
                result.accept(new DataResult(404, null));
            }
        };
        if (plugin.getScheduler().isNotAccepting() || now) {
            runnable.run();
        } else {
            try {
                plugin.getScheduler().runTaskLaterAsync(runnable, 1L);
            } catch (Exception e) {
                runnable.run();
            }
        }
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("server-name", "lobby");
        config.set("port", 1034);
        config.set("proxy-port", 1035);
        config.set("proxy-ip", "192.168.1.99");
        config.set("authentication-key", generateNewToken());
    }

    @Override
    public void disable() {
        plugin.getCommand("datacomm").setExecutor(null);

        try {
            unregisterServer();
            manager.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<LuckPermsHook.BackendRestMessenger> copy = new ArrayList<>(messengers);
        for (LuckPermsHook.BackendRestMessenger c : copy) {
            c.close();
        }
        messengers.clear();

        express.stop();
    }

    @Override
    public void enable() {
        express.post("/", (request, response) -> {
            try {
                String body = SGCraftLinkCommon.convertStreamToString(request.getBody()).trim();
                JSONObject json = new JSONObject(body);

                if (!json.has("data-type")) {
                    plugin.getLogger().warning("Request does not have any data-type!");
                    response.sendStatus(Status._500);
                    return;
                }

                String datatype = json.getString("data-type").toUpperCase();
                plugin.getLogger().warning("Received data " + datatype);

                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
                    String auth = config.getString("authentication-key");
                    if (!request.getHeader("X-Authorization").contains(auth)) {
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
                    case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                        plugin.getLogger().warning("Proxy has unregistered this server! This is probably due to a proxy plugin reload!");
                        plugin.getLogger().warning("We will try to register this server again!");
                        response.sendStatus(Status._200);

                        plugin.getScheduler().runTaskLaterAsync(this::registerServer, 20L * 15L);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE: {
                        for (LuckPermsHook.BackendRestMessenger messenger : messengers) {
                            messenger.handleIncomingMessage(json);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    default: {
                        // Intellisense is probably highlighting this to be removed, but in 1.8, this is still possible!
                        if (DataReceiveEvent.handlers.getRegisteredListeners() == null || DataReceiveEvent.handlers.getRegisteredListeners().length == 0) {
                            plugin.getLogger().warning("Received data " + datatype + " but have no implementation!");
                            response.sendStatus(Status._201);
                        } else {
                            DataReceiveEvent event = new DataReceiveEvent(json);
                            try {
                                plugin.getServer().getPluginManager().callEvent(event);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (event.getResponse() == null) {
                                response.sendStatus(event.getStatus());
                            } else {
                                response.setStatus(event.getStatus()).send(event.getResponse().toString());
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendStatus(Status._201);
            }
        });
        express.listen(config.getInt("port"));
        registerServer();
    }

    public String getAuthToken() {
        String authToken;
        if (config.contains("authentication-key")) {
            authToken = config.getString("authentication-key");
        } else {
            authToken = generateNewToken();
            config.set("authentication-key", authToken);
            save();
        }
        return authToken;
    }

    public String getServer() {
        return config.getString("server-name", "lobby");
    }

    public List<LuckPermsHook.BackendRestMessenger> getMessengers() {
        return messengers;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /datacomms [reg/unreg]"));
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "reg": {
                registerServer();
                sender.sendMessage(color("&6Trying to register server... Check console for details!"));
                break;
            }

            case "unreg": {
                unregisterServer();
                sender.sendMessage(color("&6Trying to unregister server... Check console for details!"));
                break;
            }

            default: break;
        }
        return false;
    }
}
