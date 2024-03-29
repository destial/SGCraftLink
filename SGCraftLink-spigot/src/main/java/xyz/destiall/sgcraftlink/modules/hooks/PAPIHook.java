package xyz.destiall.sgcraftlink.modules.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.PlayerReputation;
import xyz.destiall.sgcraftlink.items.Tag;
import xyz.destiall.sgcraftlink.modules.common.HeadHunt;
import xyz.destiall.sgcraftlink.modules.common.LuckPermsModule;
import xyz.destiall.sgcraftlink.modules.common.Tags;
import xyz.destiall.sgcraftlink.modules.hooks.utils.Font;
import xyz.destiall.sgcraftlink.modules.vanilla.PiggyBankModule;
import xyz.destiall.sgcraftlink.modules.vanilla.ReputationModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PAPIHook extends PlaceholderExpansion {
    private final SGCraftLinkSpigot plugin;
    private final Fonter fonter;

    public PAPIHook(SGCraftLinkSpigot plugin) {
        this.plugin = plugin;
        this.fonter = plugin.getModule(Fonter.class);
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

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Player online = player.getPlayer();
        if (params.equalsIgnoreCase("sleeping") && online != null) {
            return online.isSleeping() ? "True" : "False";
        }

        if (params.startsWith("baltop_")) {
            PiggyBankModule piggyBank = plugin.getModule(PiggyBankModule.class);
            if (piggyBank == null) {
                return "N/A";
            }

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
            HeadHunt headHunt = plugin.getModule(HeadHunt.class);
            if (headHunt == null)
                return "0";
            return String.valueOf(headHunt.getAmount(player.getName()));
        }

        if (params.equalsIgnoreCase("eggsleft")) {
            HeadHunt headHunt = plugin.getModule(HeadHunt.class);
            if (headHunt == null)
                return "0";
            return String.valueOf(headHunt.getRemaining());
        }

        if (params.toLowerCase().startsWith("eggs_top")) {
            HeadHunt headHunt = plugin.getModule(HeadHunt.class);
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
            String source = params.substring("font_".length());
            Fonter fonter = plugin.getModule(Fonter.class);
            if (fonter == null)
                return source;

            for (Font font : fonter.getFonts()) {
                if (source.startsWith(font.getName().toLowerCase())) {
                    source = source.substring(font.getName().length() + 1);
                    if (source.startsWith("+") && source.endsWith("+")) {
                        source = PlaceholderAPI.setPlaceholders(player, "%" + source.substring(1, source.length() - 1) + "%");
                    }
                    return font.format(source);
                }
            }
        }

        if (params.startsWith("fontuser_")) {
            String source = params.substring("fontuser_".length());
            Fonter fonter = plugin.getModule(Fonter.class);
            if (fonter == null)
                return source;

            if (source.startsWith("+") && source.endsWith("+")) {
                source = PlaceholderAPI.setPlaceholders(player, "%" + source.substring(1, source.length() - 1) + "%");
            }
            Font font = fonter.getUsers().get(player.getUniqueId());
            if (font == null)
                return source;

            return font.format(source);
        }

        return online != null ? this.onPlaceholderRequest(online, params) : "null";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("tags")) {
            List<String> tags = new ArrayList<>();
            try {
                Tags tagModule = plugin.getModule(Tags.class);
                if (tagModule == null)
                    return "N/A";

                if (player.isOp() || player.hasPermission("*") || player.hasPermission("tags.*")) {
                    LuckPermsModule module = plugin.getModule(LuckPermsModule.class);
                    Collection<String> permissions;
                    if (module != null) {
                        permissions = module.getPermissions(player);
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

        if (params.equalsIgnoreCase("rep")) {
            try {
                ReputationModule module = plugin.getModule(ReputationModule.class);
                if (module == null)
                    return "N/A";

                return module.getReputation(player).getFormattedRating();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("currentjoinstreak")) {
            try {
                ReputationModule module = plugin.getModule(ReputationModule.class);
                if (module == null)
                    return "N/A";

                return String.valueOf(module.getReputation(player).getDayJoinStreak());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("longestjoinstreak")) {
            try {
                ReputationModule module = plugin.getModule(ReputationModule.class);
                if (module == null)
                    return "N/A";

                return String.valueOf(module.getReputation(player).getLongestDayJoinStreak());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("totalrates")) {
            try {
                ReputationModule module = plugin.getModule(ReputationModule.class);
                if (module == null)
                    return "N/A";

                return String.valueOf(module.getReputation(player).getTotalRatings());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "N/A";
        }

        if (params.startsWith("rep_")) {
            try {
                ReputationModule module = plugin.getModule(ReputationModule.class);
                if (module == null)
                    return "N/A";

                int i = Integer.parseInt(params.substring(params.lastIndexOf("_") + 1));
                List<PlayerReputation> ordered = module.getOrderedReputations();
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
