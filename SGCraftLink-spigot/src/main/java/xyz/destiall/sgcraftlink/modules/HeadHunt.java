package xyz.destiall.sgcraftlink.modules;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HeadHunt extends Module implements Listener, CommandExecutor {
    private final Set<Location> hunts = new HashSet<>();
    private final Map<String, Integer> players = new ConcurrentHashMap<>();
    private final List<BlockPlaceEvent> internalEvents = new ArrayList<>();
    private ItemStack head = new ItemStack(Material.SKELETON_SKULL);
    private String blockData;

    public HeadHunt(SGCraftLinkSpigot plugin) {
        super(plugin, "hunt.yml");

        head = config.getItemStack("egg", head);
        String metaString = head.getItemMeta().getAsString();
        int index = metaString.indexOf(",display{");
        blockData = metaString.substring(0, index != -1 ? index : metaString.length());
        if (index != -1) {
            blockData += "}";
        }

        blockData = "minecraft:" + head.getType().name().toLowerCase() + blockData;

        List<?> locations = config.getList("locations");
        if (locations != null) {
            for (Object loc : locations) {
                try {
                    Location location = (Location) loc;
                    hunts.add(location);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection != null) {
            for (String key : playersSection.getKeys(false)) {
                int amount = playersSection.getInt(key, 0);
                players.put(key, amount);
            }
        }

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Location location : hunts) {
                World world = location.getWorld();
                if (world == null)
                    continue;

                if (!location.getChunk().isLoaded())
                    continue;

                Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, particleLoc, 1, new Particle.DustTransition(Color.GREEN, Color.YELLOW, 1));
            }
        }, 1L, 2L);

        plugin.getScheduler().runTaskTimer(() -> {
            Location random = hunts.stream().findAny().orElse(null);
            if (random == null)
                return;

            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendMessage(color("&eThere might be an egg at " + random.getBlockX() + " " + random.getBlockZ()));
            });
        }, 0L, 20L * 60L * 15L);

        plugin.getCommand("egg").setExecutor(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (internalEvents.contains(e)) return;

        if (!e.getItemInHand().isSimilar(head))
            return;

        Location location = e.getBlockPlaced().getLocation();
        if (hunts.add(location)) {
            e.getPlayer().sendMessage(color("&eYou placed an egg here!"));
        } else {
            e.getPlayer().sendMessage(color("&cThat is already an egg location!"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Location location = e.getBlock().getLocation();

        if (hunts.remove(location)) {
            if (e.getPlayer().hasPermission("easter.hunt")) {
                e.getPlayer().sendMessage(color("&eYou removed an egg!"));
                return;
            }
            e.getPlayer().sendMessage(color("&cRight click this egg to claim it!"));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClickBlock(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = e.getClickedBlock();
        if (block == null)
            return;

        if (hunts.remove(block.getLocation())) {
            int amount = players.getOrDefault(e.getPlayer().getName(), 0);
            amount++;
            players.put(e.getPlayer().getName(), amount);
            e.getPlayer().sendMessage(color("&eYou have claimed this egg!\n&eYou claimed " + amount + " eggs! There are " + hunts.size() + " more to go!"));
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player == e.getPlayer())
                    return;

                player.sendMessage(color("&e" + e.getPlayer().getName() + " has claimed an egg! There are " + hunts.size() + " more to go!"));
            });
            block.setType(Material.AIR);
            block.getWorld().spawnParticle(Particle.TOTEM, block.getLocation(), 1);

            if (hunts.size() == 0) {
                announceWinner();
                plugin.getLogger().info("All the eggs have been collected!");
            }
        }
    }

    public void save() {
        List<Location> copy = hunts == null ? new ArrayList<>() : new ArrayList<>(hunts);
        config.set("locations", copy);
        config.set("players", players);

        super.save();
    }

    @Override
    public void loadDefaultConfig() {
        head = new ItemStack(Material.SKELETON_SKULL);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&aEaster Egg"));
            UUID uuid = UUID.randomUUID();
            try {
                PlayerProfile playerProfile = Bukkit.createProfile(uuid);
                playerProfile.setProperty(new ProfileProperty("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWNlZGRjMjNmOWQ5NmJhYWEwZDJkN2I5ZWMxODBjZDdiZWE1NDQ3ZDM5YzQyNWNhOWU0NGQ4ODA4ZWExMWVhMCJ9fX0="));
                meta.setPlayerProfile(playerProfile);
                meta.setOwnerProfile(playerProfile);
            } catch (Exception e) {
                plugin.getLogger().warning("Unable to set Easter Egg texture for head hunt... Defaulting to my head!");
                meta.setOwner("destiall");
            }
            head.setItemMeta(meta);
        }
        config.set("egg", head);
    }

    @Override
    public void disable() {
        save();
        hunts.clear();
        players.clear();

        plugin.getCommand("egg").setExecutor(null);
    }

    public int getAmount(String name) {
        return players.getOrDefault(name, 0);
    }

    public Map.Entry<String, Integer> getAmountRank(int rank) {
        List<Map.Entry<String, Integer>> sorted = players.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).collect(Collectors.toList());
        int index = rank - 1;
        if (index >= sorted.size() || index < 0) {
            return null;
        }
        return sorted.get(index);
    }

    public int getRemaining() {
        return hunts.size();
    }

    private void announceWinner() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            player.sendMessage(color("&eAll the eggs have been collected!"));
        });
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        if (!player.hasPermission("easter.hunt")) {
            player.sendMessage(color("&cYou do not have permission!"));
            return false;
        }

        if (args.length == 0) {
            player.getInventory().addItem(head.clone());
            return true;
        }

        if (args[0].equalsIgnoreCase("place")) {
            if (args.length != 3) {
                player.sendMessage(color("&cUsage: /egg place [amount] [radius / border]"));
                return false;
            }

            int amount = Integer.parseInt(args[1]);

            String type = args[2];
            double radius;
            Location center = player.getLocation();
            if (type.equalsIgnoreCase("border")) {
                radius = player.getWorld().getWorldBorder().getSize() / 2d;
                center = new Location(player.getWorld(), 0, 64, 0);
            } else {
                radius = Integer.parseInt(args[2]);
            }
            Location finalCenter = center;
            final double finalRadius = radius;

            for (int i = 0; i < amount; i++) {
                tryPlaceEgg(finalCenter, finalRadius, player);
            }

            return false;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            players.clear();
            player.sendMessage(color("&aCleared all player egg data!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            for (Location loc : hunts) {
                loc.getBlock().setType(Material.AIR);
            }

            hunts.clear();
        }

        if (args[0].equalsIgnoreCase("fix")) {
            for (Location loc : hunts) {
                if (!loc.getChunk().isLoaded()) {
                    loc.getChunk().load(true);
                }

                plugin.dispatchCommand(player, "setblock " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + blockData + " replace", false);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            for (Location loc : hunts) {
                TextComponent component = new TextComponent(color(" &e> " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tppos " + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(color("Click to teleport to egg"))));
                player.spigot().sendMessage(component);
            }
            return true;
        }
        return false;
    }

    private void tryPlaceEgg(final Location center, final double radius, final Player player) {
        double x = ((Math.random() * 2 - 1) * radius) + center.getX();
        double y = center.getWorld().getMaxHeight();
        double z = ((Math.random() * 2 - 1) * radius) + center.getZ();

        Location loc = new Location(center.getWorld(), x, y, z);
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load(true);
        }

        plugin.getScheduler().runTask(() -> {
            World world = loc.getWorld();
            if (world == null)
                return;

            RayTraceResult result = world.rayTraceBlocks(loc, new Vector(0, -1, 0), world.getMaxHeight() - world.getMinHeight(), FluidCollisionMode.ALWAYS, true);
            if (result == null || result.getHitBlock() == null) {
                tryPlaceEgg(center, radius, player);
                return;
            }

            Block hit = result.getHitBlock();
            if (hit.getType() == Material.LAVA || hit.getType() == Material.WATER) {
                tryPlaceEgg(center, radius, player);
                return;
            }

            loc.setX(hit.getX());
            loc.setY(hit.getY() + 1);
            loc.setZ(hit.getZ());

            if (hunts.contains(loc)) {
                tryPlaceEgg(center, radius, player);
                return;
            }

            Block placedBlock = loc.getBlock();
            Block placedAgainst = placedBlock.getRelative(BlockFace.DOWN);
            if (!placedBlock.canPlace(placedAgainst.getBlockData())) {
                tryPlaceEgg(center, radius, player);
                return;
            }
            BlockState replacedBlockState = placedBlock.getState();
            BlockPlaceEvent event = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, head.clone(), player, true, EquipmentSlot.HAND);
            internalEvents.add(event);
            plugin.getServer().getPluginManager().callEvent(event);
            internalEvents.remove(event);

            if (event.isCancelled()) {
                tryPlaceEgg(center, radius, player);
                return;
            }

            plugin.dispatchCommand(player, "setblock " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + blockData + " replace", false);
            player.sendMessage(color("&aPlaced an egg on " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
            hunts.add(loc);
        }, loc);
    }
}
