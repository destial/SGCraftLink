package xyz.destiall.sgcraftlink.modules.common;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.ScheduledCommand;
import xyz.destiall.sgcraftlink.modules.Module;

import java.text.DateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class ScheduledCommands extends Module implements CommandExecutor {
    private final Set<ScheduledCommand> scheduledCommands = ConcurrentHashMap.newKeySet();

    public ScheduledCommands(SGCraftLinkSpigot plugin) {
        super(plugin, "scheduled-commands.yml");
        setCommandExecutor("schedulecmd");
    }

    public void scheduleCommand(Date date, Collection<String> commands) {
        for (ScheduledCommand cmd : scheduledCommands) {
            if (cmd.date.equals(date)) {
                cmd.commands.addAll(commands);
                return;
            }
        }

        Date now = new Date();
        if (date.before(now)) {
            plugin.getScheduler().runTaskLater(() -> {
                for (String command : commands) {
                    plugin.dispatchCommand(command);
                }
            }, 2L);
            return;
        }

        final ScheduledCommand cmd = new ScheduledCommand();
        cmd.date = date;
        cmd.commands = new ArrayList<>();
        cmd.commands.addAll(commands);

        cmd.timer = new Timer();
        scheduledCommands.add(cmd);

        cmd.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (String command : cmd.commands) {
                    plugin.dispatchCommand(command);
                }
                config.set(String.valueOf(cmd.date.getTime()), null);
                scheduledCommands.remove(cmd);
            }
        }, date);
    }

    public void scheduleCommand(Date date, String... commands) {
        scheduleCommand(date, Arrays.asList(commands));
    }

    @Override
    public void disable() {
        unsetCommandExecutor("schedulecmd");
        saveConfig();

        for (ScheduledCommand cmd : scheduledCommands) {
            cmd.timer.cancel();
        }
        scheduledCommands.clear();
    }

    @Override
    public void enable() {
        for (String datekey : config.getKeys(false)) {
            if (datekey.equalsIgnoreCase("enabled"))
                continue;

            Date date = new Date(Long.parseLong(datekey));
            List<String> commands = config.getStringList(datekey);

            scheduleCommand(date, commands);
        }
        log("Registered " + scheduledCommands.size() + " scheduled commands");
    }

    @Override
    public void saveConfig() {
        for (ScheduledCommand cmd : scheduledCommands) {
            config.set(String.valueOf(cmd.date.getTime()), cmd.commands);
        }
        super.saveConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /schedulecmd [list|date]"));
            return false;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&aCurrently scheduled commands:"));
            int i = 0;
            for (ScheduledCommand cmd : scheduledCommands) {
                sender.sendMessage(color("&b" +(++i) + ". " + DateFormat.getDateTimeInstance().format(cmd.date) + ": " + String.join("|", cmd.commands)));
            }
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /schedulecmd [date] [command]"));
            return false;
        }

        String dateArg = args[0];
        Date date = null;
        try {
            date = new Date(Long.parseLong(dateArg));
        } catch (Exception ignored) {}

        if (date == null) {
            Duration duration = getDuration(dateArg);
            if (duration.isZero()) {
                sender.sendMessage(color("&cInvalid date format! Date argument must be EPOCH time or duration time (7d, 7M, 7s, etc)"));
                return false;
            }

            date = Date.from(Instant.now().plus(duration));
        }

        String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        scheduleCommand(date, cmd);
        sender.sendMessage(color("&aScheduled command \"" + cmd + "\" to run on " + DateFormat.getDateTimeInstance().format(date)));
        saveConfig();
        return true;
    }

    public Duration getDuration(String s) {
        ChronoUnit unit =
            s.endsWith("d") ? ChronoUnit.DAYS :
                s.endsWith("h") ? ChronoUnit.HOURS :
                    s.endsWith("s") ? ChronoUnit.SECONDS :
                        s.endsWith("m") ? ChronoUnit.MINUTES :
                            s.endsWith("M") ? ChronoUnit.MONTHS :
                                ChronoUnit.MILLIS;

        StringBuilder numberString = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            try {
                int n = Integer.parseInt(String.valueOf(s.charAt(i++)));
                numberString.append(n);
            } catch (Exception e) {
                break;
            }
        }
        try {
            return Duration.of(Integer.parseInt(numberString.toString()), unit);
        } catch (Exception e) {
            warn("Unable to parse duration " + s);
            return Duration.ZERO;
        }
    }

    @Override
    public boolean isUniversallyCompatible() {
        return true;
    }
}
