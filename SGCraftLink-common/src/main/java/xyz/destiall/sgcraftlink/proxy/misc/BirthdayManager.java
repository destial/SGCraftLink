package xyz.destiall.sgcraftlink.proxy.misc;

import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

public class BirthdayManager {
    private final PluginProvider plugin;
    private final ConfigProvider<?> config;

    public BirthdayManager(PluginProvider plugin) {
        this.plugin = plugin;

        config = plugin.getConfigProvider().createConfig("birthdays.yml");
    }

    public void saveBirthday(PlayerProvider player, Birthday birthday, int year) {
        config.getConfig().set(player.getName() + ".month", birthday.getMonthName());
        config.getConfig().set(player.getName() + ".day", birthday.getDay());
        config.getConfig().set(player.getName() + ".last-celebrated", year);

        config.save();
    }

    public Birthday getBirthday(PlayerProvider player) {
        if (!config.getConfig().contains(player.getName()))
            return null;

        String month = config.getConfig().getString(player.getName() + ".month");
        boolean celebrated = Integer.parseInt(config.getConfig().getString(player.getName() + ".last-celebrated")) == Year.now().getValue();
        int day = config.getConfig().getInt(player.getName() + ".day");

        return new Birthday(Birthday.Month.valueOf(month.toUpperCase()), day, celebrated);
    }

    public void onPlayerJoin(PlayerProvider player) {
        Birthday birthday = getBirthday(player);
        if (birthday == null || !birthday.isToday())
            return;

        Map<String, Object> map = new HashMap<>();
        map.put("celebrated", birthday.hasAlreadyCelebrated());
        map.put("name", player.getName());
        for (String server : plugin.getDataManager().getServers()) {
            plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_BIRTHDAY, server, map, (status, content) -> {});
        }

        saveBirthday(player, birthday, Year.now().getValue());
    }
}
