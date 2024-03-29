package xyz.destiall.sgcraftlink.modules.hooks.utils;

import org.bukkit.configuration.ConfigurationSection;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.HashMap;
import java.util.Map;

public class Font {
    private static final String ALPHABET_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHABET_UPPER = ALPHABET_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";

    private final String name;
    private final Map<String, String> alphabets;
    private boolean ignoreCase;

    public Font(String name, ConfigurationSection section) {
        this.name = name;
        this.alphabets = new HashMap<>();
        SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).getLogger().info("Loading font " + name);

        if (section == null)
            return;

        this.ignoreCase = section.getBoolean("ignore-case");

        boolean twoCharPer = section.getBoolean("two-char-per", false);

        String lower = section.getString("lower", ALPHABET_LOWER);
        for (int i = 0; i < ALPHABET_LOWER.length(); i++) {
            alphabets.put(String.valueOf(ALPHABET_LOWER.charAt(i)), String.valueOf(twoCharPer ? (String.valueOf(lower.charAt(i*2)) + lower.charAt((i*2) + 1)) : lower.charAt(i)));
        }

        String number = section.getString("number", NUMBER);
        for (int i = 0; i < NUMBER.length(); i++) {
            alphabets.put(String.valueOf(NUMBER.charAt(i)), String.valueOf(twoCharPer ? (String.valueOf(number.charAt(i*2)) + number.charAt((i*2) + 1)) : number.charAt(i)));
        }

        if (ignoreCase)
            return;

        String upper = section.getString("upper", ALPHABET_UPPER);
        for (int i = 0; i < ALPHABET_UPPER.length(); i++) {
            alphabets.put(String.valueOf(ALPHABET_UPPER.charAt(i)), String.valueOf(twoCharPer ? (String.valueOf(upper.charAt(i * 2)) + upper.charAt((i * 2) + 1)) : upper.charAt(i)));
        }
    }

    public String format(String text) {
        String t = ignoreCase ? text.toLowerCase() : text;
        for (Map.Entry<String, String> entry : alphabets.entrySet()) {
            t = t.replace(entry.getKey(), entry.getValue());
        }
        return t;
    }

    public String getName() {
        return name;
    }
}
