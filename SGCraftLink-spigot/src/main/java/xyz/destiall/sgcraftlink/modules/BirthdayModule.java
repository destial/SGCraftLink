package xyz.destiall.sgcraftlink.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                plugin.executeServerCommand(command.replace("{player}", player.getName()));
            }
        }
    }

    @Override
    public void disable() {
        birthdays.clear();
    }
}
