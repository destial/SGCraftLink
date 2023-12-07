package xyz.destiall.sgcraftlink.modules;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Migration;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class MigrationModule extends Module implements Listener, CommandExecutor, TabExecutor {
    private final boolean cmi;
    private final boolean piggybank;
    private final boolean luckperms;
    private final boolean parties;

    private final Set<Migration> awaitingMigration;

    public MigrationModule(SGCraftLinkSpigot plugin) {
        super(plugin, "migration.yml");
        awaitingMigration = ConcurrentHashMap.newKeySet();
        cmi = plugin.getServer().getPluginManager().isPluginEnabled("CMI");
        piggybank = plugin.getServer().getPluginManager().isPluginEnabled("PiggyBank");
        luckperms = plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms");
        parties = plugin.getServer().getPluginManager().isPluginEnabled("Parties");

        plugin.getCommand("migrate").setExecutor(this);

        for (String oldName : config.getKeys(false)) {
            String newName = config.getString(oldName);
            Migration migration = new Migration(this, oldName, newName);
            awaitingMigration.add(migration);
        }
    }

    @Override
    public void loadDefaultConfig() {}

    public boolean hasCmi() {
        return cmi;
    }

    public boolean hasPiggybank() {
        return piggybank;
    }

    public boolean hasLuckperms() {
        return luckperms;
    }

    public boolean hasParties() {
        return parties;
    }

    public Migration addMigration(String oldName, String newName) {
        Migration migration = new Migration(this, oldName, newName);
        awaitingMigration.add(migration);
        return awaitingMigration.stream().filter(m -> m.getOldName().equals(oldName)).findFirst().orElse(null);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Migration migration = awaitingMigration.stream().filter(m -> m.getNewName().equals(e.getPlayer().getName())).findFirst().orElse(null);
        if (migration == null)
            return;

        if (migration.migrate(false)) {
            awaitingMigration.remove(migration);
            plugin.getLogger().info("Completed migration of " + migration.getOldName() + " to " + migration.getNewName());
        }
    }

    @Override
    public void disable() {
        plugin.getCommand("migrate").setExecutor(null);
        for (Migration migration : awaitingMigration) {
            if (migration.hasMigrated())
                continue;

            config.set(migration.getOldName(), migration.getNewName());
        }
        save();
        awaitingMigration.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /migrate [source] [target]"));
            return false;
        }
        String sourceName = args[0];
        String targetName = args[1];
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}
