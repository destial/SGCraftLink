package xyz.destiall.sgcraftlink.modules.vanilla;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.event.world.MVWorldRegeneratedEvent;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MVModule extends Module implements Listener {
    private final MultiverseCoreApi api;
    private List<String> worlds = null;
    public MVModule(SGCraftLinkSpigot plugin) {
        super(plugin, "regen-world.yml");
        api = MultiverseCoreApi.get();
    }

    @Override
    public void disable() {
        worlds.clear();
    }

    @Override
    public void loadDefaultConfig() {
        config.set("worlds.world_the_end.enabled", true);
        config.set("worlds.world_the_end.message", "&dThe end has been reset!");
        config.set("worlds.world_the_end.commands", Collections.singletonList("blockdoors open endportal"));
    }

    @Override
    public void enable() {
        worlds = api.getWorldManager().getLoadedWorlds().stream()
                .map(MultiverseWorld::getAliasOrName)
                .filter(name ->
                        config.getConfigurationSection("worlds").getKeys(false).stream()
                        .anyMatch(key ->
                                config.getBoolean("worlds." + key + ".enabled", false)
                                        && name.equalsIgnoreCase(key)))
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onWorldRegen(MVWorldRegeneratedEvent e) {
        if (!worlds.contains(e.getWorld().getAliasOrName()))
            return;

        String message = config.getString("worlds." + e.getWorld().getAliasOrName() + ".message", "&dThe world '" + e.getWorld().getName() + "' has been reset!");
        List<String> commands = config.getStringList("worlds." + e.getWorld().getAliasOrName() + ".commands");

        for (Player player: getPlugin().getServer().getOnlinePlayers()) {
            player.sendMessage(SGCraftLinkSpigot.color(message));
        }
        for (String cmd : commands) {
            getPlugin().dispatchCommand(cmd);
        }
    }
}
