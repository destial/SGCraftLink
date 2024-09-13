package xyz.destiall.sgcraftlink.modules.common;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Tag;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class TagModule extends Module implements CommandExecutor {
    private final List<Tag> allTags;

    public TagModule(SGCraftLinkSpigot plugin) {
        super(plugin, "tags.yml");
        allTags = new ArrayList<>();
        setCommandExecutor("tags");
    }

    @Override
    public void enable() {
        for (String key : config.getKeys(false)) {
            Tag tag = new Tag(key, config.getString(key));
            allTags.add(tag);
        }
        plugin.getLogger().info("Registered " + allTags.size() + " tags " + new JSONArray(allTags.stream().map(Tag::getName).collect(Collectors.toList())).toString().replace("\"", ""));
    }

    @Override
    public boolean isUniversallyCompatible() {
        return true;
    }

    @Override
    public void loadDefaultConfig() {
        config.set("vip", "&dVIP");
    }

    @Override
    public void disable() {
        allTags.clear();
        unsetCommandExecutor("tags");
    }

    public List<Tag> getAllTags() {
        return allTags;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /tags [reload|list|create]"));
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            allTags.clear();
            loadConfig();
            enable();
            return false;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&bSGCraftLink Tags:"));
            for (Tag tag : allTags) {
                sender.sendMessage(color("&a - " + tag.getName() + ": " + tag.getDisplay()));
            }
            return false;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                sender.sendMessage(color("&cUsage: /tags [create] [name] [display]"));
                return false;
            }

            String name = args[1];
            String display = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            config.set(name, display);
            Tag tag = new Tag(name, display);
            allTags.add(tag);
            saveConfig();
            sender.sendMessage(color("&aAdded new tag &e" + tag.getName() + " &a(" + tag.getDisplay() + "&a)"));
            return false;
        }

        if (args[0].equalsIgnoreCase("edit")) {
            if (args.length < 3) {
                sender.sendMessage(color("&cUsage: /tags [edit] [name] [display]"));
                return false;
            }

            String name = args[1];
            if (!config.contains(name)) {
                sender.sendMessage(color("&cTag '" + name + "' doesn't exist!"));
                return false;
            }
            String display = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            config.set(name, display);
            Tag tag = allTags.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
            String old = tag.getDisplay();
            tag.setDisplay(display);
            saveConfig();
            sender.sendMessage(color("&aEdited tag &e" + tag.getName() + " &a(" + old + "&a) -> &a(" + tag.getDisplay() + "&a)"));
            return false;
        }
        return false;
    }
}
