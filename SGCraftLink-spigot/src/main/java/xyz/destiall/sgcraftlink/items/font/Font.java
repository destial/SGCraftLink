package xyz.destiall.sgcraftlink.items.font;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class Font {
    private static final String ALPHABET_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHABET_UPPER = ALPHABET_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";

    private final String name;
    private final Map<String, String> lexicons;
    private boolean ignoreCase;

    public Font(String name, ConfigurationSection section) {
        this.name = name;
        this.lexicons = new HashMap<>();

        if (section == null)
            return;

        this.ignoreCase = section.getBoolean("ignore-case");

        if (section.contains("others")) {
            ConfigurationSection others = section.getConfigurationSection("others");
            if (others != null) {
                for (String key : others.getKeys(false)) {
                    lexicons.put(key, others.getString(key));
                }
            }
        }

        if (section.contains("manual")) {
            ConfigurationSection manual = section.getConfigurationSection("manual");
            if (manual != null) {
                for (String key : manual.getKeys(false)) {
                    lexicons.put(key, manual.getString(key));
                }
            }
            return;
        }

        boolean twoCharPer = section.getBoolean("two-char-per", false);

        String lower = section.getString("lower", ALPHABET_LOWER);
        for (int i = 0; i < ALPHABET_LOWER.length(); i++) {
            lexicons.put(String.valueOf(ALPHABET_LOWER.charAt(i)), String.valueOf(twoCharPer ? (String.valueOf(lower.charAt(i*2)) + lower.charAt((i*2) + 1)) : lower.charAt(i)));
        }

        String number = section.getString("number", NUMBER);
        for (int i = 0; i < NUMBER.length(); i++) {
            lexicons.put(String.valueOf(NUMBER.charAt(i)), String.valueOf(twoCharPer && section.contains("number") ? (String.valueOf(number.charAt(i*2)) + number.charAt((i*2) + 1)) : number.charAt(i)));
        }

        if (ignoreCase)
            return;

        String upper = section.getString("upper", ALPHABET_UPPER);
        for (int i = 0; i < ALPHABET_UPPER.length(); i++) {
            lexicons.put(String.valueOf(ALPHABET_UPPER.charAt(i)), String.valueOf(twoCharPer ? (String.valueOf(upper.charAt(i * 2)) + upper.charAt((i * 2) + 1)) : upper.charAt(i)));
        }
    }

    public String format(String text, boolean checkChar) {
        String t = color(ignoreCase ? text.toLowerCase() : text);
        checkChar = checkChar || text.contains(String.valueOf(ChatColor.COLOR_CHAR));
        for (Map.Entry<String, String> entry : lexicons.entrySet()) {
            if (ChatColor.getByChar(entry.getKey()) != null && checkChar) {
                int i = 0;
                while ((i = t.indexOf(entry.getKey(), i + 1)) != -1) {
                    if (i != 0 && t.charAt(i - 1) == ChatColor.COLOR_CHAR) {
                        continue;
                    }
                    t = t.substring(0, i) + entry.getValue() + t.substring(i + 1);
                }
                continue;
            }
            t = t.replace(entry.getKey(), entry.getValue());
        }
        return t;
    }

    public final String getName() {
        return name;
    }
}
