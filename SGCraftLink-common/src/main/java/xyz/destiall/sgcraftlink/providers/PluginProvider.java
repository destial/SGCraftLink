package xyz.destiall.sgcraftlink.providers;

import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsManager;
import xyz.destiall.sgcraftlink.proxy.misc.BirthdayManager;
import xyz.destiall.sgcraftlink.proxy.misc.DataManager;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface PluginProvider {
    DiscordProvider getDiscordProvider();
    ConfigProvider<?> getConfigProvider();
    LinkProvider getLinkProvider();
    PlaytimeProvider getPlaytimeProvider();
    LuckPermsManager getLuckPermsManager();
    PluginMessageProvider getPluginMessager();
    SchedulerProvider<?> getScheduler();

    BirthdayManager getBirthdayManager();

    LoggerProvider getLoggerProvider();

    DataManager getDataManager();

    PlayerProvider getPlayer(String name);
    PlayerProvider getPlayer(UUID uuid);

    Collection<PlayerProvider> getPlayers();
    void dispatchCommand(String command);

    File getDataFolder();

    default void shutdown() {
        getScheduler().cancelAll();
        try {
            getPlaytimeProvider().save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getLinkProvider().save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getDiscordProvider().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getDataManager().shutdown().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void disable();

    void enable();
}
