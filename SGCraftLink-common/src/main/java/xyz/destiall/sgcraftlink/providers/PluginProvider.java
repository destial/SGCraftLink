package xyz.destiall.sgcraftlink.providers;

import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.proxy.botsentry.BotSentryManager;
import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsManager;
import xyz.destiall.sgcraftlink.proxy.misc.BirthdayManager;
import xyz.destiall.sgcraftlink.proxy.misc.DataManager;
import xyz.destiall.sgcraftlink.proxy.misc.DiscordManager;
import xyz.destiall.sgcraftlink.proxy.skins.PerServerSkin;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface PluginProvider {

    DiscordManager getDiscordProvider();
    ConfigProvider<?> getConfigProvider();
    LinkProvider getLinkProvider();
    PlaytimeProvider getPlaytimeProvider();
    LuckPermsManager getLuckPermsManager();
    BotSentryManager getBotSentryManager();
    PluginMessageProvider getPluginMessager();
    SchedulerProvider<?> getScheduler();
    BirthdayManager getBirthdayManager();
    LoggerProvider getLoggerProvider();
    DataManager getDataManager();
    PerServerSkin getPerServerSkin();

    @NotNull PlayerProvider getPlayer(String name);
    PlayerProvider getPlayer(UUID uuid);
    PlayerProvider getConsole();

    Collection<PlayerProvider> getPlayers();
    void dispatchCommand(String command);
    Collection<ServerProvider> getServers();

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

    boolean isShuttingDown();
}
