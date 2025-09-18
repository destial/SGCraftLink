package xyz.destiall.sgcraftlink.modules;

import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkFabric;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public abstract class Module {
    protected final SGCraftLinkFabric mod;
    protected String configName;
    protected JSONObject config;
    protected File configFile;

    public Module(SGCraftLinkFabric mod) {
        this(mod, null);
    }

    protected Module(SGCraftLinkFabric mod, String configName) {
        this.mod = mod;
        this.configName = configName;
        this.config = new JSONObject();
        if (configName != null) {
            configFile = new File(mod.getDataFolder(), configName);
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile();
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                loadDefaultConfig();

                save();
            } else {
                load();
            }
        }
    }

    public void log(String log) {
        mod.getLogger().info("{}: {}", this, log);
    }

    public void warn(String warn) {
        mod.getLogger().warn("{}: {}", this, warn);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public JSONObject getConfig() {
        return config;
    }

    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(config.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String data = builder.toString();
            mod.getLogger().info("Loaded {} from {}", data, configName);
            config = new JSONObject(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDefaultConfig() {}

    public abstract void enable();

    public abstract void disable();
}