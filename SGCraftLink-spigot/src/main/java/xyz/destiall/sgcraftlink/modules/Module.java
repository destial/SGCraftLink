package xyz.destiall.sgcraftlink.modules;

import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class Module {
    public static Set<String> COMMANDS = new HashSet<>();
    protected final SGCraftLinkSpigot plugin;
    protected YamlConfiguration config;
    protected File configFile;
    protected String configName;

    public Module(SGCraftLinkSpigot plugin) {
        this(plugin, null);
    }

    protected Module(SGCraftLinkSpigot plugin, String configName) {
        this.plugin = plugin;
        this.configName = configName;
    }

    public void initConfig() {
        if (configName == null)
            return;

        this.config = new YamlConfiguration();
        configFile = new File(plugin.getDataFolder(), configName);
        if (!configFile.exists()) {
            loadDefaultConfig();
            if (!config.contains("enabled"))
                config.set("enabled", false);
            saveConfig();
        } else {
            loadConfig();
        }
    }

    public boolean isEnabled() {
        return config == null || config.getBoolean("enabled", true);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        try {
            config.load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setCommandExecutor(String command) {
        if (this instanceof CommandExecutor) {
            plugin.getCommand(command).setExecutor((CommandExecutor) this);
            COMMANDS.add(command);
        }
    }

    protected void unsetCommandExecutor(String command) {
        plugin.getCommand(command).setExecutor(null);
        COMMANDS.remove(command);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public final SGCraftLinkSpigot getPlugin() {
        return plugin;
    }

    public abstract void loadDefaultConfig();

    public abstract void disable();

    public abstract void enable();

    public boolean isUniversallyCompatible() {
        return false;
    }

    public boolean isPersistent() {
        return false;
    }
}
