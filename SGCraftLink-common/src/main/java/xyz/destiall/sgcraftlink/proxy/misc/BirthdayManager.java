package xyz.destiall.sgcraftlink.proxy.misc;

import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.time.Year;
import java.util.Map;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;

public class BirthdayManager {
    private final PluginProvider plugin;
    private final ConfigProvider<?> config;

    public BirthdayManager(PluginProvider plugin) {
        this.plugin = plugin;

        config = plugin.getConfigProvider().createConfig("birthdays.yml");
    }

    public void saveBirthday(String name, Birthday birthday) {
        config.getConfig().set(name + ".month", birthday.getMonthName());
        config.getConfig().set(name + ".day", birthday.getDay());
        config.getConfig().set(name + ".last-celebrated", birthday.getLastCelebrated());

        config.save();
    }

    public void saveBirthday(PlayerProvider player, Birthday birthday) {
        saveBirthday(player.getName(), birthday);
    }

    public Birthday getBirthday(String name) {
        if (!config.getConfig().contains(name))
            return null;

        String month = config.getConfig().getString(name + ".month");
        int lastCelebrated = config.getConfig().getInt(name + ".last-celebrated");
        int day = config.getConfig().getInt(name + ".day");

        return new Birthday(Birthday.Month.valueOf(month.toUpperCase()), day, lastCelebrated);
    }

    public Birthday getBirthday(@NotNull PlayerProvider player) {
        return getBirthday(player.getName());
    }

    public void onPlayerJoin(PlayerProvider player) {
        Birthday birthday = getBirthday(player);
        if (birthday == null || !birthday.isToday())
            return;

        Map<String, Object> map = createDataMap("celebrated", birthday.hasAlreadyCelebrated(), "name", player.getName());
        plugin.getDataManager().sendDataToAll(null, SGCraftLinkCommon.DATA_BIRTHDAY, map, mapping -> {
            if (!birthday.hasAlreadyCelebrated()) {
                birthday.setLastCelebrated(Year.now().getValue());
                saveBirthday(player, birthday);
            }
        });


    }
}
