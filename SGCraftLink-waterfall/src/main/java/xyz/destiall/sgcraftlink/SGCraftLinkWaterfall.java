package xyz.destiall.sgcraftlink;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeClickEvent;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeColor;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeHoverEvent;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeLogger;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePlayer;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeScheduler;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeTextComponent;
import xyz.destiall.sgcraftlink.commands.BungeeCommand;
import xyz.destiall.sgcraftlink.listeners.BungeeCommandListener;
import xyz.destiall.sgcraftlink.listeners.BungeePMListener;
import xyz.destiall.sgcraftlink.listeners.BungeeStaffListener;
import xyz.destiall.sgcraftlink.managers.BungeeConfigProvider;
import xyz.destiall.sgcraftlink.managers.BungeeLinkProvider;
import xyz.destiall.sgcraftlink.managers.BungeePlaytimeProvider;
import xyz.destiall.sgcraftlink.managers.BungeePluginMessager;
import xyz.destiall.sgcraftlink.providers.DiscordProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginMessageProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.commands.BirthdayCommand;
import xyz.destiall.sgcraftlink.proxy.commands.DiscordCommand;
import xyz.destiall.sgcraftlink.proxy.commands.StaffCommand;
import xyz.destiall.sgcraftlink.proxy.luckperms.LuckPermsManager;
import xyz.destiall.sgcraftlink.proxy.misc.BirthdayManager;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SGCraftLinkWaterfall extends Plugin implements PluginProvider {
    private BungeeScheduler scheduler;
    private BungeeConfigProvider configManager;
    private DiscordProvider discordManager;
    private BungeeLinkProvider linkManager;
    private BungeePlaytimeProvider playtimeManager;
    private BungeePluginMessager pluginMessager;
    private BungeeLogger logger;
    private LuckPermsManager luckPermsManager;
    private BirthdayManager birthdayManager;

    @Override
    public void onEnable() {
        new BungeeColor();
        new BungeeTextComponent();
        new BungeeClickEvent();
        new BungeeHoverEvent();
        scheduler = new BungeeScheduler(this);
        enable();
    }

    @Override
    public void onDisable() {
        shutdown();
        disable();
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
    public void disable() {
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().unregisterChannel(SGCraftLinkCommon.CHANNEL);
    }

    @Override
    public void enable() {
        try {
            luckPermsManager = new LuckPermsManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            configManager = new BungeeConfigProvider(this, "config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            discordManager = new DiscordProvider(this);
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
            pluginMessager = new BungeePluginMessager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logger = new BungeeLogger(getLogger());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            birthdayManager = new BirthdayManager(this);
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
    public BungeePlayer getPlayer(String name) {
        ProxiedPlayer player = getProxy().getPlayer(name);
        return player != null ? new BungeePlayer(player) : null;
    }

    @Override
    public BungeePlayer getPlayer(UUID uuid) {
        ProxiedPlayer player = getProxy().getPlayer(uuid);
        return player != null ? new BungeePlayer(player) : null;
    }

    @Override
    public Collection<PlayerProvider> getPlayers() {
        return getProxy().getPlayers().stream().map(BungeePlayer::new).collect(Collectors.toList());
    }

    @Override
    public DiscordProvider getDiscordProvider() {
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
}
