package xyz.destiall.sgcraftlink.items;

import xyz.destiall.sgcraftlink.modules.vanilla.ReputationModule;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PlayerReputation {
    private final ReputationModule module;
    private final String name;
    private final Map<String, Long> ratedByPlayers;
    private int playerRatings;
    private int totalRatings;
    private int dayJoinStreak;
    private int longestDayJoinStreak;
    private ZonedDateTime lastJoinDate;

    public PlayerReputation(ReputationModule module, String name) {
        this(module, name, 0, 0, 1, 1, ZonedDateTime.now(), new HashMap<>());
    }

    public PlayerReputation(ReputationModule module, String name, int playerRatings, int totalRatings, int dayJoinStreak, int longestDayJoinStreak, ZonedDateTime lastJoinDate, Map<String, Long> ratedByPlayers) {
        this.module = module;
        this.name = name;
        this.playerRatings = playerRatings;
        this.totalRatings = totalRatings;
        this.dayJoinStreak = dayJoinStreak;
        this.longestDayJoinStreak = longestDayJoinStreak;
        this.lastJoinDate = lastJoinDate;
        this.ratedByPlayers = ratedByPlayers;
    }

    public String getName() {
        return name;
    }

    public int getPlayerRatings() {
        return playerRatings;
    }

    public void setPlayerRatings(int playerRatings) {
        this.playerRatings = playerRatings;
    }

    public int getDayJoinStreak() {
        return dayJoinStreak;
    }

    public void setDayJoinStreak(int dayJoinStreak) {
        this.dayJoinStreak = dayJoinStreak;
    }

    public ZonedDateTime getLastJoinDate() {
        return lastJoinDate;
    }

    public void setLastJoinDate(ZonedDateTime lastJoinDate) {
        this.lastJoinDate = lastJoinDate;
    }

    public int getLongestDayJoinStreak() {
        return longestDayJoinStreak;
    }

    public void setLongestDayJoinStreak(int longestDayJoinStreak) {
        this.longestDayJoinStreak = longestDayJoinStreak;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Map<String, Long> getRatedByPlayers() {
        return ratedByPlayers;
    }

    public float getRating() {
        return Math.min((float) (playerRatings + (dayJoinStreak * dayJoinStreak)) / (totalRatings + longestDayJoinStreak), module.getMaxRating());
    }

    public String getFormattedRating() {
        return module.getFormatRating().replace("{rating}", String.format(Locale.US, "%.2f", getRating()))
                                       .replace("{max-rating}", String.valueOf(module.getMaxRating()))
                                       .replace("{total-ratings}", String.valueOf(totalRatings))
                                       .replace("{icon}", module.getIcon());
    }

    public void addPlayerRating(int rating) {
        this.playerRatings += rating;
        this.totalRatings += 1;
    }
}
