package xyz.destiall.sgcraftlink;

import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.hooks.CoreProtectHook;
import xyz.destiall.sgcraftlink.hooks.DiscordSRVHook;
import xyz.destiall.sgcraftlink.hooks.DynmapHook;
import xyz.destiall.sgcraftlink.hooks.PAPIHook;
import xyz.destiall.sgcraftlink.listeners.AirBlockPlaceListener;
import xyz.destiall.sgcraftlink.listeners.PML;
import xyz.destiall.sgcraftlink.listeners.PlayerListener;
import xyz.destiall.sgcraftlink.modules.BirthdayModule;
import xyz.destiall.sgcraftlink.modules.CMIModule;
import xyz.destiall.sgcraftlink.modules.Casino;
import xyz.destiall.sgcraftlink.modules.ChatBubble;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.HeadHunt;
import xyz.destiall.sgcraftlink.modules.LastLocation;
import xyz.destiall.sgcraftlink.modules.LuckPermsModule;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.PiggyBankModule;
import xyz.destiall.sgcraftlink.modules.Redeemables;
import xyz.destiall.sgcraftlink.modules.ReputationModule;
import xyz.destiall.sgcraftlink.modules.ScheduledCommands;
import xyz.destiall.sgcraftlink.modules.Tags;
import xyz.destiall.sgcraftlink.modules.Triggers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SGCraftLinkSpigot extends JavaPlugin implements CommandExecutor {
    private List<Module> modules;
    private PAPIHook papiHook;
    private Scheduler scheduler;

    @Override
    public void onEnable() {
        scheduler = new Scheduler(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, SGCraftLinkCommon.CHANNEL);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        PML pml = new PML(this);
        if (getServer().getMessenger().registerIncomingPluginChannel(this, SGCraftLinkCommon.CHANNEL, pml).isValid()) {
            getLogger().info("Registered incoming plugin channel " + SGCraftLinkCommon.CHANNEL);
        }
        if (getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", pml).isValid()) {
            getLogger().info("Registered incoming plugin channel BungeeCord");
        }

        getCommand("sgcreload").setExecutor(this);

        try {
            enable();
        } catch (Exception e) {
            getLogger().warning("This version cannot be loaded!");
        }
    }

    public void testPaperOr1_8Above() throws Exception {
        Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
    }

    public void enable() {
        modules = new ArrayList<>();
        try {
            testPaperOr1_8Above();
            modules.add(new HeadHunt(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run HeadHunt!");
        }
        try {
            testPaperOr1_8Above();
            modules.add(new Tags(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run Tags!");
        }
        try {
            testPaperOr1_8Above();
            modules.add(new LastLocation(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run LastLocation!");
        }
        try {
            modules.add(new Triggers(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run Triggers!");
        }
        try {
            testPaperOr1_8Above();
            modules.add(new Redeemables(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run Redeemables!");
        }
        try {
            modules.add(new ScheduledCommands(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run ScheduledCommands!");
        }
        try {
            testPaperOr1_8Above();
            modules.add(new ReputationModule(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run Reputation!");
        }
        try {
            testPaperOr1_8Above();
            modules.add(new BirthdayModule(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run Birthdays!");
        }
        try {
            testPaperOr1_8Above();
            modules.add(new DataModule(this));
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().warning("This version cannot run Data Communication!");
        }

        modules.add(new PlayerListener(this));
        modules.add(new AirBlockPlaceListener(this));

        try {
            testPaperOr1_8Above();
            modules.add(new Casino(this));
        } catch (Exception e) {
            getLogger().warning("This version cannot run Casino!");
        }

        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            try {
                testPaperOr1_8Above();
                modules.add(new ChatBubble(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run ChatBubbles!");
            }
        }

        if (getServer().getPluginManager().getPlugin("CMI") != null) {
            try {
                modules.add(new CMIModule(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run CMIModule!");
            }
        }

        /**
         * if (getServer().getPluginManager().getPlugin("TryMe") != null) { try {
         * modules.add(new TryMeModule(this)); } catch (Exception e) {
         * getLogger().warning("This version cannot run TryMeModule!"); } }
         **/

        if (getServer().getPluginManager().getPlugin("PiggyBank") != null) {
            try {
                modules.add(new PiggyBankModule(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run PiggyBankModule!");
            }
        }

        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager()
                    .getRegistration(LuckPerms.class);
            if (provider != null) {
                try {
                    modules.add(new LuckPermsModule(this, provider.getProvider()));
                } catch (Exception e) {
                    getLogger().warning("This version cannot run LuckPermsModule!");
                }
            }
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                papiHook = new PAPIHook(this);
                papiHook.register();
            } catch (Exception e) {
                getLogger().warning("This version cannot run PAPI Hook!");
            }
        }

        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
            try {
                testPaperOr1_8Above();
                modules.add(new DiscordSRVHook(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run DiscordSRV Hook!");
            }
        }

        if (getServer().getPluginManager().getPlugin("CoreProtect") != null) {
            try {
                testPaperOr1_8Above();
                modules.add(new CoreProtectHook(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run CoreProtect Hook!");
            }
        }

        if (getServer().getPluginManager().getPlugin("dynmap") != null) {
            try {
                modules.add(new DynmapHook(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run Dynmap Hook!");
            }
        }

        for (Module module : modules) {
            if (module instanceof Listener) {
                try {
                    getServer().getPluginManager().registerEvents((Listener) module, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            getLogger().info("Registered module " + module.getClass().getSimpleName());
        }
    }

    @Override
    public void onDisable() {
        disable();

        getCommand("sgcreload").setExecutor(null);

        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        scheduler = null;
    }

    public void disable() {
        if (papiHook != null) {
            papiHook.unregister();
            papiHook = null;
        }

        for (Module module : modules) {
            module.disable();
        }

        HandlerList.unregisterAll(this);
        scheduler.cancelTasks();
        modules.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        disable();
        reloadConfig();
        enable();

        sender.sendMessage(color("&aReloaded SGCraftLink."));
        return false;
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz))
                return clazz.cast(module);
        }

        return null;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void executeServerCommand(String command) {
        executeServerCommand(getServer().getConsoleSender(), command, true);
    }

    public void executeServerCommand(String command, boolean replace) {
        executeServerCommand(getServer().getConsoleSender(), command, replace);
    }

    public void executeServerCommand(CommandSender player, String command, boolean replace) {
        final String cmd = replace ? command.replace("_", " ") : command;
        scheduler.runTask(() -> {
            getLogger().info("Executing command " + cmd);
            getServer().dispatchCommand(player, cmd);
        });
    }

    public void executeServerCommand(Player player, String command, boolean replace) {
        final String cmd = replace ? command.replace("_", " ") : command;
        scheduler.runTask(() -> {
            getLogger().info("Executing command " + cmd);
            getServer().dispatchCommand(player, cmd);
        }, player);
    }

    public void sendFirstJoin(Player player) {
        long time = player.getFirstPlayed();
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF(SGCraftLinkCommon.SET_FIRSTJOIN);
            out.writeUTF(player.getName());
            out.writeLong(time);
            player.sendPluginMessage(this, SGCraftLinkCommon.CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendJail(Player player, CMIModule.JailResult result) {
        sendJail(player, result.reason, result.until, result.duration);
    }

    public void sendJail(Player player, String reason, long until, long duration) {
        if (reason == null)
            return;

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF(SGCraftLinkCommon.SET_JAIL);
            out.writeUTF(player.getName());
            out.writeUTF(reason.replace(" ", "*"));
            out.writeLong(until);
            out.writeLong(duration);
            player.sendPluginMessage(this, SGCraftLinkCommon.CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]){6}>");
    private static final Pattern hexBracketPattern = Pattern.compile("\\{#([A-Fa-f0-9]){6}}");

    public static String color(String s) {
        Matcher matcher = hexPattern.matcher(s);
        while (matcher.find()) {
            final net.md_5.bungee.api.ChatColor hexColor = net.md_5.bungee.api.ChatColor
                    .of(matcher.group().substring(1, matcher.group().length() - 1));
            final String before = s.substring(0, matcher.start());
            final String after = s.substring(matcher.end());
            s = before + hexColor + after;
            matcher = hexPattern.matcher(s);
        }
        matcher = hexBracketPattern.matcher(s);
        while (matcher.find()) {
            final net.md_5.bungee.api.ChatColor hexColor = net.md_5.bungee.api.ChatColor
                    .of(matcher.group().substring(1, matcher.group().length() - 1));
            final String before = s.substring(0, matcher.start());
            final String after = s.substring(matcher.end());
            s = before + hexColor + after;
            matcher = hexBracketPattern.matcher(s);
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
