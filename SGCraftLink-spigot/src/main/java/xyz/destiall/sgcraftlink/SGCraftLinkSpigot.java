package xyz.destiall.sgcraftlink;

import de.tr7zw.changeme.nbtapi.NBT;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.gamemode.MoveIfGay;
import xyz.destiall.sgcraftlink.gamemode.OnePersonDeath;
import xyz.destiall.sgcraftlink.items.SpigotLogger;
import xyz.destiall.sgcraftlink.listeners.AirBlockPlaceListener;
import xyz.destiall.sgcraftlink.listeners.BreakAnimationModule;
import xyz.destiall.sgcraftlink.listeners.PML;
import xyz.destiall.sgcraftlink.listeners.PlayerListener;
import xyz.destiall.sgcraftlink.listeners.PlaytimeGetter;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.common.BirthdayModule;
import xyz.destiall.sgcraftlink.modules.hooks.CMIHook;
import xyz.destiall.sgcraftlink.modules.common.ChatBubble;
import xyz.destiall.sgcraftlink.modules.common.Fonter;
import xyz.destiall.sgcraftlink.modules.common.HeadHunt;
import xyz.destiall.sgcraftlink.modules.common.HoverMessages;
import xyz.destiall.sgcraftlink.modules.common.MigrationModule;
import xyz.destiall.sgcraftlink.modules.hooks.PacketEventsHook;
import xyz.destiall.sgcraftlink.modules.common.Redeemables;
import xyz.destiall.sgcraftlink.modules.common.ScheduledCommands;
import xyz.destiall.sgcraftlink.modules.common.TagModule;
import xyz.destiall.sgcraftlink.modules.common.TriggerModule;
import xyz.destiall.sgcraftlink.modules.hooks.WorldGuardHook;
import xyz.destiall.sgcraftlink.modules.hooks.CoreProtectHook;
import xyz.destiall.sgcraftlink.modules.hooks.DiscordSRVHook;
import xyz.destiall.sgcraftlink.modules.hooks.DynmapHook;
import xyz.destiall.sgcraftlink.modules.hooks.EconomyHook;
import xyz.destiall.sgcraftlink.modules.hooks.LuckPermsHook;
import xyz.destiall.sgcraftlink.modules.hooks.PAPIModule;
import xyz.destiall.sgcraftlink.modules.lobby.LobbyModule;
import xyz.destiall.sgcraftlink.modules.skyblock.NovaEconomyGUIModule;
import xyz.destiall.sgcraftlink.modules.skyblock.NovaSkyblockModule;
import xyz.destiall.sgcraftlink.modules.survival.AuraSkillsHook;
import xyz.destiall.sgcraftlink.modules.survival.CustomFood;
import xyz.destiall.sgcraftlink.modules.survival.HealthBar;
import xyz.destiall.sgcraftlink.modules.survival.InvisFrames;
import xyz.destiall.sgcraftlink.modules.survival.MusicHook;
import xyz.destiall.sgcraftlink.modules.survival.MythicHook;
import xyz.destiall.sgcraftlink.modules.survival.MythicRecipes;
import xyz.destiall.sgcraftlink.modules.survival.PermissionEffects;
import xyz.destiall.sgcraftlink.modules.survival.ProtectionStonesModule;
import xyz.destiall.sgcraftlink.modules.survival.ResourcePackModule;
import xyz.destiall.sgcraftlink.modules.survival.TrashModule;
import xyz.destiall.sgcraftlink.modules.survival.VanillaAddon;
import xyz.destiall.sgcraftlink.modules.vanilla.Casino;
import xyz.destiall.sgcraftlink.modules.vanilla.LastLocation;
import xyz.destiall.sgcraftlink.modules.vanilla.PiggyBankModule;
import xyz.destiall.sgcraftlink.modules.vanilla.ReputationModule;
import xyz.destiall.sgcraftlink.utils.ClassSet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SGCraftLinkSpigot extends JavaPlugin implements CommandExecutor {
    private final Collection<Module> modules = new ClassSet<>();
    private final Collection<Class<? extends Module>> moduleClazzes = new HashSet<>();
    private Scheduler scheduler;

    @Override
    public void onLoad() {
        scheduler = new Scheduler(this);
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                modules.add(new WorldGuardHook(this));
            } catch (Exception e) {
                getLogger().warning("This version cannot run WorldGuard!");
                e.printStackTrace();
            }
        }
        if (getServer().getPluginManager().isPluginEnabled("packetevents")) {
            modules.add(new PacketEventsHook(this));
        }
        SGCraftLinkCommon.LOGGER = new SpigotLogger(this);
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

        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly!");
        }

        getCommand("sgcreload").setExecutor(this);

        try {
            enable();
        } catch (Exception e) {
            getLogger().warning("SGCraftLink cannot be loaded!");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("all")
    public void enable() {
        for (String cmd : Module.COMMANDS) {
            getCommand(cmd).setExecutor(null);
        }
        moduleClazzes.clear();
        Module.COMMANDS.clear();
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            moduleClazzes.add(EconomyHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            moduleClazzes.add(LuckPermsHook.class);
        }

        saveDefaultConfig();
        moduleClazzes.add(TagModule.class);
        moduleClazzes.add(HeadHunt.class);
        moduleClazzes.add(LastLocation.class);
        moduleClazzes.add(TriggerModule.class);
        moduleClazzes.add(Redeemables.class);
        moduleClazzes.add(TrashModule.class);
        moduleClazzes.add(ScheduledCommands.class);
        moduleClazzes.add(ReputationModule.class);
        moduleClazzes.add(BirthdayModule.class);
        moduleClazzes.add(MigrationModule.class);
        moduleClazzes.add(DataModule.class);
        moduleClazzes.add(PlayerListener.class);
        moduleClazzes.add(AirBlockPlaceListener.class);
        moduleClazzes.add(Casino.class);
        moduleClazzes.add(InvisFrames.class);
        moduleClazzes.add(ResourcePackModule.class);
        moduleClazzes.add(HealthBar.class);
        moduleClazzes.add(CustomFood.class);
        moduleClazzes.add(VanillaAddon.class);
        moduleClazzes.add(PermissionEffects.class);
        moduleClazzes.add(LobbyModule.class);
        moduleClazzes.add(BreakAnimationModule.class);

        // Gamemodes (hehe)
        moduleClazzes.add(OnePersonDeath.class);
        moduleClazzes.add(MoveIfGay.class);

        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            moduleClazzes.add(ChatBubble.class);
            moduleClazzes.add(Fonter.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("CMI")) {
            moduleClazzes.add(CMIHook.class);
        }

        /**
         * if (getServer().getPluginManager().getPlugin("TryMe") != null) { try {
         * modules.add(new TryMeModule(this)); } catch (Exception e) {
         * getLogger().warning("This version cannot run TryMeModule!"); } }
         **/

        if (getServer().getPluginManager().isPluginEnabled("PiggyBank")) {
            moduleClazzes.add(PiggyBankModule.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            moduleClazzes.add(DiscordSRVHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("CoreProtect")) {
            moduleClazzes.add(CoreProtectHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("dynmap")) {
            moduleClazzes.add(DynmapHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("Nova")) {
            if (getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                moduleClazzes.add(NovaSkyblockModule.class);
            }
            if (getServer().getPluginManager().isPluginEnabled("EconomyShopGUI")) {
                moduleClazzes.add(NovaEconomyGUIModule.class);
            }
        }

        if (getServer().getPluginManager().isPluginEnabled("AuraSkills")) {
            moduleClazzes.add(AuraSkillsHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            moduleClazzes.add(MythicHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicCrucible")) {
            moduleClazzes.add(MythicRecipes.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("ProtectionStones")) {
            moduleClazzes.add(ProtectionStonesModule.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("NoteBlockMusicPlayer")) {
            moduleClazzes.add(MusicHook.class);
        }

        if (getServer().getPluginManager().isPluginEnabled("Citizens")) {
            moduleClazzes.add(HoverMessages.class);
        }

        if (NBT.preloadApi()) {
            moduleClazzes.add(PlaytimeGetter.class);
        }

        // -=-=-=-=-=-=-=-=-=-=-=-=-=- Add all your modules before this line! -=-=-=-=-=-=-=-=-=-=-=-=-=-

        for (Class<? extends Module> clazz : moduleClazzes) {
            Module module;
            try {
                module = clazz.getDeclaredConstructor(SGCraftLinkSpigot.class).newInstance(this);
            } catch (Throwable e) {
                getLogger().warning("This version cannot run " + clazz.getSimpleName());
                getLogger().severe(e.getLocalizedMessage());
                continue;
            }

            this.modules.add(module);
            try {
                if (!module.isUniversallyCompatible()) {
                    testPaperOr1_8Above();
                }
            } catch (Throwable e) {
                getLogger().warning(module + " cannot run here!");
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
            Module m = new PAPIModule(this);
            this.modules.add(m);
            m.enable();
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

    @SuppressWarnings("all")
    public void disable() {
        for (Module module : modules) {
            module.disable();
        }

        HandlerList.unregisterAll(this);
        scheduler.cancelTasks();
        modules.removeIf(f -> !(f instanceof WorldGuardHook));
    }

    private Module getModule(CommandSender sender, String moduleName) {
        Class<? extends Module> moduleClass = moduleClazzes.stream()
                .filter(m -> m.getSimpleName().equalsIgnoreCase(moduleName))
                .findFirst().orElse(null);
        if (moduleClass == null) {
            sender.sendMessage(color("&cModule not found!"));
            return null;
        }

        return modules.stream().filter(m -> m.getClass().equals(moduleClass)).findFirst().orElse(null);
    }

    @SuppressWarnings("all")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            disable();
            reloadConfig();
            enable();
            sender.sendMessage(color("&aReloaded SGCraftLink"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&aSGCraft Modules:"));
            for (Class<? extends Module> clazz : moduleClazzes) {
                Module module = getModule(clazz);
                TextComponent component = new TextComponent(" - " + clazz.getSimpleName());
                component.setColor(module != null ? ChatColor.GREEN : ChatColor.RED);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "sgcreload " + clazz.getSimpleName()));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to reload " + clazz.getSimpleName())));
                sender.sendMessage(component);
            }
            return true;
        }

        String moduleName = null;
        boolean toggled = false;
        if (args[0].equalsIgnoreCase("toggle") && args.length > 1) {
            moduleName = args[1];
            Module module = getModule(sender, moduleName);
            if (module != null) {
                if (module.isPersistent()) {
                    sender.sendMessage(color("&cPersistent modules cannot be toggled!"));
                    return false;
                }

                module.toggle();
                module.saveConfig();
            } else {
                toggled = true;
            }
        }

        if (moduleName == null)
            moduleName = args[0];
        final String finalModuleName = moduleName;
        Class<? extends Module> moduleClass = moduleClazzes.stream()
                .filter(m -> m.getSimpleName().equalsIgnoreCase(finalModuleName))
                .findFirst().orElse(null);

        Module module = getModule(sender, moduleName);
        if (module != null) {
            if (module.isPersistent()) {
                sender.sendMessage(color("&cPersistent modules cannot be reloaded!"));
                return false;
            }

            module.disable();
            if (module instanceof Listener) {
                HandlerList.unregisterAll((Listener) module);
            }
            modules.remove(module);
        }
        boolean wasEnabled = module != null ? module.isEnabled() : false;

        try {
            module = moduleClass.getDeclaredConstructor(SGCraftLinkSpigot.class).newInstance(this);
        } catch (Throwable e) {
            sender.sendMessage(color("&cThis server version cannot run " + moduleClass.getSimpleName()));
            e.printStackTrace();
            return false;
        }

        modules.add(module);
        try {
            if (!module.isUniversallyCompatible()) {
                testPaperOr1_8Above();
            }
        } catch (Throwable e) {
            sender.sendMessage(color("&c" + module + " cannot run here!"));
            modules.remove(module);
            return false;
        }

        module.initConfig();
        if (!module.isEnabled() && !toggled) {
            modules.remove(module);
            if (wasEnabled) {
                sender.sendMessage(color("&aDisabled " + module));
            } else {
                sender.sendMessage(color("&aReloaded " + module + " but is now &cdisabled"));
            }
        } else {
            if (toggled) {
                module.toggle();
                module.saveConfig();
            }
            module.enable();
            if (module instanceof Listener) {
                try {
                    getServer().getPluginManager().registerEvents((Listener) module, this);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            sender.sendMessage(color("&aReloaded " + module));
        }

        Module papiHook = getModule(PAPIModule.class);
        if (papiHook != null) {
            papiHook.disable();
            papiHook.enable();
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length > 0) {
            return moduleClazzes.stream()
                    .map(Class::getSimpleName)
                    .filter(m -> m.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.onTabComplete(sender, command, alias, args);
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

    @SuppressWarnings("deprecation")
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
        } catch (Throwable ignored) {}

        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void testPaperOr1_8Above() throws ClassNotFoundException {
        Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
    }
}
