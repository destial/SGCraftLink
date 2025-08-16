package xyz.destiall.sgcraftlink.listeners;

import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeGetter extends Module implements Listener {
    private final Map<UUID, Integer> playTimeMap;

    public PlaytimeGetter(SGCraftLinkSpigot plugin) {
        super(plugin);
        playTimeMap = new ConcurrentHashMap<>();
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {}

    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        if (playTimeMap.containsKey(e.getPlayer().getUniqueId()))
            return;

        plugin.getModule(DataModule.class).sendProxyData(SGCraftLinkCommon.DATA_GET_PLAYTIME, SGCraftLinkCommon.createDataMap("uuid", e.getPlayer().getUniqueId()), (r) -> {
            plugin.getLogger().info(r.getContent());
            JSONObject json = r.getJSON();
            if (json == null)
                return;

            int totalTicks = 0;
            for (String server : json.keySet()) {
                int ticksPlayed = json.getJSONObject(server).getInt("ticks_played");
                totalTicks += ticksPlayed;
            }

            playTimeMap.put(e.getPlayer().getUniqueId(), totalTicks);
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        plugin.getScheduler().runTaskLater(() -> {
            if (e.getPlayer().isOnline())
                return;

            playTimeMap.remove(e.getPlayer().getUniqueId());
        }, 20L * 60);
    }

    public int getTicksPlayed(OfflinePlayer player) {
        return playTimeMap.getOrDefault(player.getUniqueId(), 0) + getLocalTicksPlayed(player.getUniqueId());
    }

    public int getLocalTicksPlayed(UUID uuid) {
        try {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
            if (player.getPlayer() != null) {
                return player.getPlayer().getStatistic(getStatistic());
            }
            return player.getStatistic(getStatistic());
        } catch (Throwable e) {}

        File statFolder = new File(plugin.getServer().getWorlds().get(0).getWorldFolder(), "stats");
        File statFile = new File(statFolder, uuid + ".json");

        if (!statFile.exists())
            return 0;

        try {
            String stats = null;
            try (FileInputStream reader = new FileInputStream(statFile)) {
                stats = SGCraftLinkCommon.convertStreamToString(reader);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (stats != null) {
                JSONObject obj = new JSONObject(stats);
                try {
                    JSONObject root = obj.getJSONObject("stats");
                    JSONObject custom = root.getJSONObject("minecraft:custom");
                    return custom.getInt(getStatistic().getKey().toString());
                } catch (Throwable ex) {
                    return obj.getInt("stat.playOneMinute");
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    private Statistic getStatistic() {
        try {
            return Statistic.PLAY_ONE_MINUTE;
        } catch (Throwable e) {
            return Statistic.valueOf("PLAY_ONE_TICK");
        }
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        if (!e.getDataType().equals(SGCraftLinkCommon.DATA_GET_PLAYTIME))
            return;

        String id = e.getJson().getString("uuid");
        plugin.getLogger().info("Requested playtime of " + id);

        UUID uuid = UUID.fromString(id);
        int ticksPlayed = getLocalTicksPlayed(uuid);
        JSONObject reply = new JSONObject();
        reply.put("ticks_played", ticksPlayed);
        e.setReply(reply);
    }

    @Override
    public boolean isUniversallyCompatible() {
        return true;
    }
}
