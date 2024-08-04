package xyz.destiall.sgcraftlink.proxy.commands;

import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;
import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsTrackResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StaffCommand implements CommandProvider, TabProvider {
    private final PluginProvider plugin;
    private final ReportCommand reportCommand;

    public StaffCommand(PluginProvider plugin) {
        this.plugin = plugin;
        reportCommand = new ReportCommand(plugin);
    }

    @Override
    public void execute(PlayerProvider sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("report")) {
            reportCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        if (!sender.hasPermission("sgc.admin")) {
            ComponentProvider<?> message = ComponentProvider.of("You do not have permission!");
            message.setColor(ColorProvider.ChatColor.RED);
            sender.sendMessageComponent(message);
            return;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("announce")) {
            String message = ColorProvider.of(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));

            Collection<PlayerProvider> players = plugin.getPlayers();
            for (PlayerProvider p : players) {
                p.sendMessage(message);
            }

            plugin.getLoggerProvider().info(message);
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.shutdown();
            plugin.disable();
            plugin.enable();

            sender.sendMessage(ColorProvider.of("&aReloaded SGCraftLink Proxy"));
            return;
        }

        if (args.length > 2) {
            String type = args[0];
            String playerName = args[1];

            PlayerProvider player = plugin.getPlayer(playerName);
            if (type.equalsIgnoreCase("promote")) {
                if (player != null) {
                    String track = args[2];
                    try {
                        LuckPermsTrackResult b = plugin.getLuckPermsManager().promote(player, track).get();
                        plugin.getLuckPermsManager().getApi().getUserManager().saveUser(b.user);
                        Map<String, Object> data = SGCraftLinkCommon.createCommandsData("lp user " + playerName + " promote " + track);
                        for (String server : plugin.getDataManager().getServers()) {
                            plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, server, data, (result) -> {
                                ComponentProvider<?> message;
                                if (result.isSuccess()) {
                                    message = ComponentProvider.of("Promoted " + player.getName() + " to " + b.group);
                                    message.setColor(ColorProvider.ChatColor.GREEN);
                                } else {
                                    message = ComponentProvider.of("Unable to promote " + player.getName() + " to " + b.group);
                                    message.setColor(ColorProvider.ChatColor.RED);
                                }
                                sender.sendMessageComponent(message);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (type.equalsIgnoreCase("demote")) {
                if (player != null) {
                    String track = args[2];
                    try {
                        LuckPermsTrackResult b = plugin.getLuckPermsManager().demote(player, track).get();
                        plugin.getLuckPermsManager().getApi().getUserManager().saveUser(b.user);
                        Map<String, Object> data = SGCraftLinkCommon.createCommandsData("lp user " + playerName + " demote " + track);
                        for (String server : plugin.getDataManager().getServers()) {
                            plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, server, data, (result) -> {
                                ComponentProvider<?> message;
                                if (result.isSuccess()) {
                                    message = ComponentProvider.of("Demoted " + player.getName() + " to " + b.group);
                                    message.setColor(ColorProvider.ChatColor.GREEN);
                                } else {
                                    message = ComponentProvider.of("Unable to demote " + player.getName() + " to " + b.group);
                                    message.setColor(ColorProvider.ChatColor.RED);
                                }
                                sender.sendMessageComponent(message);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (type.equalsIgnoreCase("clearinv")) {
                String server = args[2];
                Map<String, Object> data = SGCraftLinkCommon.createCommandsData("cmi clear " + playerName);
                plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, server.toLowerCase(), data, (result) -> {
                    ComponentProvider<?> message;
                    if (result.isSuccess()) {
                        message = ComponentProvider.of("Cleared inventory in server " + server + " for " + playerName);
                        message.setColor(ColorProvider.ChatColor.GREEN);
                    } else {
                        message = ComponentProvider.of("Unable to clear inventory in server " + server + " for " + playerName);
                        message.setColor(ColorProvider.ChatColor.RED);
                    }
                    sender.sendMessageComponent(message);
                });
            } else if (type.equalsIgnoreCase("xcatch")) {
                String server = args[2];
                Map<String, Object> data = SGCraftLinkCommon.createCommandsData("xcatch clear " + playerName);
                plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, server.toLowerCase(), data, (result) -> {
                    ComponentProvider<?> message;
                    if (result.isSuccess()) {
                        message = ComponentProvider.of("Cleared xray logs in server " + server + " for " + playerName);
                        message.setColor(ColorProvider.ChatColor.GREEN);
                    } else {
                        message = ComponentProvider.of("Unable to clear xray logs in server " + server + " for " + playerName);
                        message.setColor(ColorProvider.ChatColor.RED);
                    }
                    sender.sendMessageComponent(message);
                });
            } else if (type.equalsIgnoreCase("jail")) {
                String server = args[2];
                String time = args[3];
                String reason = args[4].replace("_", " ");
                Map<String, Object> data = SGCraftLinkCommon.createCommandsData("jail " + playerName + " " + time + " " + "r:" + reason);
                plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, server.toLowerCase(), data, (result) -> {
                    ComponentProvider<?> message;
                    if (result.isSuccess()) {
                        message = ComponentProvider.of("Jailed in server " + server + " for " + playerName);
                        message.setColor(ColorProvider.ChatColor.GREEN);
                    } else {
                        message = ComponentProvider.of("Unable to jail in server " + server + " for " + playerName);
                        message.setColor(ColorProvider.ChatColor.RED);
                    }
                    sender.sendMessageComponent(message);
                });
            }
        }
    }

    @Override
    public List<String> tab(PlayerProvider sender, String[] args) {
        if (!sender.hasPermission("sgc.admin")) {
            return Collections.emptyList();
        }

        if (args == null || args[0].isEmpty() || args.length == 1) {
            return Stream.of("announce", "promote", "demote", "clearinv", "reload").filter(a -> args == null || args[0].isEmpty() || a.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("report")) {
            return reportCommand.tab(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (args.length == 2) {
            return plugin.getPlayers().stream().map(PlayerProvider::getName).filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote")) {
            return Stream.of("staff", "vip", "builder").filter(a -> a.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("clearinv")) {
            return Stream.of("vanilla", "skyblock", "survival").filter(a -> a.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
