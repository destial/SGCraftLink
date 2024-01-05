package xyz.destiall.sgcraftlink.gamemode;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.OPDGroup;
import xyz.destiall.sgcraftlink.items.Pair;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class OnePersonDeath extends Module implements Listener, CommandExecutor, TabExecutor {
    private final Set<OPDGroup> groups;
    private final Set<UUID> dyingCache;

    public OnePersonDeath(SGCraftLinkSpigot plugin) {
        super(plugin, "one-person-death.yml");
        plugin.getCommand("opd").setExecutor(this);
        this.groups = new HashSet<>();
        this.dyingCache = ConcurrentHashMap.newKeySet();

        ConfigurationSection groupSection = config.getConfigurationSection("groups");
        if (groupSection == null)
            return;

        for (String key : groupSection.getKeys(false)) {
            ConfigurationSection group = groupSection.getConfigurationSection(key);
            if (group == null)
                continue;

            OPDGroup opdGroup = new OPDGroup();
            for (String name : group.getKeys(false)) {
                UUID uuid = UUID.fromString(group.getString(name));
                Pair<UUID, String> pair = new Pair<>(uuid, name);
                opdGroup.getMembers().add(pair);
            }

            groups.add(opdGroup);
        }
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
        config.set("use-party-hook", true);
        config.set("everyone", false);
        config.set("same-world", true);
    }

    @Override
    public void save() {
        if (isEveryone()) {
            super.save();
            return;
        }

        if (isUsingParties()) {
            super.save();
            return;
        }

        if (groups != null) {
            int i = 1;
            for (OPDGroup group : groups) {
                for (Pair<UUID, String> member : group.getMembers()) {
                    config.set("groups." + i + "." + member.getValue(), member.getKey());
                }
                i++;
            }
        }

        super.save();
    }

    @Override
    public void disable() {
        plugin.getCommand("opd").setExecutor(null);
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled");
    }

    public boolean isUsingParties() {
        return config.getBoolean("use-party-hook") && plugin.getServer().getPluginManager().isPluginEnabled("Parties");
    }

    public boolean isEveryone() {
        return config.getBoolean("everyone", false);
    }

    public boolean isSameWorld() {
        return config.getBoolean("same-world", true);
    }

    public OPDGroup getGroup(Player player) {
        if (isEveryone()) {
            OPDGroup group = new OPDGroup();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                group.getMembers().add(new Pair<>(online.getUniqueId(), online.getName()));
            }

            return group;
        }

        if (isUsingParties()) {
            Party party = Parties.getApi().getPartyOfPlayer(player.getUniqueId());
            if (party == null)
                return null;

            UUID leader = party.getLeader();
            if (leader == null)
                return null;

            OPDGroup group = new OPDGroup();
            Pair<UUID, String> pair = new Pair<>(party.getLeader(), plugin.getServer().getOfflinePlayer(leader).getName());
            group.getMembers().add(pair);
            for (UUID member : party.getMembers()) {
                if (group.getLeader().getKey().equals(member))
                    continue;

                group.getMembers().add(new Pair<>(member, plugin.getServer().getOfflinePlayer(member).getName()));
            }
            return group;
        }

        return groups.stream().filter(g -> g.getMembers().stream().anyMatch(p -> p.getKey().equals(player.getUniqueId()))).findFirst().orElse(null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!isEnabled())
            return;

        if (dyingCache.remove(e.getPlayer().getUniqueId())) {
            e.setKeepInventory(true);
            return;
        }

        OPDGroup group = getGroup(e.getPlayer());
        if (group == null)
            return;

        List<Pair<UUID, String>> filtered = group.getMembers().stream().filter(pair -> !pair.getKey().equals(e.getPlayer().getUniqueId())).collect(Collectors.toList());
        for (Pair<UUID, String> pair : filtered) {
            Player member = plugin.getServer().getPlayer(pair.getKey());
            if (member == null)
                continue;

            if (isSameWorld() && !member.getWorld().equals(e.getPlayer().getWorld()))
                continue;

            dyingCache.add(member.getUniqueId());
            member.setKiller(e.getPlayer());
            member.setHealth(0);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /opd [join|leave|accept|deny|kick|info" + (sender.hasPermission("opd.admin") ? "|toggle|everyone|party|world" : "") + "]"));
            return false;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!sender.hasPermission("opd.admin")) {
                sender.sendMessage(color("&cYou do not have permission to use this command!"));
                return false;
            }

            config.set("enabled", !isEnabled());
            sender.sendMessage(color("&eToggled OnePersonDeath: " + (isEnabled() ? "&a&lENABLED" : "&c&lDISABLED")));

            save();
            return false;
        }

        if (args[0].equalsIgnoreCase("everyone")) {
            if (!sender.hasPermission("opd.admin")) {
                sender.sendMessage(color("&cYou do not have permission to use this command!"));
                return false;
            }

            config.set("everyone", !isEveryone());
            sender.sendMessage(color("&eToggled OnePersonDeath everyone: " + (isEveryone() ? "&a&lENABLED" : "&c&lDISABLED")));

            save();
            return false;
        }

        if (args[0].equalsIgnoreCase("party")) {
            if (!sender.hasPermission("opd.admin")) {
                sender.sendMessage(color("&cYou do not have permission to use this command!"));
                return false;
            }

            config.set("use-party-hook", !config.getBoolean("use-party-hook"));
            sender.sendMessage(color("&eToggled OnePersonDeath party: " + (isUsingParties() ? "&a&lENABLED" : "&c&lDISABLED")));

            save();
            return false;
        }

        if (args[0].equalsIgnoreCase("world")) {
            if (!sender.hasPermission("opd.admin")) {
                sender.sendMessage(color("&cYou do not have permission to use this command!"));
                return false;
            }

            config.set("same-world", !isSameWorld());
            sender.sendMessage(color("&eToggled OnePersonDeath same world: " + (isSameWorld() ? "&a&lENABLED" : "&c&lDISABLED")));

            save();
            return false;
        }

        if (!isEnabled()) {
            sender.sendMessage(color("&cThis game-mode is not enabled right now!"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cYou have to be a player to use this command!"));
            return false;
        }

        if (isEveryone()) {
            sender.sendMessage(color("&cThis game-mode is on everyone mode!"));
            return false;
        }

        if (isUsingParties()) {
            sender.sendMessage(color("&cThis game-mode is using parties as groups!"));
            return false;
        }

        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("join")) {
            OPDGroup group = getGroup(player);
            if (group != null) {
                sender.sendMessage(color("&cYou are already a part of a group:"));
                for (Pair<UUID, String> pair : group.getMembers()) {
                    if (pair.getKey().equals(player.getUniqueId()))
                        continue;

                    sender.sendMessage(color(" &c- " + pair.getValue()));
                }
                return false;
            }

            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /opd join [player]"));
                return false;
            }

            Player other = plugin.getServer().getPlayer(args[1]);
            if (other == null) {
                sender.sendMessage(color("&cPlayer not found!"));
                return false;
            }

            OPDGroup otherGroup = getGroup(other);
            if (otherGroup == null) {
                otherGroup = new OPDGroup();
                otherGroup.getMembers().add(new Pair<>(other.getUniqueId(), other.getName()));
                groups.add(otherGroup);
            }

            if (otherGroup.getInvites().stream().anyMatch(p -> p.getKey().equals(player.getUniqueId()))) {
                sender.sendMessage(color("&cYou have already asked to join this group!"));
                return false;
            }

            Pair<UUID, String> invite = new Pair<>(player.getUniqueId(), player.getName());
            otherGroup.getInvites().add(invite);
            TextComponent component = Component.text(color("&b" + player.getName() + " wants to join your group!"));
            component.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text(color("&aClick to accept"))));
            component.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/opd accept " + player.getName()));
            other.sendMessage(component);

            OPDGroup finalGroup = otherGroup;
            plugin.getScheduler().runTaskLater(() -> {
                if (finalGroup.getInvites().remove(invite)) {
                    sender.sendMessage(color("&cYour request to join " + other.getName() + " has expired!"));
                }
            }, 1000L * 60);
            return false;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            OPDGroup group = getGroup(player);
            if (group == null) {
                sender.sendMessage(color("&cYou do not have a group!"));
                return false;
            }

            if (!group.getLeader().getKey().equals(player.getUniqueId())) {
                sender.sendMessage(color("&cYou are not the leader of this group! " + group.getLeader().getValue() + " is the leader!"));
                return false;
            }

            if (group.getInvites().isEmpty()) {
                sender.sendMessage(color("&cYou do not have any invites!"));
                return false;
            }

            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /opd accept [player]"));
                return false;
            }

            Player other = plugin.getServer().getPlayer(args[1]);
            if (other == null) {
                sender.sendMessage(color("&cPlayer not found!"));
                return false;
            }

            Pair<UUID, String> invite = group.getInvites().stream().filter(p -> p.getKey().equals(other.getUniqueId())).findFirst().orElse(null);
            if (invite == null) {
                sender.sendMessage("&cPlayer does not have an invite to join your group!");
                return false;
            }

            group.getInvites().remove(invite);
            group.getMembers().add(invite);
            for (Pair<UUID, String> pair : group.getMembers()) {
                Player member = plugin.getServer().getPlayer(pair.getKey());
                if (member == null)
                    continue;

                member.sendMessage(color("&b" + pair.getValue() + " has joined this group!"));
            }

            save();
            return false;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            OPDGroup group = getGroup(player);
            if (group == null) {
                sender.sendMessage(color("&cYou do not have a group!"));
                return false;
            }

            if (!group.getLeader().getKey().equals(player.getUniqueId())) {
                sender.sendMessage(color("&cYou are not the leader of this group! " + group.getLeader().getValue() + " is the leader!"));
                return false;
            }

            if (group.getInvites().isEmpty()) {
                sender.sendMessage(color("&cYou do not have any invites!"));
                return false;
            }

            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /opd deny [player]"));
                return false;
            }

            Player other = plugin.getServer().getPlayer(args[1]);
            if (other == null) {
                sender.sendMessage(color("&cPlayer not found!"));
                return false;
            }

            Pair<UUID, String> invite = group.getInvites().stream().filter(p -> p.getKey().equals(other.getUniqueId())).findFirst().orElse(null);
            if (invite == null) {
                sender.sendMessage("&cPlayer does not have an invite to join your group!");
                return false;
            }

            group.getInvites().remove(invite);
            other.sendMessage(color("&cYou have been denied to join " + sender.getName() + "'s group!"));
            sender.sendMessage(color("&bYou denied " + invite.getValue() + " to join!"));
            return false;
        }

        if (args[0].equalsIgnoreCase("kick")) {
            OPDGroup group = getGroup(player);
            if (group == null) {
                sender.sendMessage(color("&cYou do not have a group!"));
                return false;
            }

            if (!group.getLeader().getKey().equals(player.getUniqueId())) {
                sender.sendMessage(color("&cYou are not the leader of this group! " + group.getLeader().getValue() + " is the leader!"));
                return false;
            }

            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /opd kick [player]"));
                return false;
            }

            Pair<UUID, String> member = group.getMembers().stream().filter(p -> p.getValue().equalsIgnoreCase(args[1])).findFirst().orElse(null);
            if (member == null) {
                sender.sendMessage(color("&cPlayer is not a member!"));
                return false;
            }

            group.getMembers().remove(member);
            sender.sendMessage(color("&bYou have kicked " + member.getValue()));
            Player onlineMember = plugin.getServer().getPlayer(member.getKey());
            if (onlineMember != null) {
                onlineMember.sendMessage(color("&c" + player.getName() + " has kicked you from their group!"));
            }

            save();
            return false;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            OPDGroup group = getGroup(player);
            if (group == null) {
                sender.sendMessage(color("&cYou do not have a group!"));
                return false;
            }

            Pair<UUID, String> member = group.getMembers().stream().filter(p -> p.getKey().equals(player.getUniqueId())).findFirst().orElse(null);
            if (member == null) {
                sender.sendMessage(color("&cThis error cannot happen!"));
                return false;
            }

            group.getMembers().remove(member);
            sender.sendMessage(color("&bYou have left your current group!"));

            save();
            return false;
        }

        if (args[0].equalsIgnoreCase("info")) {
            OPDGroup group = getGroup(player);
            if (group == null) {
                sender.sendMessage(color("&cYou do not have a group!"));
                return false;
            }

            sender.sendMessage(color("&bYour group:" + (group.getLeader().getKey().equals(player.getUniqueId()) ? " (You are the leader)" : "")));
            for (Pair<UUID, String> pair : group.getMembers()) {
                if (pair.getKey().equals(player.getUniqueId()))
                    continue;

                sender.sendMessage(color(" &b- " + pair.getValue() + (group.getLeader().getKey().equals(pair.getKey()) ? " (Leader)" : "")));
            }

            return false;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return args.length == 1 ? Stream.of("join", "leave", "accept", "deny", "kick", "info", (sender.hasPermission("opd.admin") ? "world" : ""), (sender.hasPermission("opd.admin") ? "party" : ""), (sender.hasPermission("opd.admin") ? "everyone" : ""), (sender.hasPermission("opd.admin") ? "toggle" : "")).distinct().filter(c -> c.startsWith(args[0].toLowerCase())).collect(Collectors.toList()) :
                args.length == 2 && Stream.of("join", "accept", "deny", "kick").anyMatch(c -> args[0].equalsIgnoreCase(c)) ? plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList()) :
                        Collections.emptyList();
    }
}
