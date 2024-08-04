package xyz.destiall.sgcraftlink.modules;

import org.bukkit.configuration.file.YamlConfiguration;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.io.File;

public abstract class Module {
    protected final SGCraftLinkSpigot plugin;
    protected YamlConfiguration config;
    protected File configFile;

    public Module(SGCraftLinkSpigot plugin) {
        this(plugin, null);
    }

    public Module(SGCraftLinkSpigot plugin, String configName) {
        this.plugin = plugin;

        if (configName == null)
            return;

        this.config = new YamlConfiguration();
        configFile = new File(plugin.getDataFolder(), configName);
        if (!configFile.exists()) {
            config.set("enabled", false);
            loadDefaultConfig();
            save();
        } else {
            load();
        }
    }

    public boolean isEnabled() {
        return config == null || config.getBoolean("enabled", true);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            config.load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
