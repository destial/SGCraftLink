package xyz.destiall.sgcraftlink.modules.common;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.items.Migration;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class MigrationModule extends Module implements Listener, CommandExecutor, TabExecutor {
    private final boolean cmi;
    private final boolean piggybank;
    private final boolean luckperms;
    private final boolean parties;
    private final boolean griefPrevention;
    private final boolean survivalPlots;

    private final Set<Migration> awaitingMigration;

    public MigrationModule(SGCraftLinkSpigot plugin) {
        super(plugin, "migration.yml");
        awaitingMigration = ConcurrentHashMap.newKeySet();
        cmi = plugin.getServer().getPluginManager().isPluginEnabled("CMI");
        piggybank = plugin.getServer().getPluginManager().isPluginEnabled("PiggyBank");
        luckperms = plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms");
        parties = plugin.getServer().getPluginManager().isPluginEnabled("Parties");
        griefPrevention = plugin.getServer().getPluginManager().isPluginEnabled("GriefPrevention");
        survivalPlots = plugin.getServer().getPluginManager().isPluginEnabled("SurvivalPlots");

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

    public boolean hasGriefPrevention() {
        return griefPrevention;
    }

    public boolean hasSurvivalPlots() {
        return survivalPlots;
    }

    public Migration addMigration(String oldName, String newName) {
        Migration migration = new Migration(this, oldName, newName);
        awaitingMigration.add(migration);
        return awaitingMigration.stream().filter(m -> m.getOldName().equals(oldName)).findFirst().orElse(null);
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        if (!e.getDataType().equals(SGCraftLinkCommon.DATA_PLAYER_MIGRATE))
            return;

        String oldName = e.getJson().getString("old_name");
        String newName = e.getJson().getString("new_name");
        Migration migration = addMigration(oldName, newName);
        if (migration.hasMigrated() && !migration.migrate(true))
            return;

        e.setResponse(new JSONObject().put("success", true));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Migration migration = awaitingMigration.stream().filter(m -> m.getNewName().equals(e.getPlayer().getName())).findFirst().orElse(null);
        if (migration == null)
            return;

        if (migration.migrate(false)) {
            awaitingMigration.remove(migration);
            config.set(migration.getOldName(), null);
            plugin.getLogger().info("Completed migration of " + migration.getOldName() + " to " + migration.getNewName());
        }
    }

    @Override
    public void disable() {
        plugin.getCommand("migrate").setExecutor(null);
        for (Migration migration : awaitingMigration) {
            if (migration.hasMigrated()) {
                config.set(migration.getOldName(), null);
                continue;
            }

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

        Migration migration = addMigration(sourceName, targetName);
        if (migration.migrate(false)) {
            awaitingMigration.remove(migration);
            config.set(migration.getOldName(), null);
            plugin.getLogger().info("Completed migration of " + migration.getOldName() + " to " + migration.getNewName());
            sender.sendMessage(color("&aCompleted migration of " + migration.getOldName() + " to " + migration.getNewName()));
        } else {
            sender.sendMessage(color("&cUnable to complete migration of " + migration.getOldName() + " to " + migration.getNewName()));
            sender.sendMessage(color("&cSee console for more details!"));
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList())

                : args.length == 2 ? plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(args[0]))
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList())
                : null;
    }
}
