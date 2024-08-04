package xyz.destiall.sgcraftlink.modules.common;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class BirthdayModule extends Module implements Listener {
    private final Map<String, Boolean> birthdays = new HashMap<>();
    public BirthdayModule(SGCraftLinkSpigot plugin) {
        super(plugin, "birthday.yml");
    }

    @Override
    public void loadDefaultConfig() {
        config.set("birthday.commands", Arrays.asList("command here when a player's birthday", "another command here"));
        config.set("birthday.messages-to-player", Arrays.asList("&bHappy Birthday, {player}!", "&bWe wish you a good life!"));
        config.set("birthday.title-message", "&bHappy Birthday, {player}!");
        config.set("birthday.subtitle-message", "&bWe wish you a good life!");
        config.set("birthday.broadcasts", Arrays.asList("&bToday is {player}'s birthday!", "&b{player} is now {age} years old!"));
    }

    public void addBirthday(String name, boolean celebrated) {
        birthdays.putIfAbsent(name, celebrated);
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        if (!e.getDataType().equals(SGCraftLinkCommon.DATA_BIRTHDAY))
            return;

        String name = e.getJson().getString("name");
        boolean celebrated = e.getJson().getBoolean("celebrated");

        Player online = plugin.getServer().getPlayer(name);
        if (online != null) {
            celebrate(online, celebrated);
        } else {
            addBirthday(name, celebrated);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (birthdays.containsKey(e.getPlayer().getName())) {
            celebrate(e.getPlayer(), birthdays.remove(e.getPlayer().getName()));
        }
    }

    public void celebrate(Player player, boolean celebrated) {
        List<String> messages = config.getStringList("birthday.messages-to-player");
        List<String> broadcasts = config.getStringList("birthday.broadcasts");
        List<String> commands = config.getStringList("birthday.commands");

        String title = config.getString("birthday.title-message", "");
        String subtitle = config.getString("birthday.subtitle-message", "");

        player.sendTitle(color(title.replace("{player}", player.getName())),
                color(subtitle.replace("{player}", player.getName())));

        for (String message : messages) {
            player.sendMessage(color(message));
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online == player)
                continue;

            for (String broadcast : broadcasts) {
                online.sendMessage(color(broadcast.replace("{player}", player.getName())));
            }
        }

        if (!celebrated) {
            for (String command : commands) {
                plugin.dispatchCommand(command.replace("{player}", player.getName()));
            }
        }
    }

    @Override
    public void disable() {
        birthdays.clear();
    }

    @Override
    public void enable() {
        plugin.getScheduler().runTaskLater(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (birthdays.containsKey(player.getName())) {
                    celebrate(player, birthdays.remove(player.getName()));
                }
            }
        }, 60L);
    }

    @Override
    public boolean isUniversallyCompatible() {
        return true;
    }
}
