package xyz.destiall.sgcraftlink.proxy.commands;

import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;
import xyz.destiall.sgcraftlink.proxy.misc.Birthday;
import xyz.destiall.sgcraftlink.proxy.misc.BirthdayManager;

import java.time.Year;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BirthdayCommand implements CommandProvider, TabProvider {
    private final PluginProvider plugin;
    private final BirthdayManager birthdayManager;

    public BirthdayCommand(PluginProvider plugin) {
        this.plugin = plugin;
        this.birthdayManager = plugin.getBirthdayManager();
    }

    @Override
    public void execute(PlayerProvider player, String[] args) {
        Birthday birthday = birthdayManager.getBirthday(player);
        if (birthday != null) {
            player.sendMessage(ColorProvider.of("&cYou already have your birthday set to " + birthday.getMonthName() + " " + birthday.getDay()));
            player.sendMessage(ColorProvider.of("&cYour birthday is in "  + birthday.getTimeLeft()));
            return;
        }

        if (args.length != 3) {
            sendHelp(player);
            return;
        }

        try {
            String month = args[0];
            int day = Integer.parseInt(args[1]);
            Birthday.Month m = Birthday.Month.valueOf(month.toUpperCase());

            birthday = new Birthday(m, day, false);
            birthdayManager.saveBirthday(player, birthday, Year.now().getValue() - 1);

            player.sendMessage(ColorProvider.of("&aYou set your birthday to " + birthday.getMonthName() + " " + birthday.getDay()));
        } catch (Exception e) {
            e.printStackTrace();
            sendHelp(player);
        }
    }

    public void sendHelp(PlayerProvider player) {
        player.sendMessage(ColorProvider.of("&cUsage: /birthday [Month] [Day]"));
    }

    @Override
    public List<String> tab(PlayerProvider sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Birthday.Month.values()).map(Birthday.Month::getName).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            try {
                Birthday.Month month = Birthday.Month.valueOf(args[0].toUpperCase());
                return IntStream.range(1, Birthday.Days.ALL_DAYS.get(month).getMax()).mapToObj(String::valueOf).filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}
