package xyz.destiall.sgcraftlink.hooks;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.database.Database;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CoreProtectHook extends Module {
    private CoreProtectAPI api;
    public CoreProtectHook(SGCraftLinkSpigot plugin) {
        super(plugin, null);

        Plugin cp = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
        if (!(cp instanceof CoreProtect)) {
            return;
        }

        if (!cp.isEnabled()) {
            return;
        }

        api = ((CoreProtect) cp).getAPI();
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
}
