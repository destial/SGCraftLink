package xyz.destiall.sgcraftlink;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeClickEvent;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeColor;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeConnectedServer;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeConsolePlayer;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeHoverEvent;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeLogger;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeOfflinePlayer;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePerServerSkin;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePlayer;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeScheduler;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeTextComponent;
import xyz.destiall.sgcraftlink.commands.BungeeCommand;
import xyz.destiall.sgcraftlink.listeners.BungeeCommandListener;
import xyz.destiall.sgcraftlink.listeners.BungeePMListener;
import xyz.destiall.sgcraftlink.listeners.BungeeStaffListener;
import xyz.destiall.sgcraftlink.managers.BungeeBotSentryManager;
import xyz.destiall.sgcraftlink.managers.BungeeConfigProvider;
import xyz.destiall.sgcraftlink.managers.BungeeLinkProvider;
import xyz.destiall.sgcraftlink.managers.BungeePlaytimeProvider;
import xyz.destiall.sgcraftlink.managers.BungeePluginMessager;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginMessageProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.ServerProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;
import xyz.destiall.sgcraftlink.proxy.commands.BirthdayCommand;
import xyz.destiall.sgcraftlink.proxy.commands.DiscordCommand;
import xyz.destiall.sgcraftlink.proxy.commands.StaffCommand;
import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsManager;
import xyz.destiall.sgcraftlink.proxy.misc.BirthdayManager;
import xyz.destiall.sgcraftlink.proxy.misc.DataManager;
import xyz.destiall.sgcraftlink.proxy.misc.DiscordManager;
import xyz.destiall.sgcraftlink.proxy.skins.PerServerSkin;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SGCraftLinkWaterfall extends Plugin implements PluginProvider {
    private BungeeScheduler scheduler;
    private BungeeConfigProvider configManager;
    private DiscordManager discordManager;
    private BungeeLinkProvider linkManager;
    private BungeePlaytimeProvider playtimeManager;
    private BungeePluginMessager pluginMessager;
    private BungeeLogger logger;
    private LuckPermsManager luckPermsManager;
    private BungeeBotSentryManager botSentryManager;
    private BirthdayManager birthdayManager;
    private DataManager dataManager;
    private BungeePerServerSkin perServerSkin;
    private boolean shuttingDown = false;

    @Override
    public void onEnable() {
        SGCraftLinkCommon.PROXY = this;
        try {
            logger = new BungeeLogger(getLogger());
            SGCraftLinkCommon.LOGGER = logger;
        } catch (Exception e) {
            e.printStackTrace();
        }
        ColorProvider.setProvider(new BungeeColor());
        ComponentProvider.setProvider(BungeeTextComponent::new);
        ComponentClickProvider.setProvider(BungeeClickEvent::new);
        ComponentHoverProvider.setProvider(BungeeHoverEvent::new);
        scheduler = new BungeeScheduler(this);
        shuttingDown = false;
        enable();
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        shutdown();
        disable();
    }

    @Override
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    @Override
    public void dispatchCommand(String command) {
        try {
            getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<ServerProvider> getServers() {
        return getProxy().getServersCopy().values().stream().map(BungeeConnectedServer::new).collect(Collectors.toList());
    }

    @Override
    public void disable() {
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().unregisterChannel(SGCraftLinkCommon.CHANNEL);
        getConfigProvider().save();
    }

    @Override
    public void enable() {
        try {
            pluginMessager = new BungeePluginMessager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            configManager = new BungeeConfigProvider(this, "config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            dataManager = new DataManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            luckPermsManager = new LuckPermsManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            discordManager = new DiscordManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            playtimeManager = new BungeePlaytimeProvider(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            linkManager = new BungeeLinkProvider(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            birthdayManager = new BirthdayManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            perServerSkin = new BungeePerServerSkin(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            botSentryManager = new BungeeBotSentryManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        getProxy().registerChannel(SGCraftLinkCommon.CHANNEL);
        getProxy().getPluginManager().registerListener(this, new BungeePMListener(this));
        getProxy().getPluginManager().registerListener(this, new BungeeCommandListener(this));
        getProxy().getPluginManager().registerListener(this, new BungeeStaffListener(this));

        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(this, "discord", new DiscordCommand(this)));
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(this, "sgc", new StaffCommand(this)));
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(this, "birthday", new BirthdayCommand(this)));
    }

    @Override
    public BungeeConfigProvider getConfigProvider() {
        return configManager;
    }

    @Override
    public @NotNull PlayerProvider getPlayer(String name) {
        ProxiedPlayer player = getProxy().getPlayer(name);
        return player != null ? new BungeePlayer(player) : new BungeeOfflinePlayer(name);
    }

    @Override
    public PlayerProvider getPlayer(UUID uuid) {
        ProxiedPlayer player = getProxy().getPlayer(uuid);
        return player != null ? new BungeePlayer(player) : null;
    }

    @Override
    public BungeeConsolePlayer getConsole() {
        return new BungeeConsolePlayer(getProxy());
    }

    @Override
    public Collection<PlayerProvider> getPlayers() {
        return getProxy().getPlayers().stream().map(BungeePlayer::new).collect(Collectors.toList());
    }

    @Override
    public DiscordManager getDiscordProvider() {
        return discordManager;
    }

    @Override
    public BungeeLinkProvider getLinkProvider() {
        return linkManager;
    }

    @Override
    public BungeePlaytimeProvider getPlaytimeProvider() {
        return playtimeManager;
    }

    @Override
    public BungeeScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public BirthdayManager getBirthdayManager() {
        return birthdayManager;
    }

    @Override
    public LuckPermsManager getLuckPermsManager() {
        return luckPermsManager;
    }

    @Override
    public PluginMessageProvider getPluginMessager() {
        return pluginMessager;
    }

    @Override
    public BungeeLogger getLoggerProvider() {
        return logger;
    }

    @Override
    public DataManager getDataManager() {
        return dataManager;
    }

    @Override
    public PerServerSkin getPerServerSkin() {
        return perServerSkin;
    }

    @Override
    public BungeeBotSentryManager getBotSentryManager() {
        return botSentryManager;
    }
}
