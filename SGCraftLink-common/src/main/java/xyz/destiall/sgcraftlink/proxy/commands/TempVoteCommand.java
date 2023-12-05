package xyz.destiall.sgcraftlink.proxy.commands;

import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TempVoteCommand implements CommandProvider, TabProvider {
    private final PluginProvider plugin;
    private final ConfigProvider<?> config;
    private boolean dirty = false;

    private final String[] sites = {
            "MinecraftServers.Biz", "MinecraftServers.Org", "TopMinecraftServers.Org",
            "Minecraft-MP", "MinecraftServersList", "Servers-Minecraft", "MCSL",
            "minecraftservers_host"
    };

    public TempVoteCommand(PluginProvider plugin) {
        this.plugin = plugin;
        config = plugin.getConfigProvider().createConfig("temp-vote.yml");

        plugin.getScheduler().schedule(() -> {
            if (dirty) {
                config.save();
                dirty = false;
            }
        }, 60L, 300L, TimeUnit.SECONDS);
    }

    @Override
    public void execute(PlayerProvider player, String[] args) {
        if (args == null || args.length < 1) {
            player.sendMessage(ColorProvider.of("&cUsage: /vote [number]"));
            return;
        }

        String value = args[0];
        int i = 0;
        try {
            i = Integer.parseInt(value) - 1;
        } catch (Exception e) {
            player.sendMessage(ColorProvider.of("&cUsage: /vote [number]"));
            return;
        }

        if (i < 0 || i >= sites.length) {
            player.sendMessage(ColorProvider.of("&cUse numbers 1 to " + sites.length));
            return;
        }

        String site = sites[i];
        String siteConfig = site.replace(".", "-");
        long cd = config.getConfig().getLong(player.getUUID().toString() + "." + siteConfig, 0L);
        long current = System.currentTimeMillis();
        if (cd > current) {
            player.sendMessage(ColorProvider.of("&cYou can vote on that site in the next " + SGCraftLinkCommon.relativeDate(cd)));
            return;
        }

        plugin.dispatchCommand("ptestvote " + player.getName() + " serviceName=" + site);
        config.getConfig().set(player.getUUID().toString() + "." + siteConfig, current + 86400000L);
        dirty = true;
    }

    @Override
    public List<String> tab(PlayerProvider sender, String[] args) {
        return IntStream.range(1, sites.length + 1).mapToObj(Integer::toString).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
    }
}
