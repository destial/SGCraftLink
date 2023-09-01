package xyz.destiall.sgcraftlink.modules;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.ScheduledCommand;

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

        for (String datekey : config.getKeys(false)) {
            Date date = new Date(Long.parseLong(datekey));
            List<String> commands = config.getStringList(datekey);

            scheduleCommand(date, commands);
        }

        plugin.getCommand("schedulecmd").setExecutor(this);
    }

    @Override
    public void loadDefaultConfig() {}

    public void scheduleCommand(Date date, Collection<String> commands) {
        for (ScheduledCommand cmd : scheduledCommands) {
            if (cmd.date.equals(date)) {
                cmd.commands.addAll(commands);
                return;
            }
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
        save();
        for (ScheduledCommand cmd : scheduledCommands) {
            cmd.timer.cancel();
        }
        scheduledCommands.clear();

        plugin.getCommand("schedulecmd").setExecutor(null);
    }

    @Override
    public void save() {
        if (scheduledCommands != null) {
            for (ScheduledCommand cmd : scheduledCommands) {
                config.set(String.valueOf(cmd.date.getTime()), cmd.commands);
            }
        }
        super.save();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /schedulecmd [date] [command]
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /schedulecmd [date] [command]"));
            return false;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&aCurrently scheduled commands:"));
            for (ScheduledCommand cmd : scheduledCommands) {
                sender.sendMessage(color("&b" + DateFormat.getDateTimeInstance().format(cmd.date) + ": " + String.join("|", cmd.commands)));
            }
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
            plugin.getLogger().severe("Unable to parse duration " + s);
            return Duration.ZERO;
        }
    }
}
