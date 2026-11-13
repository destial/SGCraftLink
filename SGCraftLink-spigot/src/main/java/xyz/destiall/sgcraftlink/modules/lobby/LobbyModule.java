package xyz.destiall.sgcraftlink.modules.lobby;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyModule extends Module implements Listener {
    private final Map<String, NPCGlow> npcMap;
    private final List<String> commandQueue;
    private boolean dirtyConfig = false;
    public LobbyModule(SGCraftLinkSpigot plugin) {
        super(plugin, "lobby.yml");
        npcMap = new HashMap<>();
        commandQueue = new ArrayList<>();
    }

    @Override
    public void loadDefaultConfig() {
        DataModule dm = plugin.getModule(DataModule.class);
        config.set("enabled", dm != null && dm.getServer().equalsIgnoreCase("lobby"));
        config.set("npcs.survival.id", 513);
        config.set("npcs.survival.color", ChatColor.RED.name());

        config.set("npcs.vanilla.id", 512);
        config.set("npcs.vanilla.color", ChatColor.LIGHT_PURPLE.name());

        config.set("npcs.practice.id", 514);
        config.set("npcs.practice.color", ChatColor.YELLOW.name());

        config.set("npcs.pgm.id", 515);
        config.set("npcs.pgm.color", ChatColor.DARK_PURPLE.name());

        config.set("npcs.events.id", 516);
        config.set("npcs.events.color", ChatColor.AQUA.name());
    }

    @Override
    public void disable() {
        if (dirtyConfig) {
            saveConfig();
        }
    }

    @Override
    public void enable() {
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section != null) {
            for (String server : section.getKeys(false)) {
                int id = section.getInt(server + ".id");
                ChatColor color = ChatColor.valueOf(section.getString(server + ".color"));
                boolean glowing = section.getBoolean(server + ".glowing", false);
                npcMap.put(server, new NPCGlow(id, color, glowing));
            }
        }

        plugin.getScheduler().runTaskTimer(() -> {
            if (!commandQueue.isEmpty()) {
                String cmd = commandQueue.remove(0);
                plugin.dispatchCommand(cmd, false);
            }
        }, 0L, 10L);
    }

    @EventHandler
    public void onRegister(DataReceiveEvent e) {
        if (e.getDataType().equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
            String server = e.getJson().getString("server");
            NPCGlow pair = npcMap.get(server);
            if (pair == null)
                return;

            if (pair.isGlowing())
                return;

            pair.setGlowing(true);
            config.set("npcs." + server + ".glowing", true);
            dirtyConfig = true;
            String select = "npc select " + pair.getId();
            String color = "npc glowing --color " + pair.getColor().name().replace(" ", "_");
            String glow = "npc glowing";
            commandQueue.add(select);
            commandQueue.add(color);
            commandQueue.add(glow);

            if (e.getJson().has("lowest_ver") && !server.equalsIgnoreCase("pgm")) {

                String rename = "&" + pair.getColor().getChar() + server.substring(0, 1).toUpperCase() + server.substring(1) + " &7(&f";

                if (e.getJson().getString("lowest_ver").equalsIgnoreCase(e.getJson().getString("highest_ver"))) {
                    rename += e.getJson().getString("lowest_ver");
                } else {
                    rename += e.getJson().getString("lowest_ver") + " - " + e.getJson().getString("highest_ver");
                }
                rename += "&7)";

                commandQueue.add("npc rename " + rename);
            }
        } else if (e.getDataType().equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_UNREGISTER)) {
            String server = e.getJson().getString("server");
            NPCGlow pair = npcMap.get(server);
            if (pair == null)
                return;

            if (!pair.isGlowing())
                return;

            pair.setGlowing(false);
            config.set("npcs." + server + ".glowing", false);
            dirtyConfig = true;
            String select = "npc select " + pair.getId();
            String glow = "npc glowing";
            commandQueue.add(select);
            commandQueue.add(glow);
        }
    }
}
