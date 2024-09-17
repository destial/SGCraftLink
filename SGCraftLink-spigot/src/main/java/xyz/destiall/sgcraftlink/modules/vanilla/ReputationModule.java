package xyz.destiall.sgcraftlink.modules.vanilla;

import me.clip.placeholderapi.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.items.PlayerReputation;
import xyz.destiall.sgcraftlink.modules.Module;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class ReputationModule extends Module implements Listener, CommandExecutor, TabExecutor {
    private final List<PlayerReputation> reputations;
    private int maxRating;
    private String formatRating;
    private String icon;
    private List<String> possibleRatings;
    private boolean dirty = false;
    private Scheduler.Task saveTask;

    public ReputationModule(SGCraftLinkSpigot plugin) {
        super(plugin, "reputation.yml");
        this.reputations = new CopyOnWriteArrayList<>();

        plugin.getCommand("reputation").setExecutor(this);
    }

    public int getMaxRating() {
        return maxRating;
    }

    public String getFormatRating() {
        return formatRating;
    }

    public String getIcon() {
        return icon;
    }

    public void updateReputation(PlayerReputation reputation) {
        config.set("players." + reputation.getName() + ".player-ratings", reputation.getPlayerRatings());
        config.set("players." + reputation.getName() + ".total-ratings", reputation.getTotalRatings());
        config.set("players." + reputation.getName() + ".day-streak", reputation.getDayJoinStreak());
        config.set("players." + reputation.getName() + ".longest-day-streak", reputation.getLongestDayJoinStreak());
        config.set("players." + reputation.getName() + ".last-join-date", reputation.getLastJoinDate().toString());
        for (Map.Entry<String, Long> entry : reputation.getRatedByPlayers().entrySet()) {
            config.set("players." + reputation.getName() + ".rated." + entry.getKey(), entry.getValue());
        }

        dirty = true;
    }

    public List<PlayerReputation> getOrderedReputations() {
        reputations.sort((a, b) -> {
            int compare = Float.compare(b.getRating(), a.getRating());
            if (compare == 0) {
                return Integer.compare(b.getTotalRatings(), a.getTotalRatings());
            }
            return compare;
        });
        return reputations;
    }

    public PlayerReputation getReputation(String name) {
        PlayerReputation reputation = reputations.stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);

        if (reputation == null) {
            reputation = new PlayerReputation(this, name);
            if (config.contains("players." + reputation.getName())) {
                int playerRatings = config.getInt("players." + reputation.getName() + ".player-ratings", 0);
                int totalRatings = config.getInt("players." + reputation.getName() + ".total-ratings", 0);
                int dayStreak = config.getInt("players." + reputation.getName() + ".day-streak", 1);
                int longestDayStreak = config.getInt("players." + reputation.getName() + ".longest-day-streak", 1);
                ZonedDateTime lastJoinDate = ZonedDateTime.parse(config.getString("players." + reputation.getName() + ".last-join-date", ZonedDateTime.now().toString()));
                reputation.setPlayerRatings(playerRatings);
                reputation.setTotalRatings(totalRatings);
                reputation.setDayJoinStreak(dayStreak);
                reputation.setLastJoinDate(lastJoinDate);
                reputation.setLongestDayJoinStreak(longestDayStreak);

                ConfigurationSection ratedSection = config.getConfigurationSection("players." + reputation.getName() + ".rated");
                if (ratedSection != null) {
                    for (String rated : ratedSection.getKeys(false)) {
                        long time = ratedSection.getLong(rated);
                        if (time <= System.currentTimeMillis()) {
                            ratedSection.set(rated, null);
                            continue;
                        }

                        reputation.getRatedByPlayers().put(rated, time);
                    }
                }
            } else {
                updateReputation(reputation);
            }

            reputations.add(reputation);
        }

        return reputation;
    }

    public PlayerReputation getReputation(Player player) {
        return getReputation(player.getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerReputation reputation = getReputation(e.getPlayer());
        ZonedDateTime nowDate = ZonedDateTime.now();
        ZonedDateTime lastJoinDate = reputation.getLastJoinDate();
        if ((nowDate.getDayOfYear() - lastJoinDate.getDayOfYear() == 1) || // Same year
            (nowDate.getDayOfYear() == 1 && (lastJoinDate.getDayOfYear() == 365 || lastJoinDate.getDayOfYear() == 366))) { // New Year

            reputation.setDayJoinStreak(reputation.getDayJoinStreak() + 1);
            String streakMessage = config.getString("format.day-streak-continue", "&aYou have joined {streak} days in a row! Keep it up!")
                    .replace("{longest-streak}", String.valueOf(reputation.getLongestDayJoinStreak()))
                    .replace("{streak}", String.valueOf(reputation.getDayJoinStreak()));
            e.getPlayer().sendMessage(color(streakMessage));

            if (reputation.getDayJoinStreak() > reputation.getLongestDayJoinStreak()) {
                int oldLongestDayJoinStreak = reputation.getLongestDayJoinStreak();
                reputation.setLongestDayJoinStreak(reputation.getDayJoinStreak());
                String surpassMessage = config.getString("format.day-streak-surpass", "&aYou have surpassed your longest join streak of {longest-streak}!")
                        .replace("{longest-streak}", String.valueOf(oldLongestDayJoinStreak))
                        .replace("{streak}", String.valueOf(reputation.getDayJoinStreak()));
                e.getPlayer().sendMessage(color(surpassMessage));
            }
        } else if (nowDate.getDayOfYear() != lastJoinDate.getDayOfYear()) {
            String resetMessage = config.getString("format.day-streak-reset", "&cYour day streak has been reset! :(")
                            .replace("{longest-streak}", String.valueOf(reputation.getLongestDayJoinStreak()))
                            .replace("{streak}", String.valueOf(reputation.getDayJoinStreak()));

            reputation.setDayJoinStreak(1);
            e.getPlayer().sendMessage(color(resetMessage));
        }

        reputation.setLastJoinDate(nowDate);
        updateReputation(reputation);
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        loadFormatConfig();
    }

    public void loadFormatConfig() {
        config.set("format.max-rating", 5);
        config.set("format.rating", "{rating}/{max-rating} {icon}");
        config.set("format.icon", "★");
        config.set("format.player-rating", "&a{target}'s rating: {rating}");
        config.set("format.self-rating", "&aYour rating: {rating}");
        config.set("format.rate-player-message", "&aYou rated {target} a {rating} out of {max-rating} {icon}!");
        config.set("format.current-rate-player-message", "&aTheir current rating is now {rating}");
        config.set("format.rate-receive-message", "&a{target} rated you a {rating} out of {max-rating} {icon}!");
        config.set("format.current-rate-receive-message", "&aYour current rating is now {rating}");
        config.set("format.day-streak-continue", "&aYou have joined {streak} days in a row! Keep it up!");
        config.set("format.day-streak-surpass", "&aYou have surpassed your longest join streak of {longest-streak}!");
        config.set("format.day-streak-reset", "&cYour day streak has been reset! :(");
    }

    @Override
    public void disable() {
        plugin.getCommand("reputation").setExecutor(null);
        saveTask.cancel();
        saveConfig();
    }

    @Override
    public void enable() {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players != null) {
            for (String name : players.getKeys(false)) {
                getReputation(name);
            }
        }

        if (!config.contains("format")) {
            loadFormatConfig();
        }

        maxRating = config.getInt("format.max-rating", 5);
        formatRating = config.getString("format.rating", "{rating}/{max-rating} {icon}");
        icon = config.getString("format.icon", "★");

        possibleRatings = new ArrayList<>();
        for (int i = 0; i <= maxRating; i++) {
            possibleRatings.add(String.valueOf(i));
        }

        saveTask = plugin.getScheduler().runTaskTimerAsync(() -> {
            if (dirty) {
                saveConfig();
                dirty = false;
            }
        }, 600L, 600L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(Msg.color("&cThis feature is not enabled!"));
            return false;
        }

        // /reputation [player] [0 - 5]
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&cUsage: /" + label + " [player]"));
                return false;
            }

            Player player = (Player) sender;
            String selfMessage = config.getString("format.self-rating", "&aYour rating: {rating}")
                            .replace("{rating}", getReputation(player).getFormattedRating());
            sender.sendMessage(color(selfMessage));
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(color("&cPlayer is not online!"));
            return false;
        }

        if (target == sender) {
            sender.sendMessage(color("&cYou cannot rate yourself!"));
            return false;
        }

        PlayerReputation reputation = getReputation(target);
        if (args.length == 1) {
            String targetMessage = config.getString("format.player-rating", "&a{target}'s rating: {rating}")
                            .replace("{target}", target.getName())
                            .replace("{rating}", reputation.getFormattedRating());
            sender.sendMessage(color(targetMessage));
            return false;
        }

        if (args.length == 2) {
            try {
                int rating = Integer.parseInt(args[1]);
                if (rating < 0 || rating > 5) {
                    throw new Exception();
                }

                if (reputation.getRatedByPlayers().containsKey(sender.getName())) {
                    long time = reputation.getRatedByPlayers().get(sender.getName());
                    if (time > System.currentTimeMillis()) {
                        sender.sendMessage(color("&cYou have to wait at least 24 hours in between rating the same player!"));
                        return false;
                    }
                }

                long next = System.currentTimeMillis() + 86400000;
                reputation.addPlayerRating(rating);
                reputation.getRatedByPlayers().put(sender.getName(), next);

                updateReputation(reputation);

                String rateMessage = config.getString("format.rate-player-message", "&aYou rated {target} a {rating} out of {max-rating} {icon}!")
                                        .replace("{target}", target.getName())
                                        .replace("{rating}", String.valueOf(rating))
                                        .replace("{max-rating}", String.valueOf(maxRating))
                                        .replace("{icon}", icon);

                String currentRateMessage = config.getString("format.current-rate-player-message", "&aTheir current rating is now {rating}")
                        .replace("{target}", target.getName())
                        .replace("{rating}", reputation.getFormattedRating())
                        .replace("{max-rating}", String.valueOf(maxRating))
                        .replace("{icon}", icon);

                sender.sendMessage(color(rateMessage));
                sender.sendMessage(color(currentRateMessage));

                String receiveMessage = config.getString("format.rate-receive-message", "&a{target} rated you a {rating} out of {max-rating} {icon}!")
                        .replace("{target}", sender.getName())
                        .replace("{rating}", String.valueOf(rating))
                        .replace("{max-rating}", String.valueOf(maxRating))
                        .replace("{icon}", icon);

                String currentReceiveMessage = config.getString("format.current-rate-receive-message", "&aYour current rating is now {rating}")
                        .replace("{target}", sender.getName())
                        .replace("{rating}", reputation.getFormattedRating())
                        .replace("{max-rating}", String.valueOf(maxRating))
                        .replace("{icon}", icon);

                target.sendMessage(color(receiveMessage));
                target.sendMessage(color(currentReceiveMessage));

            } catch (Exception e) {
                sender.sendMessage("&cYour rating has to be from 0 to " + maxRating + "!");
            }

            return false;
        }

        sender.sendMessage(color("&cUsage: /" + label + " [player]"));
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream()
                                                            .map(Player::getName)
                                                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                                                            .collect(Collectors.toList())

                : args.length == 2 ? possibleRatings.stream().filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                                                .collect(Collectors.toList()) : null;
    }
}
