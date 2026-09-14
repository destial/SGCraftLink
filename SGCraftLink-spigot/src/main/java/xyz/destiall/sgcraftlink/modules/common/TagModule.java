package xyz.destiall.sgcraftlink.modules.common;

import express.utils.Status;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.items.Tag;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class TagModule extends Module implements CommandExecutor, Listener {
    private final List<Tag> allTags;

    public TagModule(SGCraftLinkSpigot plugin) {
        super(plugin, "tags.yml");
        allTags = new ArrayList<>();
        setCommandExecutor("tags");
    }

    @Override
    public void enable() {
        for (String key : config.getKeys(false)) {
            if (key.equalsIgnoreCase("enabled"))
                continue;

            Tag tag = new Tag(key, config.getString(key));
            allTags.add(tag);
        }
        log("Registered " + allTags.size() + " tags " + new JSONArray(allTags.stream().map(Tag::getName).collect(Collectors.toList())).toString().replace("\"", ""));
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
            sender.sendMessage(color("&cUsage: /tags [reload|list|set]"));
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            allTags.clear();
            loadConfig();
            enable();
            sender.sendMessage(color("&aReloaded all tags!"));
            return false;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&bSGCraftLink Tags:"));
            for (Tag tag : allTags) {
                sender.sendMessage(color("&a - " + tag.getName() + ": " + tag.getDisplay()));
            }
            return false;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage(color("&cUsage: /tags set [name] [display]"));
                return false;
            }

            String name = args[1];
            String display = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

            boolean edited = false;
            Tag tag;
            if (config.contains(name)) {
                tag = allTags.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                if (tag == null) {
                    tag = new Tag(name, display);
                    allTags.add(tag);
                }
                tag.setDisplay(display);
                edited = true;
            } else {
                tag = new Tag(name, display);
                allTags.add(tag);
            }

            config.set(name, display);
            saveConfig();
            sender.sendMessage(color((edited ? "&aEdited tag " : "&aAdded new tag ") + "&e" + tag.getName() + " &a(" + tag.getDisplay() + "&a)"));

            DataModule dataModule = plugin.getModule(DataModule.class);
            if (dataModule == null)
                return false;

            Map<String, Object> data = SGCraftLinkCommon.createDataMap("name", tag.getName(), "display", tag.getDisplay());
            dataModule.sendProxyData(SGCraftLinkCommon.DATA_SYNC_TAGS, data);
            return false;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSyncTags(DataReceiveEvent event) {
        if (!event.getDataType().equalsIgnoreCase(SGCraftLinkCommon.DATA_SYNC_TAGS))
            return;

        event.setStatus(Status._200);

        String name = event.getJson().getString("name");
        String display = event.getJson().getString("display");

        config.set(name, display);
        Tag tag = new Tag(name, display);
        allTags.add(tag);
        saveConfig();
        plugin.getLogger().info("&aSet tag &e" + tag.getName() + " to &a(" + tag.getDisplay() + "&a)");
    }
}
