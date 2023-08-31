package xyz.destiall.sgcraftlink.items;

import com.Zrips.CMI.Modules.Economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.modules.Casino;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class CasinoSlot {
    private static final AtomicInteger ids = new AtomicInteger(0);
    private final int id;
    private final Casino casino;
    private final BlockFace left;
    private final Block center;
    private final BlockFace right;

    private Scheduler.Task rollingTask;

    public CasinoSlot(Casino casino, int id, Block center, BlockFace left, BlockFace right) {
        this.casino = casino;
        this.id = id;
        this.center = center;
        this.left = left;
        this.right = right;

        int max = Math.max(id, ids.get());
        ids.set(max);
    }

    public CasinoSlot(Casino casino, Block center, BlockFace left, BlockFace right) {
        this(casino, ids.incrementAndGet(), center, left, right);
    }

    public CasinoSlot(Casino casino, int id, ConfigurationSection config) {
        this(casino, id, config.getLocation("center").getBlock(), BlockFace.valueOf(config.getString("left")),
                BlockFace.valueOf(config.getString("right")));
    }

    public Configuration toConfig() {
        Configuration section = new YamlConfiguration();
        section.set("center", center.getLocation());
        section.set("left", left.name());
        section.set("right", right.name());
        return section;
    }

    public Block getCenter() {
        return center;
    }

    public Block getLeft() {
        return center.getRelative(left);
    }

    public Block getRight() {
        return center.getRelative(right);
    }

    public int getId() {
        return id;
    }

    public boolean inUse() {
        return rollingTask != null;
    }

    public void rollSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.5f);
    }

    public void roll(final float credits, final Player player) {
        if (inUse())
            return;

        AtomicInteger ticks = new AtomicInteger(0);
        rollingTask = casino.getPlugin().getScheduler().runTaskTimer(() -> {
            int t = ticks.incrementAndGet();

            if (!player.isOnline()) {
                stop();
                return;
            }

            if (t < 10) {
                rollLeft();
                rollCenter();
                rollRight();
                rollSound(player);
                return;
            }

            if (t < 15) {
                rollCenter();
                rollRight();
                rollSound(player);
                return;
            }

            Material left = getLeft().getType();
            Material center = getCenter().getType();

            int ticksLeft = left == center ? 35 : 20;

            if (t < ticksLeft) {
                if (t > 20) {
                    if (t % 2 == 0) {
                        rollRight();
                        rollSound(player);
                    }
                    return;
                }
                rollRight();
                rollSound(player);
                return;
            }

            stop();
            reward(credits, player);
        }, center.getLocation(), 1L, 5L);
    }

    public void rollLeft() {
        Block block = getLeft();
        Material next = getNextBlock();
        block.setType(next);
    }

    public void rollCenter() {
        Block block = getCenter();
        Material next = getNextBlock();
        block.setType(next);
    }

    public void rollRight() {
        Block block = getRight();
        Material next = getNextBlock();
        block.setType(next);
    }

    private Material getNextBlock() {
        int bound = casino.getRewards().size();
        int random = (int) (Math.random() * bound);
        return new ArrayList<>(casino.getRewards().keySet()).get(random);
    }

    public void stop() {
        if (!inUse())
            return;

        rollingTask.cancel();
        rollingTask = null;
    }

    private void reward(final float credits, final Player player) {
        Material left = getLeft().getType();
        Material center = getCenter().getType();
        Material right = getRight().getType();

        if (left == center && center == right) {
            double multiplier = casino.getRewards().get(left);
            float total = (float) (credits * multiplier);
            if (casino.isVault()) {
                Economy.depositPlayer(player, total);
            } else {
                Casino.CasinoBank bank = new Casino.CasinoBank(casino, player);
                bank.deposit((int) total);
            }
            player.sendMessage(color("&aYou have won " + (casino.isVault() ? total : (int) total) + " "
                    + casino.getCurrencyName() + "!"));
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

        player.sendMessage(color("&cYou won nothing :("));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 0.1f);
    }
}
