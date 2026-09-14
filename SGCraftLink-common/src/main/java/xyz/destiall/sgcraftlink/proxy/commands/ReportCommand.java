package xyz.destiall.sgcraftlink.proxy.commands;

import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportCommand implements CommandProvider, TabProvider {
    private final PluginProvider plugin;
    private final Map<UUID, Long> reportCooldown;

    private final String[] reportKeywords = {
            "Xraying", "Begging", "Hacking", "Scamming", "Griefing",
            "Stealing", "TP-Killing", "Advertising", "Spamming", "Racism",
            "Exploiting", "Ban Evading", "Harassing", "Doxxing", "Botting",
            "Name-Change", "Compromised", "Abusing", "Autoclicking"
    };

    public ReportCommand(PluginProvider plugin) {
        this.plugin = plugin;
        reportCooldown = new HashMap<>();
    }

    @Override
    public void execute(PlayerProvider player, String[] args) {
        if (args == null || args.length == 0) {
            player.sendMessage(ColorProvider.of("&cUsage: /report [player]"));
            return;
        }

        long cd = reportCooldown.getOrDefault(player.getUUID(), 0L);
        long current = System.currentTimeMillis();
        if (cd > current) {
            player.sendMessage(ColorProvider.of("&cYou can report in the next " + ((int) (cd - current) / 1000L) + " seconds!"));
            return;
        }

        String playerName = args[0];
        PlayerProvider target = plugin.getPlayer(playerName);
        if (!target.isOnline()) {
            player.sendMessage(ColorProvider.of("&cPlayer not found!"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorProvider.of("&cYou need to specify a report reason!"));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        plugin.getDiscordProvider().sendReport(player, target, message);
        player.sendMessage(ColorProvider.of("&aYour report has been sent!"));

        for (PlayerProvider p : plugin.getPlayers()) {
            if (!p.hasPermission("sgc.report.bypass"))
                continue;

            p.sendMessage(ColorProvider.of("&6&l[REPORT]&e " + player.getName() + " has reported &c" + target.getName() + "&e for: &6" + message));
        }

        reportCooldown.put(player.getUUID(), current + (1000L * 60));
    }

    @Override
    public List<String> tab(PlayerProvider sender, String[] args) {
        if (args == null || args[0].isEmpty() || args.length == 1) {
            return plugin.getPlayers().stream()
                    .filter(s -> !s.hasPermission("sgc.report.bypass"))
                    .map(PlayerProvider::getName)
                    .filter(s -> args == null || args[0].isEmpty() || s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Arrays.stream(reportKeywords).filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
    }
}
