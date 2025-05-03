package xyz.destiall.sgcraftlink.modules.hooks;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.database.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.Module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CoreProtectHook extends Module implements Listener {
    private CoreProtectAPI api;
    public CoreProtectHook(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        if (!e.getDataType().equals(SGCraftLinkCommon.DATA_CHATS))
            return;

        @Nullable String player = e.getJson().has("player") ? e.getJson().getString("player") : null;
        int limit = e.getJson().getInt("limit");
        long since = e.getJson().has("time") ? System.currentTimeMillis() : e.getJson().getLong("time");

        JSONArray array = new JSONArray();
        JSONObject res = new JSONObject();

        List<CoreProtectHook.ChatMessageResult> messages = getChats(player, limit, since);
        if (messages == null) {
            res.put("messages", array);
            plugin.getLogger().warning("No messages found!");
            plugin.getLogger().info("Sending back to proxy");
            e.setReply(res);
            return;
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
        e.setReply(res);
    }

    public List<ChatMessageResult> getChats(@Nullable String player, int limit, long since) {
        if (api == null)
            return null;

        List<ChatMessageResult> data = new ArrayList<>();
        String prompt = "SELECT time,message,user FROM " + ConfigHandler.prefix + "chat WHERE" +
                (player != null ? " user=(SELECT id FROM " + ConfigHandler.prefix + "user WHERE user=?) AND" : "") +
                " time < ? ORDER BY time DESC LIMIT ?";
        try (Connection connection = Database.getConnection(false, 100)) {
            PreparedStatement statement = connection.prepareStatement(prompt);
            if (player != null) {
                statement.setString(1, player);
            }
            statement.setLong(player != null ? 2 : 1, since);
            statement.setInt(player != null ? 3 : 2, limit);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                long time = resultSet.getLong("time");
                String message = resultSet.getString("message");
                if (player == null) {
                    int user = resultSet.getInt("user");
                    try (PreparedStatement userStatement = connection.prepareStatement("SELECT user FROM " + ConfigHandler.prefix + "user WHERE id=?")) {
                        userStatement.setInt(1, user);

                        ResultSet userResult = userStatement.executeQuery();
                        if (userResult.next()) {
                            String username = userResult.getString("user");
                            ChatMessageResult chatMessageResult = new ChatMessageResult(time, username, message);
                            data.add(chatMessageResult);
                        }
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                    continue;
                }
                ChatMessageResult chatMessageResult = new ChatMessageResult(time, player, message);
                data.add(chatMessageResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    public static class ChatMessageResult {
        private final long time;
        private final String username;
        private final String message;

        public ChatMessageResult(long time, String username, String message) {
            this.time = time;
            this.username = username;
            this.message = message;
        }

        public long getTime() {
            return time;
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public void enable() {
        Plugin cp = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
        if (!(cp instanceof CoreProtect)) {
            return;
        }

        if (!cp.isEnabled()) {
            return;
        }

        api = ((CoreProtect) cp).getAPI();
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
