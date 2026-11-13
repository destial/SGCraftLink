package xyz.destiall.sgcraftlink;

import ch.andre601.advancedserverlist.api.AdvancedServerListAPI;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import xyz.destiall.sgcraftlink.addons.VelocityASLPlaceholder;
import xyz.destiall.sgcraftlink.commands.GList;
import xyz.destiall.sgcraftlink.commands.VelocityCommand;
import xyz.destiall.sgcraftlink.listeners.VelocityCommandListener;
import xyz.destiall.sgcraftlink.listeners.VelocityPMListener;
import xyz.destiall.sgcraftlink.listeners.VelocityStaffListener;
import xyz.destiall.sgcraftlink.managers.VelocityBotSentryManager;
import xyz.destiall.sgcraftlink.managers.VelocityConfigProvider;
import xyz.destiall.sgcraftlink.managers.VelocityLinkProvider;
import xyz.destiall.sgcraftlink.managers.VelocityPlaytimeProvider;
import xyz.destiall.sgcraftlink.managers.VelocityPluginMessager;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
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
import xyz.destiall.sgcraftlink.velocityproviders.VelocityClickEvent;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityColor;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConnectedServer;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConsolePlayer;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityHoverEvent;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityLogger;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityOfflinePlayer;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPerServerSkin;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPlayer;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityScheduler;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityTextComponent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Plugin(
        id = "sgcraftlink",
        name = "SGCraftLink",
        version = "1.0-SNAPSHOT",
        authors = {"destiall"},
        dependencies = {
            @Dependency(id = "skinsrestorer", optional = true),
            @Dependency(id = "luckperms", optional = true),
            @Dependency(id = "litebans", optional = true),
            @Dependency(id = "botsentry", optional = true),
            @Dependency(id = "advancedserverlist", optional = true)
        }
)
public class SGCraftLinkVelocity implements PluginProvider {
    public static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(SGCraftLinkCommon.CHANNEL);
    private final VelocityLogger logger;
    private final ProxyServer server;
    private final File dataFolder;

    private VelocityScheduler scheduler;
    private VelocityConfigProvider configManager;
    private VelocityLinkProvider linkManager;
    private DiscordManager discordManager;
    private VelocityPlaytimeProvider playtimeManager;
    private VelocityPluginMessager pluginMessager;
    private VelocityPerServerSkin perServerSkin;
    private LuckPermsManager luckPermsManager;
    private VelocityBotSentryManager botSentryManager;
    private BirthdayManager birthdayManager;
    private DataManager dataManager;

    private List<VelocityCommand> commands;

    private boolean shuttingDown = false;

    @Inject
    public SGCraftLinkVelocity(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = new VelocityLogger(logger);
        this.server = server;
        this.dataFolder = dataDirectory.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent ignored) {
        SGCraftLinkCommon.PROXY = this;
        SGCraftLinkCommon.LOGGER = this.logger;
        ColorProvider.setProvider(new VelocityColor());
        ComponentProvider.setProvider(VelocityTextComponent::new);
        ComponentClickProvider.setProvider(VelocityClickEvent::new);
        ComponentHoverProvider.setProvider(VelocityHoverEvent::new);
        scheduler = new VelocityScheduler(this);
        shuttingDown = false;
        enable();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        shuttingDown = true;
        shutdown();
        disable();
    }

    public ProxyServer getProxy() {
        return server;
    }

    @Override
    public DiscordManager getDiscordProvider() {
        return discordManager;
    }

    @Override
    public VelocityConfigProvider getConfigProvider() {
        return configManager;
    }

    @Override
    public VelocityLinkProvider getLinkProvider() {
        return linkManager;
    }

    @Override
    public VelocityPlaytimeProvider getPlaytimeProvider() {
        return playtimeManager;
    }

    @Override
    public LuckPermsManager getLuckPermsManager() {
        return luckPermsManager;
    }

    @Override
    public VelocityBotSentryManager getBotSentryManager() {
        return botSentryManager;
    }

    @Override
    public VelocityPluginMessager getPluginMessager() {
        return pluginMessager;
    }

    @Override
    public VelocityScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public BirthdayManager getBirthdayManager() {
        return birthdayManager;
    }

    @Override
    public VelocityLogger getLoggerProvider() {
        return logger;
    }

    @Override
    public DataManager getDataManager() {
        return dataManager;
    }

    @Override
    public VelocityPerServerSkin getPerServerSkin() {
        return perServerSkin;
    }

    @Override
    public @NotNull PlayerProvider getPlayer(String name) {
        Player player = server.getPlayer(name).orElse(null);
        return player != null ? new VelocityPlayer(player) : new VelocityOfflinePlayer(name);
    }

    @Override
    public PlayerProvider getPlayer(UUID uuid) {
        Player player = server.getPlayer(uuid).orElse(null);
        return player != null ? new VelocityPlayer(player) : null;
    }

    @Override
    public VelocityConsolePlayer getConsole() {
        return new VelocityConsolePlayer(server);
    }

    @Override
    public Collection<PlayerProvider> getPlayers() {
        return server.getAllPlayers().stream().map(VelocityPlayer::new).collect(Collectors.toList());
    }

    @Override
    public void dispatchCommand(String command) {
        try {
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<ServerProvider> getServers() {
        return server.getAllServers().stream().map(VelocityConnectedServer::new).collect(Collectors.toList());
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public void disable() {
        server.getEventManager().unregisterListeners(this);
        for (VelocityCommand command : commands) {
            server.getCommandManager().unregister(command.getMeta());
        }
        commands.clear();
        server.getChannelRegistrar().unregister(CHANNEL);
        getConfigProvider().save();
    }

    @Override
    public void enable() {
        try {
            pluginMessager = new VelocityPluginMessager(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            configManager = new VelocityConfigProvider(this, "config.yml");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            dataManager = new DataManager(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            luckPermsManager = new LuckPermsManager(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            linkManager = new VelocityLinkProvider(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            discordManager = new DiscordManager(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            playtimeManager = new VelocityPlaytimeProvider(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            birthdayManager = new BirthdayManager(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            perServerSkin = new VelocityPerServerSkin(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            botSentryManager = new VelocityBotSentryManager(this);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        server.getChannelRegistrar().register(CHANNEL);
        server.getEventManager().register(this, new VelocityCommandListener(this));
        server.getEventManager().register(this, new VelocityPMListener(this));
        server.getEventManager().register(this, new VelocityStaffListener(this));

        commands = new ArrayList<>();
        commands.add(new VelocityCommand(this, "discord", new DiscordCommand(this)));
        commands.add(new VelocityCommand(this, "sgc", new StaffCommand(this)));
        commands.add(new VelocityCommand(this, "birthday", new BirthdayCommand(this)));

        // Mimic BungeeCord commands
        commands.add(new VelocityCommand(this, "glist", new GList(this)));
        //commands.add(new VelocityCommand(this, "send", new Send(this)));

        try {
            VelocityASLPlaceholder pl = new VelocityASLPlaceholder(this);
            AdvancedServerListAPI.get().addPlaceholderProvider(pl);
            server.getEventManager().register(this, pl);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isShuttingDown() {
        return shuttingDown;
    }
}
