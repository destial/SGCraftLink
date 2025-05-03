package xyz.destiall.sgcraftlink.modules.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.FontUserSettings;
import xyz.destiall.sgcraftlink.items.PlayerReputation;
import xyz.destiall.sgcraftlink.items.Tag;
import xyz.destiall.sgcraftlink.items.font.Font;
import xyz.destiall.sgcraftlink.listeners.PlaytimeGetter;
import xyz.destiall.sgcraftlink.modules.common.Fonter;
import xyz.destiall.sgcraftlink.modules.common.HeadHunt;
import xyz.destiall.sgcraftlink.modules.common.TagModule;
import xyz.destiall.sgcraftlink.modules.survival.MusicHook;
import xyz.destiall.sgcraftlink.modules.vanilla.PiggyBankModule;
import xyz.destiall.sgcraftlink.modules.vanilla.ReputationModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class PAPIHook extends PlaceholderExpansion implements Relational {
    private final SGCraftLinkSpigot plugin;
    private final Fonter fonter;
    private final PiggyBankModule piggyBank;
    private final HeadHunt headHunt;
    private final TagModule tagModule;
    private final ReputationModule reputationModule;
    private final LuckPermsHook luckpermsModule;
    private final MusicHook musicHook;
    private final PlaytimeGetter playtimeHook;

    public PAPIHook(SGCraftLinkSpigot plugin) {
        this.plugin = plugin;
        this.fonter = plugin.getModule(Fonter.class);
        this.piggyBank = plugin.getModule(PiggyBankModule.class);
        this.headHunt = plugin.getModule(HeadHunt.class);
        this.tagModule = plugin.getModule(TagModule.class);
        this.reputationModule = plugin.getModule(ReputationModule.class);
        this.luckpermsModule = plugin.getModule(LuckPermsHook.class);
        this.musicHook = plugin.getModule(MusicHook.class);
        this.playtimeHook = plugin.getModule(PlaytimeGetter.class);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sgcraftlink";
    }

    @Override
    public @NotNull String getAuthor() {
        return "sgcnetwork";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    private static final Pattern RELATIONAL_BRACKET_PLACEHOLDER_PATTERN = Pattern
            .compile("[{](rel_)([^{}]+)[}]");

    public static String setBracketRelationalPlaceholders(Player one, Player two, String text) {
        final Matcher matcher = RELATIONAL_BRACKET_PLACEHOLDER_PATTERN.matcher(text);

        while (matcher.find()) {
            final String format = matcher.group(2);
            final int index = format.indexOf("_");

            if (index <= 0) {
                continue;
            }

            String identifier = format.substring(0, index).toLowerCase(Locale.ROOT);
            String params = format.substring(index + 1);
            final PlaceholderExpansion expansion = PlaceholderAPIPlugin.getInstance()
                    .getLocalExpansionManager().getExpansion(identifier);

            if (!(expansion instanceof Relational)) {
                continue;
            }

            final String value = ((Relational) expansion).onPlaceholderRequest(one, two, params);

            if (value != null) {
                text = text.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(value));
            }
        }

        return text;
    }

    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String params) {
        try {
            if (params.equalsIgnoreCase("texturepack")) {
                return String.valueOf(viewer.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED ||
                        viewer.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);
            }

            if (params.startsWith("ternary_")) {
                String source = params.substring("ternary_".length()); // condition?true:false (%sgcraftlink_ternary_{}?{}:{}%)
                if (!source.contains("?") && !source.contains(":")) {
                    return "Invalid expression!";
                }
                boolean stripColors = false;
                if (source.startsWith("strip_")) {
                    source = params.substring("strip_".length());
                    stripColors = true;
                }
                int qIndex = source.indexOf("?");
                while (source.charAt(qIndex - 1) == '\\') {
                    source = source.substring(0, qIndex - 1) + source.substring(qIndex);
                    qIndex = source.indexOf("?", qIndex);
                }
                String condition = source.substring(0, qIndex);
                int cIndex = source.indexOf(":");
                while (source.charAt(cIndex - 1) == '\\') {
                    source = source.substring(0, cIndex - 1) + source.substring(cIndex);
                    cIndex = source.indexOf(":", cIndex);
                }

                String ifTrue = source.substring(qIndex + 1, cIndex);
                String ifFalse = source.substring(cIndex + 1);

                String leftCondition;
                String rightCondition;
                String operator;
                if (condition.contains("==")) {
                    leftCondition = condition.substring(0, condition.indexOf("=="));
                    rightCondition = condition.substring(condition.indexOf("==") + 2);
                    operator = "==";
                } else if (condition.contains("~=")) {
                    leftCondition = condition.substring(0, condition.indexOf("~="));
                    rightCondition = condition.substring(condition.indexOf("~=") + 2);
                    operator = "~=";
                } else if (condition.contains("!=")) {
                    leftCondition = condition.substring(0, condition.indexOf("!="));
                    rightCondition = condition.substring(condition.indexOf("!=") + 2);
                    operator = "!=";
                } else if (condition.contains("<=")) {
                    leftCondition = condition.substring(0, condition.indexOf("<="));
                    rightCondition = condition.substring(condition.indexOf("<=") + 2);
                    operator = "<=";
                } else if (condition.contains(">=")) {
                    leftCondition = condition.substring(0, condition.indexOf(">="));
                    rightCondition = condition.substring(condition.indexOf(">=") + 2);
                    operator = ">=";
                } else if (condition.contains("<")) {
                    leftCondition = condition.substring(0, condition.indexOf("<"));
                    rightCondition = condition.substring(condition.indexOf("<") + 1);
                    operator = "<";
                } else if (condition.contains(">")) {
                    leftCondition = condition.substring(0, condition.indexOf(">"));
                    rightCondition = condition.substring(condition.indexOf(">") + 1);
                    operator = ">";
                } else {
                    return "Invalid condition!";
                }

                String leftSide = PlaceholderAPI.setBracketPlaceholders(target, setBracketRelationalPlaceholders(viewer, target, leftCondition));
                String rightSide = PlaceholderAPI.setBracketPlaceholders(target, setBracketRelationalPlaceholders(viewer, target, rightCondition));

                if (stripColors) {
                    leftSide = ChatColor.stripColor(color(leftSide));
                    rightSide = ChatColor.stripColor(color(rightSide));
                }

                boolean conditionResult;
                switch (operator) {
                    case "==": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = floatLeft == floatRight;
                        } catch (Exception ignore) {
                            conditionResult = leftSide.equals(rightSide);
                        }
                        break;
                    }
                    case "~=": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = ((int) floatLeft) == ((int) floatRight);
                        } catch (Exception ignore) {
                            conditionResult = ChatColor.stripColor(leftSide).equalsIgnoreCase(ChatColor.stripColor(rightSide));
                        }
                        break;
                    }
                    case "!=": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = floatLeft != floatRight;
                        } catch (Exception ignore) {
                            conditionResult = !leftSide.equals(rightSide);
                        }
                        break;
                    }
                    case ">=": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = floatLeft >= floatRight;
                        } catch (Exception ignore) {
                            return "Invalid ternary results!";
                        }
                        break;
                    }
                    case "<=": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = floatLeft <= floatRight;
                        } catch (Exception ignore) {
                            return "Invalid ternary results!";
                        }
                        break;
                    }
                    case ">": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = floatLeft > floatRight;
                        } catch (Exception ignore) {
                            return "Invalid ternary results!";
                        }
                        break;
                    }
                    case "<": {
                        try {
                            double floatLeft = Double.parseDouble(leftSide);
                            double floatRight = Double.parseDouble(rightSide);
                            conditionResult = floatLeft < floatRight;
                        } catch (Exception ignore) {
                            return "Invalid ternary results!";
                        }
                        break;
                    }
                    default: {
                        return "Invalid ternary operator!";
                    }
                }

                if (conditionResult) {
                    return PlaceholderAPI.setBracketPlaceholders(target, setBracketRelationalPlaceholders(viewer, target, ifTrue));
                }

                return PlaceholderAPI.setBracketPlaceholders(target, setBracketRelationalPlaceholders(viewer, target, ifFalse));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "N/A";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Player online = player.getPlayer();
        if (params.equalsIgnoreCase("sleeping") && online != null) {
            return online.isSleeping() ? "True" : "False";
        }

        if (params.startsWith("playtime")) {
            int totalTicksPlayed = playtimeHook.getTicksPlayed(player);
            float seconds = totalTicksPlayed / 20f;
            Map<String, Long> duration = SGCraftLinkCommon.duration((long) (seconds * 1000L));
            String format = "";
            if (duration.containsKey("years")) {
                format += "" + ChatColor.YELLOW + duration.get("years") + " " + ChatColor.GOLD + "years ";
            }
            if (duration.containsKey("days")) {
                format += "" + ChatColor.YELLOW + duration.get("days") + " " + ChatColor.GOLD + "days ";
            }
            if (duration.containsKey("hours")) {
                format += "" + ChatColor.YELLOW + duration.get("hours") + " " + ChatColor.GOLD + "hours ";
            }
            if (duration.containsKey("mins")) {
                format += "" + ChatColor.YELLOW + duration.get("mins") + " " + ChatColor.GOLD + "mins ";
            }
            if (duration.containsKey("sec")) {
                format += "" + ChatColor.YELLOW + duration.get("sec") + " " + ChatColor.GOLD + "sec ";
            }
            return format.trim();
        }

        if (params.startsWith("baltop_")) {
            if (piggyBank == null)
                return "N/A";

            String number = params.substring(params.lastIndexOf("_") + 1);
            int i = Integer.parseInt(number);
            int index = i - 1;
            Map.Entry<String, Double> entry = piggyBank.getBaltop(index);
            if (entry == null)
                return null;

            if (params.startsWith("baltop_amount_")) {
                return String.valueOf(entry.getValue().intValue());
            } else if (params.startsWith("baltop_name_")) {
                return plugin.getServer().getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
            }
        }

        if (params.equalsIgnoreCase("eggs")) {
            if (headHunt == null)
                return "0";

            return String.valueOf(headHunt.getAmount(player.getName()));
        }

        if (params.equalsIgnoreCase("eggsleft")) {
            if (headHunt == null)
                return "0";
            return String.valueOf(headHunt.getRemaining());
        }

        if (params.toLowerCase().startsWith("eggs_top")) {
            if (headHunt == null)
                return "N/A";

            String number = params.substring(params.lastIndexOf("_") + 1);
            int rank = Integer.parseInt(number);

            Map.Entry<String, Integer> entry = headHunt.getAmountRank(rank);
            if (entry == null)
                return null;

            if (params.toLowerCase().contains("name")) {
                return entry.getKey() != null ? entry.getKey() : "N/A";
            } else if (params.toLowerCase().contains("amount")) {
                return entry.getValue() != null ? String.valueOf(entry.getValue()) : "N/A";
            }

            return "N/A";
        }

        if (params.startsWith("font_")) {
            String source;
            boolean checkChar = false;
            if (params.startsWith("font_color_")) {
                source = color(params.substring("font_color_".length()));
                checkChar = true;
            } else {
                source = params.substring("font_".length());
            }

            if (fonter == null)
                return source;

            for (Font font : fonter.getFonts()) {
                if (source.startsWith(font.getName().toLowerCase())) {
                    source = source.substring(font.getName().length() + 1);
                    if (source.startsWith("+") && source.endsWith("+")) {
                        source = PlaceholderAPI.setPlaceholders(player, "%" + source.substring(1, source.length() - 1) + "%");
                    }

                    source = PlaceholderAPI.setBracketPlaceholders(player, source);
                    return font.format(source, checkChar);
                }
            }
        }

        if (params.startsWith("fontuser_")) {
            String source;
            boolean checkChar = false;
            if (params.startsWith("fontuser_color_")) {
                source = color(params.substring("fontuser_color_".length()));
                checkChar = true;
            } else {
                source = params.substring("fontuser_".length());
            }

            if (fonter == null)
                return source;

            if (source.startsWith("+") && source.endsWith("+")) {
                source = PlaceholderAPI.setPlaceholders(player, "%" + source.substring(1, source.length() - 1) + "%");
            }

            source = PlaceholderAPI.setBracketPlaceholders(player, source);

            FontUserSettings fontUserSettings = fonter.getFontUser(player.getUniqueId());
            Font font = fontUserSettings.getFont();

            return font.format(source, checkChar);
        }

        if (params.startsWith("ternary_")) {
            String source = params.substring("ternary_".length()); // condition?true:false (%sgcraftlink_ternary_{}?{}:{}%)
            if (!source.contains("?") && !source.contains(":")) {
                return "Invalid expression!";
            }
            boolean stripColors = false;
            if (source.startsWith("strip_")) {
                source = params.substring("strip_".length());
                stripColors = true;
            }
            int qIndex = source.indexOf("?");
            while (source.charAt(qIndex - 1) == '\\') {
                source = source.substring(0, qIndex - 1) + source.substring(qIndex);
                qIndex = source.indexOf("?", qIndex);
            }
            String condition = source.substring(0, qIndex);
            int cIndex = source.indexOf(":");
            while (source.charAt(cIndex - 1) == '\\') {
                source = source.substring(0, cIndex - 1) + source.substring(cIndex);
                cIndex = source.indexOf(":", cIndex);
            }

            String ifTrue = source.substring(qIndex + 1, cIndex);
            String ifFalse = source.substring(cIndex + 1);

            String leftCondition;
            String rightCondition;
            String operator;
            if (condition.contains("==")) {
                leftCondition = condition.substring(0, condition.indexOf("=="));
                rightCondition = condition.substring(condition.indexOf("==") + 2);
                operator = "==";
            } else if (condition.contains("~=")) {
                leftCondition = condition.substring(0, condition.indexOf("~="));
                rightCondition = condition.substring(condition.indexOf("~=") + 2);
                operator = "~=";
            } else if (condition.contains("!=")) {
                leftCondition = condition.substring(0, condition.indexOf("!="));
                rightCondition = condition.substring(condition.indexOf("!=") + 2);
                operator = "!=";
            } else if (condition.contains("<=")) {
                leftCondition = condition.substring(0, condition.indexOf("<="));
                rightCondition = condition.substring(condition.indexOf("<=") + 2);
                operator = "<=";
            } else if (condition.contains(">=")) {
                leftCondition = condition.substring(0, condition.indexOf(">="));
                rightCondition = condition.substring(condition.indexOf(">=") + 2);
                operator = ">=";
            } else if (condition.contains("<")) {
                leftCondition = condition.substring(0, condition.indexOf("<"));
                rightCondition = condition.substring(condition.indexOf("<") + 1);
                operator = "<";
            } else if (condition.contains(">")) {
                leftCondition = condition.substring(0, condition.indexOf(">"));
                rightCondition = condition.substring(condition.indexOf(">") + 1);
                operator = ">";
            } else {
                return "Invalid condition!";
            }

            String leftSide = PlaceholderAPI.setBracketPlaceholders(player, leftCondition).trim();
            String rightSide = PlaceholderAPI.setBracketPlaceholders(player, rightCondition).trim();

            if (stripColors) {
                leftSide = ChatColor.stripColor(color(leftSide));
                rightSide = ChatColor.stripColor(color(rightSide));
            }

            boolean conditionResult;
            switch (operator) {
                case "==": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = floatLeft == floatRight;
                    } catch (Exception ignore) {
                        conditionResult = leftSide.equals(rightSide);
                    }
                    break;
                }
                case "~=": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = ((int) floatLeft) == ((int) floatRight);
                    } catch (Exception ignore) {
                        conditionResult = ChatColor.stripColor(leftSide).equalsIgnoreCase(ChatColor.stripColor(rightSide));
                    }
                    break;
                }
                case "!=": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = floatLeft != floatRight;
                    } catch (Exception ignore) {
                        conditionResult = !leftSide.equals(rightSide);
                    }
                    break;
                }
                case ">=": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = floatLeft >= floatRight;
                    } catch (Exception ignore) {
                        return "Invalid ternary results!";
                    }
                    break;
                }
                case "<=": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = floatLeft <= floatRight;
                    } catch (Exception ignore) {
                        return "Invalid ternary results!";
                    }
                    break;
                }
                case ">": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = floatLeft > floatRight;
                    } catch (Exception ignore) {
                        return "Invalid ternary results!";
                    }
                    break;
                }
                case "<": {
                    try {
                        double floatLeft = Double.parseDouble(leftSide);
                        double floatRight = Double.parseDouble(rightSide);
                        conditionResult = floatLeft < floatRight;
                    } catch (Exception ignore) {
                        return "Invalid ternary results!";
                    }
                    break;
                }
                default: {
                    return "Invalid ternary operator!";
                }
            }

            if (conditionResult) {
                return PlaceholderAPI.setBracketPlaceholders(player, ifTrue);
            }

            return PlaceholderAPI.setBracketPlaceholders(player, ifFalse);
        }

        return online != null ? this.onPlaceholderRequest(online, params) : "null";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("tags")) {
            List<String> tags = new ArrayList<>();
            try {
                if (tagModule == null)
                    return "N/A";

                if (player.isOp() || player.hasPermission("*") || player.hasPermission("tags.*")) {
                    Collection<String> permissions;
                    if (luckpermsModule != null) {
                        permissions = luckpermsModule.getPermissions(player);
                    } else {
                        permissions = player.getEffectivePermissions().stream()
                                .filter(PermissionAttachmentInfo::getValue)
                                .map(PermissionAttachmentInfo::getPermission)
                                .collect(Collectors.toList());
                    }
                    for (Tag tag : tagModule.getAllTags()) {
                        if (permissions.contains("tags." + tag.getName().toLowerCase())) {
                            tags.add(tag.getDisplay());
                        }
                    }
                } else {
                    for (Tag tag : tagModule.getAllTags()) {
                        if (player.hasPermission("tags." + tag.getName().toLowerCase())) {
                            tags.add(tag.getDisplay());
                        }
                    }
                }

                String separator = plugin.getConfig().getString("tag-separator", ", ");
                return String.join(separator, tags);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("songtitle")) {
            if (musicHook == null) {
                return "N/A";
            }
            return musicHook.getListeningTitle(player);
        }

        if (params.equalsIgnoreCase("songauthor")) {
            if (musicHook == null) {
                return "N/A";
            }
            return musicHook.getListeningAuthor(player);
        }

        if (params.equalsIgnoreCase("texturepack")) {
            return String.valueOf(player.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED ||
                    player.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);
        }

        if (params.equalsIgnoreCase("rep")) {
            try {
                if (reputationModule == null)
                    return "N/A";

                return reputationModule.getReputation(player).getFormattedRating();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("currentjoinstreak")) {
            try {
                if (reputationModule == null)
                    return "N/A";

                return String.valueOf(reputationModule.getReputation(player).getDayJoinStreak());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("longestjoinstreak")) {
            try {
                if (reputationModule == null)
                    return "N/A";

                return String.valueOf(reputationModule.getReputation(player).getLongestDayJoinStreak());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("totalrates")) {
            try {
                if (reputationModule == null)
                    return "N/A";

                return String.valueOf(reputationModule.getReputation(player).getTotalRatings());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.startsWith("rep_")) {
            try {
                if (reputationModule == null)
                    return "N/A";

                int i = Integer.parseInt(params.substring(params.lastIndexOf("_") + 1));
                List<PlayerReputation> ordered = reputationModule.getOrderedReputations();
                if (i <= 0 || i > ordered.size())
                    return "N/A";

                PlayerReputation reputation = ordered.get(i - 1);
                if (params.startsWith("rep_name_")) {
                    return reputation.getName();
                } else if (params.startsWith("rep_rating_")) {
                    return reputation.getFormattedRating();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "N/A";
    }
}
