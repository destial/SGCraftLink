package xyz.destiall.sgcraftlink.modules;

import express.Express;
import express.utils.Status;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkFabric;
import xyz.destiall.sgcraftlink.utils.DataResult;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;

public class DataModule extends Module implements ServerPlayerEvents.Join {
    private final Express express;
    private final HTTPManager httpManager;

    private SecureRandom secureRandom;
    private Base64.Encoder base64Encoder;
    private TimerTask currentRegistrationTask = null;

    public DataModule(SGCraftLinkFabric mod) {
        super(mod, "comms.json");
        express = new Express();
        httpManager = new HTTPManager();
    }

    @Override
    public void enable() {
        express.post("/", (request, response) -> {
            try {
                String body = SGCraftLinkCommon.convertStreamToString(request.getBody()).trim();
                warn("Received data " + body);
                JSONObject json = new JSONObject(body);

                if (!json.has("data-type")) {
                    warn("Request does not have any data-type!");
                    response.sendStatus(Status._500);
                    return;
                }

                String datatype = json.getString("data-type").toUpperCase();

                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
                    String auth = config.getString("authentication-key");
                    if (!request.getHeader("X-Authorization").contains(auth)) {
                        response.sendStatus(Status._403);
                        return;
                    }
                }

                switch (datatype) {
                    case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                        JSONArray commands = json.optJSONArray("commands");
                        if (commands != null) {
                            for (int i = 0; i < commands.length(); i++) {
                                String command = commands.getString(i);
                                mod.dispatchCommand(command);
                            }
                        } else {
                            String cmd = json.optString("commands");
                            mod.dispatchCommand(cmd);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                        warn("Proxy has unregistered this server! This is probably due to a proxy plugin reload!");
                        warn("We will try to register this server again!");
                        response.sendStatus(Status._200);

                        mod.getScheduler().schedule(this::registerServer, 15L * 1000L);
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

    public void registerServer() {
        Map<String, Object> data = createDataMap(
                "port", config.optInt("port", 9999),
                "auth", getAuthToken(),
                "panel_id", config.optString("panel_id", "null"),
                "whitelisted", mod.getServer().isEnforceWhitelist(),
                "announce", config.optBoolean("announce", true),
                "ip", config.optString("proxy-ip", "172.18.0.1")
        );
        tryRegisterServer(data);
    }

    private void unregisterServer() {
        String authToken = config.getString("authentication-key");
        Map<String, Object> data = createDataMap("auth", authToken);
        tryUnregisterServer(data);
    }

    private void tryRegisterServer(Map<String, Object> data) {
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_REGISTER, data, (result) -> {
            if (!result.isSuccess()) {
                warn("Failed! Trying to register server with proxy again...");
                if (currentRegistrationTask != null) {
                    currentRegistrationTask.cancel();
                }
                currentRegistrationTask = mod.getScheduler().schedule(() -> tryRegisterServer(data), 10 * 1000L);
                return;
            }
            if (currentRegistrationTask != null) {
                currentRegistrationTask.cancel();
            }
            log("Registered server with proxy");
        });
    }

    private void tryUnregisterServer(Map<String, Object> data) {
        sendProxyData(SGCraftLinkCommon.DATA_SERVER_UNREGISTER, data, (result) -> {
            if (!result.isSuccess()) {
                warn("Failed trying to unregister with proxy!");
                return;
            }
            log("Unregistered server with proxy");
        }, true);
    }

    public void sendProxyData(String type) {
        sendProxyData(type, null, null, false);
    }

    public void sendProxyData(String type, @Nullable Map<?, ?> map) {
        sendProxyData(type, map, null, false);
    }

    public void sendProxyData(String type, @Nullable Consumer<DataResult> result) {
        sendProxyData(type, null, result, false);
    }

    public void sendProxyData(String type, @Nullable Map<?, ?> map, @Nullable Consumer<DataResult> result) {
        sendProxyData(type, map, result, false);
    }

    public void sendProxyData(String type, @Nullable Map<?, ?> map, @Nullable Consumer<DataResult> result, boolean now) {
        JSONObject object;
        if (map != null) {
            object = new JSONObject(map);
        } else {
            object = new JSONObject();
        }
        object.put("data-type", type);
        object.put("server", config.getString("server-name"));
        Map<String, String> headers = SGCraftLinkCommon.createStringMap("X-Authorization", config.getString("authentication-key"));
        Runnable runnable = () -> {
            try {
                warn("Sending " + type + " to proxy");
                CompletableFuture<DataResult> response = httpManager.fetch("http://" + config.getString("proxy-ip"), config.getInt("proxy-port"), HTTPManager.Method.POST, headers, object);
                response.whenComplete((res, err) -> {
                    if (err != null) {
                        mod.getLogger().warn(err.getLocalizedMessage());
                        if (result != null)
                            result.accept(new DataResult(404, null));
                        return;
                    }
                    warn("Responded back with " + res.getStatus());
                    if (result != null)
                        result.accept(res);
                });
            } catch (Throwable e) {
                e.printStackTrace();
                if (result != null)
                    result.accept(new DataResult(404, null));
            }
        };
        if (SGCraftLinkFabric.SHUTTING_DOWN) {
            runnable.run();
            return;
        }
        try {
            mod.getScheduler().schedule(runnable, 1L);
        } catch (Exception e) {
            runnable.run();
        }
    }

    public void loadDefaultConfig() {
        config.put("enabled", true);
        config.put("server-name", "modded");
        config.put("port", 9999);
        config.put("proxy-port", 1035);
        config.put("proxy-ip", "172.18.0.1");
        config.put("announce", false);
        config.put("authentication-key", generateNewToken());
        config.put("check-proxy", false);
    }

    public String generateNewToken() {
        byte[] randomBytes = new byte[24];
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        secureRandom.nextBytes(randomBytes);
        if (base64Encoder == null) {
            base64Encoder = Base64.getUrlEncoder();
        }
        return base64Encoder.encodeToString(randomBytes);
    }

    public String getAuthToken() {
        String authToken;
        if (config.has("authentication-key")) {
            authToken = config.getString("authentication-key");
        } else {
            authToken = generateNewToken();
            config.put("authentication-key", authToken);
            save();
        }
        return authToken;
    }

    @Override
    public void onJoin(ServerPlayerEntity player) {
        if (config.optBoolean("check-proxy", true)) {
            mod.getScheduler().schedule(() -> {
                Map<String, Object> map = createDataMap("uuid", player.getUuid().toString());
                sendProxyData(SGCraftLinkCommon.DATA_SECURITY_CHECK, map, (result) -> {
                    mod.getScheduler().schedule(() -> {
                        if (!player.isDisconnected()) {
                            player.onDisconnect();
                            player.networkHandler.disconnect(new DisconnectionInfo(Text.of("You are not connected to the proxy!")));
                        }
                    }, 1L);
                });
            }, 50L);
        }
    }

    public HTTPManager getHttpManager() {
        return httpManager;
    }

    @Override
    public void disable() {
        unregisterServer();
        express.stop();
        httpManager.shutdown();
    }
}
