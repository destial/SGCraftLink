package xyz.destiall.sgcraftlink.modules.common;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.items.packets.PacketArmorStand;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class ChatBubble extends Module implements Listener, CommandExecutor {

    private class Chat {
        private Scheduler.Task teleport;
        private Map<Scheduler.TaskRunnable, List<PacketArmorStand>> lines;

        private Chat(Player player) {
            init(player);
        }

        private void init(Player player) {
            lines = new ConcurrentHashMap<>();
            teleport = plugin.getScheduler().runTaskTimer(() -> {
                Location loc = player.getEyeLocation();
                lines.forEach((task, l) -> l.forEach(as -> as.teleport(loc)));
            }, player, 0L, 1L);
        }

        public void clear() {
            lines.forEach((task, list) -> {
                list.forEach(PacketArmorStand::remove);
                list.clear();
                task.cancel();
            });
            lines.clear();
            teleport.cancel();
        }
    }

    private final Map<Player, Chat> chatBubbles = new ConcurrentHashMap<>();

    public ChatBubble(SGCraftLinkSpigot plugin) {
        super(plugin, "chat-bubble.yml");
        setCommandExecutor("chatbubble");
    }

    public void disable() {
        chatBubbles.values().forEach(Chat::clear);
        chatBubbles.clear();
        unsetCommandExecutor("chatbubble");
    }

    @Override
    public void enable() {}

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo().getWorld() == e.getFrom().getWorld())
            return;

        Player player = e.getPlayer();
        Chat playerBubbles = chatBubbles.remove(player);
        if (playerBubbles == null)
            return;

        playerBubbles.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Chat playerBubbles = chatBubbles.remove(player);
        if (playerBubbles == null)
            return;

        playerBubbles.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent e) {
        chatMessage(e.getPlayer(), e.getMessage());
    }

    public void chatMessage(Player player, String message) {
        if (!config.getBoolean("bubble.enabled", false))
            return;

        if (player.getGameMode() == GameMode.SPECTATOR)
            return;

        plugin.getScheduler().runTask(() -> {
            final int max = config.getInt("bubble.max-length-per-line", 15);
            final int maxLines = config.getInt("bubble.max-lines", 5);
            final double radius = config.getDouble("bubble.radius", 30);
            final double spacing = config.getDouble("bubble.spacing", 0.15);
            final double height = config.getDouble("bubble.height-offset", 0.5);
            final double disappear = config.getDouble("bubble.disappear-seconds", 3d);

            String text = ChatColor.stripColor(message);
            List<String> lines = new ArrayList<>();

            while (!text.isEmpty()) {
                if (text.length() > max) {
                    String part = text.substring(0, max);
                    int firstSpace = part.lastIndexOf(" ");
                    if (firstSpace != -1) {
                        lines.add(part.substring(0, firstSpace).trim());
                        text = text.substring(firstSpace).trim();
                    } else {
                        lines.add(part.trim());
                        text = text.substring(max).trim();
                    }
                } else {
                    lines.add(text.trim());
                    break;
                }
            }

            if (lines.size() > maxLines) {
                lines.set(maxLines - 1, lines.get(maxLines - 1) + "...");
                while (lines.size() > maxLines) {
                    lines.remove(lines.size() - 1);
                }
            }

            String color = String.valueOf(ChatColor.WHITE);
            int currentWeight = -1;
            for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
                String p = perm.getPermission();
                if (p.startsWith("chatbubbles.color.") && perm.getValue()) {
                    int index = p.lastIndexOf(".");
                    int weight = Integer.parseInt(p.substring(index + 1));
                    if (weight < currentWeight)
                        continue;
                    currentWeight = weight;
                    color = color(p.substring("chatbubbles.color.".length(), index));
                }
            }

            Location loc = player.getEyeLocation();
            Collection<Player> nearby = loc.getWorld().getNearbyPlayers(loc, radius,
                    p -> config.getBoolean("data." + p.getName(), true) &&
                            (p == player || p.canSee(player)));

            AtomicDouble deltaY = new AtomicDouble(lines.size() * spacing);
            String finalColor = color;
            List<PacketArmorStand> bubbles = lines.stream().map(line -> {
                PacketArmorStand as = PacketArmorStand.get(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
                as.getOffset().setY(height + deltaY.getAndAdd(-spacing));
                as.setPos(loc.getX() + as.getOffset().getX(), loc.getY() + as.getOffset().getY(), loc.getZ() + as.getOffset().getZ());
                as.setCustomName(finalColor + (player.hasPermission("chatbubbles.color") ? color(line) : line));
                as.setCustomNameVisible(true);
                as.setInvisible(true);
                as.setSmall(true);
                as.setGravity(false);
                for (Player p : nearby) {
                    as.createFor(p);
                }
                return as;
            }).collect(Collectors.toList());

            Chat playerBubbles = chatBubbles.getOrDefault(player, chatBubbles.putIfAbsent(player, new Chat(player)));
            int current = lines.size();
            for (Map.Entry<Scheduler.TaskRunnable, List<PacketArmorStand>> entry : playerBubbles.lines.entrySet()) {
                if ((current += entry.getValue().size()) > maxLines) {
                    entry.getKey().cancel();
                    continue;
                }
                for (PacketArmorStand as : entry.getValue()) {
                    as.getOffset().setY(as.getOffset().getY() + (lines.size() * spacing));
                }
            }

            Scheduler.TaskRunnable runnable = new Scheduler.TaskRunnable() {
                @Override
                public void run() {
                    remove();
                }

                public void remove() {
                    bubbles.forEach(PacketArmorStand::remove);
                    bubbles.clear();
                    playerBubbles.lines.remove(this);
                    if (playerBubbles.lines.isEmpty()) {
                        Chat c = chatBubbles.remove(player);
                        if (c != null) {
                            c.clear();
                        }
                    }

                    System.gc();
                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    remove();
                    super.cancel();
                }
            };

            runnable.runTaskLater(plugin.getScheduler(), player, (long) (20L * disappear));
            playerBubbles.lines.put(runnable, bubbles);
        }, player);
    }

    @Override
    public void loadDefaultConfig() {
        config.set("bubble.enabled", true);
        config.set("bubble.max-length-per-line", 15);
        config.set("bubble.height-offset", 0.5);
        config.set("bubble.spacing", 0.25);
        config.set("bubble.disappear-seconds", 3);
        config.set("bubble.radius", 30);
        config.set("bubble.max-lines", 5);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        boolean state = config.getBoolean("data." + player.getName(), true);
        state = !state;
        config.set("data." + player.getName(), state);

        player.sendMessage(color("&eYou have toggled chat bubbles: " + (state ? "&a&lON" : "&c&lOFF")));

        saveConfig();
        return false;
    }
}
