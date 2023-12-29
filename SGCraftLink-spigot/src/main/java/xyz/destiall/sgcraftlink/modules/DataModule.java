package xyz.destiall.sgcraftlink.modules;

import express.Express;
import express.utils.MediaType;
import express.utils.Status;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.track.Track;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.hooks.CoreProtectHook;
import xyz.destiall.sgcraftlink.items.Migration;
import xyz.destiall.sgcraftlink.utils.DataResult;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;

public class DataModule extends Module {
    private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

    public static String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    private final Express express;
    private final HTTPManager manager;

    private final List<LuckPermsModule.BackendRestMessenger> messengers;

    public DataModule(SGCraftLinkSpigot plugin) {
        super(plugin, "comms.yml");
        express = new Express();
        manager = new HTTPManager();
        messengers = new ArrayList<>();
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
                    case SGCraftLinkCommon.DATA_CHATS: {
                        @Nullable String player = json.has("player") ? json.getString("player") : null;
                        int limit = json.getInt("limit");
                        long since = json.has("time") ? System.currentTimeMillis() : json.getLong("time");

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

                        List<CoreProtectHook.ChatMessageResult> messages = hook.getChats(player, limit, since);
                        if (messages == null) {
                            res.put("messages", array);
                            plugin.getLogger().warning("No messages found!");
                            plugin.getLogger().info("Sending back to proxy");
                            response.setContentType(MediaType._str);
                            response.setStatus(Status._200).send(res.toString());
                            break;
                        }

                        for (int i = 0; i < messages.size() && i < limit; i++) {
                            CoreProtectHook.ChatMessageResult m = messages.get(i);
                            JSONObject obj = new JSONObject();
                            obj.put("time", m.getTime());
                            obj.put("msg", m.getMessage());
                            obj.put("user", m.getUsername());
                            array.put(obj);
                        }

                        res.put("messages", array);
                        plugin.getLogger().info("Sending back " + array.length() + " messages to proxy");
                        response.setContentType(MediaType._str);
                        response.setStatus(Status._200).send(res.toString());
                        break;
                    }
                    case SGCraftLinkCommon.DATA_GET_HEADHUNT: {
                        JSONObject res = new JSONObject();
                        HeadHunt hunt = plugin.getModule(HeadHunt.class);
                        if (hunt == null) {
                            response.setStatus(Status._201).send(res.toString());
                            break;
                        }

                        res.put("remaining", hunt.getRemaining());
                        JSONObject players = new JSONObject();
                        for (Map.Entry<String, Integer> score : hunt.getScore().entrySet()) {
                            players.put(score.getKey(), score.getValue());
                        }
                        res.put("score", players);
                        response.setStatus(Status._200).send(res.toString());
                        break;
                    }
                    case SGCraftLinkCommon.DATA_START_HEADHUNT: {
                        HeadHunt hunt = plugin.getModule(HeadHunt.class);
                        if (hunt == null) {
                            response.sendStatus(Status._201);
                            break;
                        }
                        hunt.setCheckOtherServers(true);
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_END_HEADHUNT: {
                        HeadHunt hunt = plugin.getModule(HeadHunt.class);
                        if (hunt == null) {
                            response.sendStatus(Status._201);
                            break;
                        }
                        hunt.setCheckOtherServers(false);
                        hunt.announceWinner();
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_CLEAR_SCORE_HEADHUNT: {
                        HeadHunt hunt = plugin.getModule(HeadHunt.class);
                        if (hunt == null) {
                            response.setStatus(Status._201);
                            break;
                        }

                        hunt.getScore().clear();
                        hunt.getOtherServersScore().clear();
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_REMOVE_HEAD_HEADHUNT: {
                        HeadHunt hunt = plugin.getModule(HeadHunt.class);
                        if (hunt == null) {
                            response.setStatus(Status._201);
                            break;
                        }

                        for (Location loc : hunt.getHunts()) {
                            loc.getBlock().setType(Material.AIR);
                        }

                        hunt.getHunts().clear();
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_BIRTHDAY: {
                        BirthdayModule bday = plugin.getModule(BirthdayModule.class);
                        if (bday == null) {
                            response.sendStatus(Status._201);
                            break;
                        }

                        String name = json.getString("name");
                        boolean celebrated = json.getBoolean("celebrated");

                        Player online = plugin.getServer().getPlayer(name);
                        if (online != null) {
                            bday.celebrate(online, celebrated);
                        } else {
                            bday.addBirthday(name, celebrated);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_SERVER_UNREGISTER: {
                        plugin.getLogger().warning("Proxy has unregistered this server! This is probably due to a proxy plugin reload!");
                        plugin.getLogger().warning("We will try to register this server again!");
                        registerServer();
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_GET_RANKS: {
                        LuckPermsModule module = plugin.getModule(LuckPermsModule.class);
                        if (module == null)
                            return;

                        UUID uuid = UUID.fromString(json.getString("uuid"));

                        module.getApi().getUserManager().loadUser(uuid).whenComplete((user, error) -> {
                            if (error != null) {
                                error.printStackTrace();
                                response.sendStatus(Status._201);
                                return;
                            }

                            Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
                            Map<Track, Group> tracks = new HashMap<>();
                            for (Track track : module.getApi().getTrackManager().getLoadedTracks()) {
                                tracks.put(track, null);
                                List<String> groups = track.getGroups();
                                for (String group : groups) {
                                    for (Group inherited : inheritedGroups) {
                                        if (inherited.getName().equalsIgnoreCase(group)) {
                                            Group current = tracks.get(track);
                                            if (current == null || current.getWeight().orElse(0) < inherited.getWeight().orElse(0)) {
                                                tracks.put(track, inherited);
                                            }
                                        }
                                    }
                                }
                                if (tracks.get(track) == null) {
                                    tracks.remove(track);
                                }
                            }

                            List<String> groups = tracks.values().stream().map(Group::getName).collect(Collectors.toList());
                            JSONObject res = new JSONObject();
                            res.put("ranks", new JSONArray(groups));

                            response.setStatus(Status._200).send(res.toString());
                        });
                        break;
                    }
                    case SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE: {
                        for (LuckPermsModule.BackendRestMessenger messenger : messengers) {
                            messenger.handleIncomingMessage(json);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                    case SGCraftLinkCommon.DATA_PLAYER_MIGRATE: {
                        MigrationModule module = plugin.getModule(MigrationModule.class);
                        if (module == null) {
                            response.sendStatus(Status._201);
                            break;
                        }
                        String oldName = json.getString("old_name");
                        String newName = json.getString("new_name");
                        Migration migration = module.addMigration(oldName, newName);
                        if (migration.hasMigrated() && !migration.migrate(true)) {
                            response.sendStatus(Status._202);
                            break;
                        }
                        response.sendStatus(Status._200);
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
                response.sendStatus(Status._201);
            }
        });
        express.listen(config.getInt("port"));

        registerServer();
    }

    public void registerServer() {
        String authToken;
        if (config.contains("authentication-key")) {
            authToken = config.getString("authentication-key");
        } else {
            authToken = generateNewToken();
            config.set("authentication-key", authToken);
            save();
        }

        Map<String, Object> data = createDataMap("port", config.getInt("port", 1034), "auth", authToken);
        tryRegisterServer(data);
    }

    private void unregisterServer() {
        String authToken;
        if (config.contains("authentication-key")) {
            authToken = config.getString("authentication-key");
        } else {
            authToken = generateNewToken();
            config.set("authentication-key", authToken);
            save();
        }

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
                currentRegistrationTask = plugin.getScheduler().runTaskLaterAsync(() -> tryRegisterServer(data), 20L * 60L);
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
        });
    }

    public void sendProxyData(String type, Map<?, ?> map, Consumer<DataResult> result) {
        JSONObject object = new JSONObject(map);
        object.put("data-type", type);
        object.put("server", config.getString("server-name", "lobby"));
        Map<String, String> headers = HTTPManager.headers("X-Authorization", config.getString("authentication-key"));
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
        if (plugin.getScheduler().isNotAccepting()) {
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
        config.set("server-name", "lobby");
        config.set("port", 1034);
        config.set("proxy-port", 1035);
        config.set("proxy-ip", "192.168.1.99");
        config.set("authentication-key", generateNewToken());
    }

    @Override
    public void disable() {
        try {
            unregisterServer();
            manager.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<LuckPermsModule.BackendRestMessenger> copy = new ArrayList<>(messengers);
        for (LuckPermsModule.BackendRestMessenger c : copy) {
            c.close();
        }
        messengers.clear();

        express.stop();
    }

    public String getServer() {
        return config.getString("server-name", "lobby");
    }

    public List<LuckPermsModule.BackendRestMessenger> getMessengers() {
        return messengers;
    }
}
