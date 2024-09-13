package xyz.destiall.sgcraftlink;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.gamemode.MoveIfGay;
import xyz.destiall.sgcraftlink.gamemode.OnePersonDeath;
import xyz.destiall.sgcraftlink.items.ClassSet;
import xyz.destiall.sgcraftlink.listeners.AirBlockPlaceListener;
import xyz.destiall.sgcraftlink.listeners.PML;
import xyz.destiall.sgcraftlink.listeners.PlayerListener;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.common.BirthdayModule;
import xyz.destiall.sgcraftlink.modules.common.CMIModule;
import xyz.destiall.sgcraftlink.modules.common.ChatBubble;
import xyz.destiall.sgcraftlink.modules.common.Fonter;
import xyz.destiall.sgcraftlink.modules.common.HeadHunt;
import xyz.destiall.sgcraftlink.modules.common.HoverMessages;
import xyz.destiall.sgcraftlink.modules.common.MigrationModule;
import xyz.destiall.sgcraftlink.modules.common.Redeemables;
import xyz.destiall.sgcraftlink.modules.common.ScheduledCommands;
import xyz.destiall.sgcraftlink.modules.common.TagModule;
import xyz.destiall.sgcraftlink.modules.common.TriggerModule;
import xyz.destiall.sgcraftlink.modules.common.WorldGuardModule;
import xyz.destiall.sgcraftlink.modules.hooks.CoreProtectHook;
import xyz.destiall.sgcraftlink.modules.hooks.DiscordSRVHook;
import xyz.destiall.sgcraftlink.modules.hooks.DynmapHook;
import xyz.destiall.sgcraftlink.modules.hooks.EconomyHook;
import xyz.destiall.sgcraftlink.modules.hooks.LuckPermsHook;
import xyz.destiall.sgcraftlink.modules.hooks.PAPIHook;
import xyz.destiall.sgcraftlink.modules.skyblock.NovaEconomyGUIModule;
import xyz.destiall.sgcraftlink.modules.skyblock.NovaSkyblockModule;
import xyz.destiall.sgcraftlink.modules.survival.AuraSkillsHook;
import xyz.destiall.sgcraftlink.modules.survival.CustomFood;
import xyz.destiall.sgcraftlink.modules.survival.MythicRecipes;
import xyz.destiall.sgcraftlink.modules.survival.HealthBar;
import xyz.destiall.sgcraftlink.modules.survival.InvisFrames;
import xyz.destiall.sgcraftlink.modules.survival.MusicHook;
import xyz.destiall.sgcraftlink.modules.survival.MythicHook;
import xyz.destiall.sgcraftlink.modules.survival.PermissionEffects;
import xyz.destiall.sgcraftlink.modules.survival.ProtectionStonesModule;
import xyz.destiall.sgcraftlink.modules.survival.ResourcePackModule;
import xyz.destiall.sgcraftlink.modules.survival.TrashModule;
import xyz.destiall.sgcraftlink.modules.survival.VanillaAddon;
import xyz.destiall.sgcraftlink.modules.vanilla.Casino;
import xyz.destiall.sgcraftlink.modules.vanilla.LastLocation;
import xyz.destiall.sgcraftlink.modules.vanilla.PiggyBankModule;
import xyz.destiall.sgcraftlink.modules.vanilla.ReputationModule;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SGCraftLinkSpigot extends JavaPlugin implements CommandExecutor {
    private final Collection<Module> modules = new ClassSet<>();
    private PAPIHook papiHook;
    private Scheduler scheduler;

    @Override
    public void onLoad() {
        scheduler = new Scheduler(this);
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                modules.add(new WorldGuardModule(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run WorldGuard!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEnable() {
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
            e.printStackTrace();
        }
    }

    public void enable() {
        for (String cmd : Module.COMMANDS) {
            getCommand(cmd).setExecutor(null);
        }
        Collection<Class<? extends Module>> modules = new HashSet<>();
        Module.COMMANDS.clear();
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            modules.add(EconomyHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            modules.add(LuckPermsHook.class);
        }

        saveDefaultConfig();
        modules.add(TagModule.class);
        modules.add(HeadHunt.class);
        modules.add(LastLocation.class);
        modules.add(TriggerModule.class);
        modules.add(Redeemables.class);
        modules.add(TrashModule.class);
        modules.add(ScheduledCommands.class);
        modules.add(ReputationModule.class);
        modules.add(BirthdayModule.class);
        modules.add(MigrationModule.class);
        modules.add(DataModule.class);
        modules.add(PlayerListener.class);
        modules.add(AirBlockPlaceListener.class);
        modules.add(Casino.class);
        modules.add(InvisFrames.class);
        modules.add(ResourcePackModule.class);
        modules.add(HealthBar.class);
        modules.add(CustomFood.class);
        modules.add(VanillaAddon.class);
        modules.add(PermissionEffects.class);

        // Gamemodes (hehe)
        modules.add(OnePersonDeath.class);
        modules.add(MoveIfGay.class);

        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            modules.add(ChatBubble.class);
            modules.add(Fonter.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("CMI")) {
            modules.add(CMIModule.class);
        }

        /**
         * if (getServer().getPluginManager().getPlugin("TryMe") != null) { try {
         * modules.add(new TryMeModule(this)); } catch (Exception e) {
         * getLogger().warning("This version cannot run TryMeModule!"); } }
         **/

        if (getServer().getPluginManager().isPluginEnabled("PiggyBank")) {
            modules.add(PiggyBankModule.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            modules.add(DiscordSRVHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("CoreProtect")) {
            modules.add(CoreProtectHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("dynmap")) {
            modules.add(DynmapHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("Nova")) {
            if (getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                modules.add(NovaSkyblockModule.class);
            }
            if (getServer().getPluginManager().isPluginEnabled("EconomyShopGUI")) {
                modules.add(NovaEconomyGUIModule.class);
            }
        }

        if (getServer().getPluginManager().isPluginEnabled("AuraSkills")) {
            modules.add(AuraSkillsHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            modules.add(MythicHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicCrucible")) {
            modules.add(MythicRecipes.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("ProtectionStones")) {
            modules.add(ProtectionStonesModule.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("NoteBlockMusicPlayer")) {
            modules.add(MusicHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("Citizens")) {
            modules.add(HoverMessages.class);
        }

        for (Class<? extends Module> clazz : modules) {
            Module module;
            try {
                module = clazz.getDeclaredConstructor(SGCraftLinkSpigot.class).newInstance(this);
            } catch (Throwable e) {
                getLogger().warning("This version cannot run " + clazz.getSimpleName());
                e.printStackTrace();
                continue;
            }

            this.modules.add(module);
            try {
                if (!module.isUniversallyCompatible()) {
                    testPaperOr1_8Above();
                }
            } catch (Throwable e) {
                getLogger().warning("This version cannot run " + module);
                this.modules.remove(module);
            }

            module.initConfig();
            if (!module.isEnabled()) {
                this.modules.remove(module);
            }
        }

        for (Module module : this.modules) {
            module.enable();
            if (module instanceof Listener) {
                try {
                    getServer().getPluginManager().registerEvents((Listener) module, this);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                papiHook = new PAPIHook(this);
                papiHook.register();
            } catch (Exception e) {
                getLogger().warning("This version cannot run PAPI Hook!");
            }
        }

        getLogger().info("Registered " + this.modules.size() + " modules " + new JSONArray(this.modules.stream().map(Module::toString).collect(Collectors.toList())).toString().replace("\"", ""));
    }

    @Override
    public void onDisable() {
        disable();

        getCommand("sgcreload").setExecutor(null);

        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
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
        modules.removeIf(f -> !(f instanceof WorldGuardModule));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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

    public void dispatchCommand(String command) {
        dispatchCommand(getServer().getConsoleSender(), command, true);
    }

    public void dispatchCommand(String command, boolean replace) {
        dispatchCommand(getServer().getConsoleSender(), command, replace);
    }

    public void dispatchCommand(CommandSender player, String command, boolean replace) {
        final String cmd = replace ? command.replace("_", " ") : command;
        scheduler.runTask(() -> {
            getLogger().info("Executing command " + cmd);
            getServer().dispatchCommand(player, cmd);
        });
    }

    public void dispatchCommand(Player player, String command, boolean replace) {
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

    private static final Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]){6}>");
    private static final Pattern hexBracketPattern = Pattern.compile("\\{#([A-Fa-f0-9]){6}}");

    public static String color(String s) {
        try {
            testPaperOr1_8Above();
            Matcher matcher = hexPattern.matcher(s);
            while (matcher.find()) {
                final ChatColor hexColor = ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
                final String before = s.substring(0, matcher.start());
                final String after = s.substring(matcher.end());
                s = before + hexColor + after;
                matcher = hexPattern.matcher(s);
            }
            matcher = hexBracketPattern.matcher(s);
            while (matcher.find()) {
                final ChatColor hexColor = ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
                final String before = s.substring(0, matcher.start());
                final String after = s.substring(matcher.end());
                s = before + hexColor + after;
                matcher = hexBracketPattern.matcher(s);
            }
        } catch (Throwable e) {}

        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void testPaperOr1_8Above() throws ClassNotFoundException {
        Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
    }
}
