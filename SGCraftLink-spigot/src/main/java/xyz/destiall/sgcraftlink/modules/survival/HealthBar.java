package xyz.destiall.sgcraftlink.modules.survival;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HealthBar extends Module implements Listener {
    public static class MobBar {
        private String oldName;
        private Scheduler.Task revertTask;
        private boolean customNameVisible;
    }

    private final String[] defaultBar = new String[21];
    private final Map<UUID, MobBar> mobs = new ConcurrentHashMap<>();
    private List<String> blockedWorlds;
    private Scoreboard board;
    private Objective objective;

    public HealthBar(SGCraftLinkSpigot plugin) {
        super(plugin, "health-bar.yml");
    }

    private String generateBarString(int health, int maxHealth) {
        String bar = "";
        int scaledHealth = health * 20 / maxHealth;
        if (scaledHealth < 0)
            return bar;

        bar = defaultBar[scaledHealth];

        return bar;
    }

    public void checkDisabledWorld(Player player, String worldName) {
        List<String> s = config.getStringList("disabled-worlds");
        if (s.contains(worldName)) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        } else {
            player.setScoreboard(board);
        }
    }

    public void checkPermission(Player player) {
        if (player.hasPermission("healthbar.see")) {
            checkDisabledWorld(player, player.getWorld().getName());
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void createHealthBar() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
        String displayName = config.getString("displayName", "&c<3").replaceAll("&", "§");
        displayName = displayName.replaceAll("<3", "❤");
        objective = board.registerNewObjective("healthBar", "health");
        objective.setDisplayName(displayName);
        if (config.getBoolean("players")) {
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        } else {
            board.clearSlot(DisplaySlot.BELOW_NAME);
        }

        if (config.getBoolean("require-permission")) {
            plugin.getServer().getOnlinePlayers().forEach(this::checkPermission);
        } else {
            plugin.getServer().getOnlinePlayers().forEach((player) -> checkDisabledWorld(player, player.getWorld().getName()));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        if (blockedWorlds.contains(entity.getWorld().getName()))
            return;

        UUID uuid = entity.getUniqueId();
        boolean fatalDamage = event.getFinalDamage() >= entity.getHealth();

        if (!fatalDamage &&
            entity.getType() != EntityType.ENDER_DRAGON &&
            entity.getType() != EntityType.WITHER &&
            entity.getType() != EntityType.PLAYER) {
            MobBar mobBar;
            if (!mobs.containsKey(uuid)) {
                mobBar = new MobBar();
                mobBar.customNameVisible = entity.isCustomNameVisible();
                mobBar.oldName = entity.getCustomName();
                mobs.put(uuid, mobBar);
            } else {
                mobBar = mobs.get(uuid);
                mobBar.revertTask.cancel();
            }
            mobBar.revertTask = plugin.getScheduler().runTaskLater(() -> {
                MobBar mb = mobs.remove(uuid);
                if (entity.isDead() || mb == null)
                    return;

                entity.setCustomNameVisible(mb.customNameVisible);
                entity.setCustomName(mb.oldName);
            }, entity, (long) config.getInt("mobs-fade-out", 13) * 20L);
            entity.setCustomNameVisible(true);
            entity.setCustomName(generateBarString((int) entity.getHealth() - (int) event.getFinalDamage(), (int) entity.getMaxHealth()));
        }

        if (fatalDamage && mobs.containsKey(uuid)) {
            MobBar mb = mobs.remove(uuid);
            entity.setCustomName(mb.oldName);
            entity.setCustomNameVisible(mb.customNameVisible);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent e) {
        mobs.remove(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        checkDisabledWorld(e.getPlayer(), e.getPlayer().getWorld().getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        checkPermission(e.getPlayer());
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
        config.set("players", true);
        config.set("displayName", "&c<3");
        config.set("mobs-fade-out", 13);
        config.set("require-permission", false);
        config.set("disabled-worlds", Arrays.asList("world_example", "world_example2"));
    }

    @Override
    public void disable() {
        mobs.forEach((uuid, mb) -> {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null) {
                mb.revertTask.cancel();
                entity.setCustomName(mb.oldName);
                entity.setCustomNameVisible(mb.customNameVisible);
            }
        });
        objective.unregister();
    }

    @Override
    public void enable() {
        defaultBar[0]  = "§c▌         ";
        defaultBar[1]  = "§c█         ";
        defaultBar[2]  = "§c█▌        ";
        defaultBar[3]  = "§c██        ";
        defaultBar[4]  = "§c██▌       ";
        defaultBar[5]  = "§c███       ";
        defaultBar[6]  = "§e███▌      ";
        defaultBar[7]  = "§e████      ";
        defaultBar[8]  = "§e████▌     ";
        defaultBar[9]  = "§e█████     ";
        defaultBar[10] = "§e█████▌    ";
        defaultBar[11] = "§e██████    ";
        defaultBar[12] = "§a██████▌   ";
        defaultBar[13] = "§a███████   ";
        defaultBar[14] = "§a███████▌  ";
        defaultBar[15] = "§a████████  ";
        defaultBar[16] = "§a████████▌ ";
        defaultBar[17] = "§a█████████ ";
        defaultBar[18] = "§a█████████▌";
        defaultBar[19] = "§a██████████";
        defaultBar[20] = "§a██████████▌";

        blockedWorlds = config.getStringList("disabled-worlds");
        createHealthBar();
    }
}
