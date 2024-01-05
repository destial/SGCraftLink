package xyz.destiall.sgcraftlink.modules.common;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Triggers extends Module implements Listener {
    private final Map<Pattern, String> triggers;

    public Triggers(SGCraftLinkSpigot plugin) {
        super(plugin, "triggers.yml");
        triggers = new HashMap<>();

        ConfigurationSection triggerSection = config.getConfigurationSection("triggers");
        if (triggerSection == null)
            return;
        for (String key : triggerSection.getKeys(false)) {
            String pattern = triggerSection.getString(key + ".pattern");
            boolean caseInsensitive = triggerSection.getBoolean(key + ".case-insensitive");
            String target = triggerSection.getString(key + ".replace");
            if (pattern == null || target == null)
                continue;
            Pattern p = Pattern.compile(pattern, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
            triggers.put(p, target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNormalChat(AsyncPlayerChatEvent e) {
        String message = e.getMessage();
        for (Map.Entry<Pattern, String> trigger : triggers.entrySet()) {
            message = trigger.getKey().matcher(message).replaceAll(trigger.getValue());
        }
        e.setMessage(message);
    }

    @Override
    public void loadDefaultConfig() {
        config.set("triggers.skull.pattern", ":skull:");
        config.set("triggers.skull.case-insensitive", true);
        config.set("triggers.skull.replace", "☠");
    }

    @Override
    public void disable() {
        triggers.clear();
    }
}
