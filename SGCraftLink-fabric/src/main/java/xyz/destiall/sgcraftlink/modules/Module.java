package xyz.destiall.sgcraftlink.modules;

import org.apache.logging.log4j.core.config.yaml.YamlConfiguration;
import org.apache.logging.log4j.core.config.yaml.YamlConfigurationFactory;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkFabric;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

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
            configFile = new File(mod.getDataFolder(), configName + ".json");
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
        try (FileOutputStream stream = new FileOutputStream(configFile)) {
            try (DataOutputStream out = new DataOutputStream(stream)) {
                out.writeUTF(config.toString(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try (FileInputStream stream = new FileInputStream(configFile)) {
            try (DataInputStream in = new DataInputStream(stream)) {
                String data = in.readUTF();

                config = new JSONObject(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDefaultConfig() {}

    public abstract void enable();

    public abstract void disable();
}