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
        this.config = new YamlConfiguration();
        if (configName == null) {
            File testConfig = new File(plugin.getDataFolder(), this + ".yml");
            if (!testConfig.exists())
                return;

            configName = this + ".yml";
        }

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

    public void log(String log) {
        plugin.getLogger().info(this + ": " + log);
    }

    public void warn(String warn) {
        plugin.getLogger().warning(this + ": " + warn);
    }

    public void error(String error) {
        plugin.getLogger().severe(this + ": " + error);
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

    public void loadDefaultConfig() {}

    public void setConfig(String name) {
        if (configName == null) {
            configName = name;
            configFile = new File(plugin.getDataFolder(), configName);
        }
    }

    public abstract void disable();

    public abstract void enable();

    public boolean isUniversallyCompatible() {
        return false;
    }

    public boolean isPersistent() {
        return false;
    }

    public void toggle() {
        if (configFile == null) {
            setConfig(this + ".yml");
        }
        config.set("enabled", !isEnabled());
    }

    protected final boolean hasPlugin(String pl) {
        return plugin.hasPlugin(pl);
    }
}
