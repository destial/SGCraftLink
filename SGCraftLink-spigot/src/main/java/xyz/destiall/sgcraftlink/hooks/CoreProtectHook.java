package xyz.destiall.sgcraftlink.hooks;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.database.Database;
import org.bukkit.plugin.Plugin;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Pair;
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

    public List<Pair<Long, String>> getChats(String player, int limit) {
        if (api == null)
            return null;

        List<Pair<Long, String>> data = new ArrayList<>();
        try (Connection connection = Database.getConnection(false, 100)) {

            PreparedStatement statement = connection.prepareStatement("SELECT time,message FROM " + ConfigHandler.prefix + "chat WHERE user=(SELECT id FROM " + ConfigHandler.prefix + "user WHERE user=?) AND time < ? ORDER BY time DESC LIMIT ?");
            statement.setString(1, player);
            statement.setLong(2, System.currentTimeMillis());
            statement.setInt(3, limit);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                long time = resultSet.getLong("time");
                String message = resultSet.getString("message");
                data.add(new Pair<>(time, message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}
}
