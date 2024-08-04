package xyz.destiall.sgcraftlink.modules.common;

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
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.emptyDataMap;

public class HeadHunt extends Module implements Listener, CommandExecutor {
    private final Set<Location> hunts = new HashSet<>();
    private final Map<String, Integer> players = new ConcurrentHashMap<>();
    private final Map<String, Integer> otherServerPlayers = new ConcurrentHashMap<>();
    private final List<BlockPlaceEvent> internalEvents = new ArrayList<>();
    private ItemStack head = new ItemStack(Material.SKELETON_SKULL);
    private String blockData;
    private boolean checkOtherServers = true;
    private AtomicBoolean dirty = new AtomicBoolean(false);

    public HeadHunt(SGCraftLinkSpigot plugin) {
        super(plugin, "hunt.yml");
    }

    public Set<Location> getHunts() {
        return hunts;
    }

    public Map<String, Integer> getScore() {
        return players;
    }

    public Map<String, Integer> getOtherServersScore() {
        return otherServerPlayers;
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        switch (e.getDataType()) {
            case SGCraftLinkCommon.DATA_GET_HEADHUNT: {
                JSONObject res = new JSONObject();
                res.put("remaining", getRemaining());
                JSONObject players = new JSONObject();
                for (Map.Entry<String, Integer> score : getScore().entrySet()) {
                    players.put(score.getKey(), score.getValue());
                }
                res.put("score", players);
                e.setResponse(res);
                break;
            }
            case SGCraftLinkCommon.DATA_START_HEADHUNT: {
                setCheckOtherServers(true);
                break;
            }
            case SGCraftLinkCommon.DATA_END_HEADHUNT: {
                setCheckOtherServers(false);
                announceWinner();
                break;
            }
            case SGCraftLinkCommon.DATA_CLEAR_SCORE_HEADHUNT: {
                getScore().clear();
                getOtherServersScore().clear();
                break;
            }
            case SGCraftLinkCommon.DATA_REMOVE_HEAD_HEADHUNT: {
                for (Location loc : getHunts()) {
                    loc.getBlock().setType(Material.AIR);
                }

                getHunts().clear();
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (internalEvents.contains(e)) return;

        if (!e.getItemInHand().isSimilar(head))
            return;

        Location location = e.getBlockPlaced().getLocation();
        if (hunts.add(location)) {
            dirty.compareAndSet(false, true);
            e.getPlayer().sendMessage(color("&eYou placed an egg here!"));
        } else {
            e.getPlayer().sendMessage(color("&cThat is already an egg location!"));
        }

        if (hunts.size() == 1) {
            DataModule module = plugin.getModule(DataModule.class);
            if (module == null)
                return;

            module.sendProxyData(SGCraftLinkCommon.DATA_START_HEADHUNT, new HashMap<>(), (s) -> {});
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Location location = e.getBlock().getLocation();

        if (hunts.contains(location)) {
            if (e.getPlayer().hasPermission("easter.hunt")) {
                e.getPlayer().sendMessage(color("&eYou removed an egg!"));
                hunts.remove(location);
                dirty.compareAndSet(false, true);

                DataModule module = plugin.getModule(DataModule.class);
                if (module == null)
                    return;

                module.sendProxyData(SGCraftLinkCommon.DATA_END_HEADHUNT, emptyDataMap(), (s) -> {});
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
            dirty.compareAndSet(false, true);
            int amount = players.getOrDefault(e.getPlayer().getName(), 0);
            amount++;
            players.put(e.getPlayer().getName(), amount);
            e.getPlayer().sendMessage(color("&eYou have claimed this egg!\n&eYou claimed " + amount + " eggs!" + (!hunts.isEmpty() ? " There are " + hunts.size() + " more to go!" : "")));
            e.getPlayer().sendTitle(color("&eYou claimed " + amount + " eggs!"), color(!hunts.isEmpty() ? " There are " + hunts.size() + " more to go!" : "All is collected!"));
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player == e.getPlayer())
                    return;

                player.sendMessage(color("&e" + e.getPlayer().getName() + " has claimed an egg!" + (!hunts.isEmpty() ? " There are " + hunts.size() + " more to go!" : "")));
                player.sendTitle(color("&e" + e.getPlayer().getName() + " has claimed an egg!"), color(!hunts.isEmpty() ? " There are " + hunts.size() + " more to go!" : "All is collected!"));
                if (checkOtherServers && hunts.isEmpty()) {
                    player.sendMessage(color("&aAll eggs in this server have been collected! Check out the other servers to see if there are any eggs!"));
                }
            });

            block.setType(Material.AIR);
            block.getWorld().spawnParticle(Particle.TOTEM, block.getLocation(), 1);

            getAllRemaining().whenComplete((remaining, err) -> {
                if (remaining == 0) {
                    announceWinner();
                    plugin.getLogger().info("All the eggs have been collected!");

                    DataModule module = plugin.getModule(DataModule.class);
                    if (module == null)
                        return;

                    module.sendProxyData(SGCraftLinkCommon.DATA_END_HEADHUNT, emptyDataMap(), (s) -> {});
                }
            });
        }
    }

    public boolean isCheckOtherServers() {
        return checkOtherServers;
    }

    public void setCheckOtherServers(boolean checkOtherServers) {
        this.checkOtherServers = checkOtherServers;
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
        if (dirty.get()) {
            save();
        }
        hunts.clear();
        players.clear();

        plugin.getCommand("egg").setExecutor(null);
    }

    @Override
    public void enable() {
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
            if (random == null) {
                if (checkOtherServers) {
                    DataModule module = plugin.getModule(DataModule.class);
                    if (module == null)
                        return;

                    module.sendProxyData(SGCraftLinkCommon.DATA_GET_HEADHUNT, emptyDataMap(), (result) -> {
                        if (result.isSuccess()) {
                            JSONObject obj = result.getJSON();
                            JSONObject score = obj.getJSONObject("score");
                            for (String player : score.keySet()) {
                                otherServerPlayers.put(player, score.getInt(player));
                            }

                            if (obj.getInt("total") != 0) {
                                plugin.getServer().getOnlinePlayers().forEach(player -> {
                                    player.sendMessage(color("&eThere are still more eggs in other servers!"));
                                    JSONObject servers = obj.getJSONObject("servers");
                                    for (String server : obj.keySet()) {
                                        player.sendMessage(color("&e" + server + ": " + servers.getInt(server) + " left"));
                                    }
                                });
                            } else {
                                checkOtherServers = false;
                            }
                        }
                    });
                }
                return;
            }

            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendMessage(color("&eThere might be an egg at " + random.getBlockX() + " " + random.getBlockZ()));
            });
        }, 20L * 10, 20L * 60L * 15L);

        plugin.getCommand("egg").setExecutor(this);
    }

    public int getAmount(String name) {
        Map<String, Integer> merged = new HashMap<>(players);
        for (Map.Entry<String, Integer> other : otherServerPlayers.entrySet()) {
            merged.merge(other.getKey(), other.getValue(), Integer::sum);
        }
        return merged.getOrDefault(name, 0);
    }

    public Map.Entry<String, Integer> getAmountRank(int rank) {
        Map<String, Integer> merged = new HashMap<>(players);
        for (Map.Entry<String, Integer> other : otherServerPlayers.entrySet()) {
            merged.merge(other.getKey(), other.getValue(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = merged.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).collect(Collectors.toList());
        int index = rank - 1;
        if (index >= sorted.size() || index < 0) {
            return null;
        }
        return sorted.get(index);
    }

    public CompletableFuture<Integer> getAllRemaining() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        DataModule module = plugin.getModule(DataModule.class);
        if (module == null || !isCheckOtherServers()) {
            future.complete(getRemaining());
            return future;
        }
        module.sendProxyData(SGCraftLinkCommon.DATA_GET_HEADHUNT, emptyDataMap(), (result) -> {
            if (result.getStatus() == 200) {
                JSONObject obj = result.getJSON();
                int total = obj.getInt("total") + getRemaining();
                future.complete(total);
                return;
            }
            future.complete(getRemaining());
        });
        return future;
    }

    public int getRemaining() {
        return hunts.size();
    }

    public void announceWinner() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            player.sendMessage(color("&eAll the eggs have been collected!"));
            player.sendTitle(color("&eAll the eggs have been collected!"), color("&aThank you for playing!"));
        });

        DataModule module = plugin.getModule(DataModule.class);
        if (module == null)
            return;

        module.sendProxyData(SGCraftLinkCommon.DATA_GET_HEADHUNT, emptyDataMap(), (result) -> {
            if (result.getStatus() == 200) {
                JSONObject obj = result.getJSON();
                JSONObject score = obj.getJSONObject("score");
                for (String player : score.keySet()) {
                    otherServerPlayers.put(player, score.getInt(player));
                }

                Map.Entry<String, Integer> first = getAmountRank(1);

                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    player.sendMessage(color("&eThe winner is " + first.getKey() + " with " + first.getValue() + " collected!"));
                    player.sendTitle(color("&eWinner: " + first.getKey()), color("Collected: " + first.getValue() + "!"));
                });
            }
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

            DataModule module = plugin.getModule(DataModule.class);
            if (module == null)
                return true;

            module.sendProxyData(SGCraftLinkCommon.DATA_START_HEADHUNT, new HashMap<>(), (s) -> {});
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            players.clear();
            otherServerPlayers.clear();
            player.sendMessage(color("&aCleared all player egg data!"));

            DataModule module = plugin.getModule(DataModule.class);
            if (module == null)
                return true;

            module.sendProxyData(SGCraftLinkCommon.DATA_CLEAR_SCORE_HEADHUNT, emptyDataMap(), (s) -> {});
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            for (Location loc : hunts) {
                loc.getBlock().setType(Material.AIR);
            }

            hunts.clear();

            DataModule module = plugin.getModule(DataModule.class);
            if (module == null)
                return true;

            module.sendProxyData(SGCraftLinkCommon.DATA_REMOVE_HEAD_HEADHUNT, emptyDataMap(), (s) -> {});
            return true;
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
            dirty.compareAndSet(false, true);
        }, loc);
    }
}
