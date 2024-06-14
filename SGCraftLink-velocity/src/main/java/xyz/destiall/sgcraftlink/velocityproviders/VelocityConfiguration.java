package xyz.destiall.sgcraftlink.velocityproviders;

import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import xyz.destiall.sgcraftlink.providers.ConfigSectionProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VelocityConfiguration extends ConfigSectionProvider<ConfigurationNode> {
    private final ConfigurationNode config;

    public VelocityConfiguration(ConfigurationNode config) {
        this.config = config;
    }

    @Override
    public int getInt(String key) {
        return config.node((Object[]) key.split("\\.")).getInt();
    }

    @Override
    public int getInt(String key, int def) {
        return config.node((Object[]) key.split("\\.")).getInt(def);
    }

    @Override
    public float getFloat(String key) {
        return config.node((Object[]) key.split("\\.")).getFloat();
    }

    @Override
    public float getFloat(String key, float def) {
        return config.node((Object[]) key.split("\\.")).getFloat(def);
    }

    @Override
    public long getLong(String key) {
        return config.node((Object[]) key.split("\\.")).getLong();
    }

    @Override
    public long getLong(String key, long def) {
        return config.node((Object[]) key.split("\\.")).getLong(def);
    }

    @Override
    public String getString(String key) {
        return config.node((Object[]) key.split("\\.")).getString();
    }

    @Override
    public String getString(String key, String def) {
        if (def == null)
            return getString(key);
        return config.node((Object[]) key.split("\\.")).getString(def);
    }

    @Override
    public List<String> getStringList(String key) {
        try {
            return config.node((Object[]) key.split("\\.")).getList(TypeToken.get(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public double getDouble(String key) {
        return config.node((Object[]) key.split("\\.")).getDouble();
    }

    @Override
    public double getDouble(String key, double def) {
        return config.node((Object[]) key.split("\\.")).getDouble(def);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.node((Object[]) key.split("\\.")).getBoolean();
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return config.node((Object[]) key.split("\\.")).getBoolean(def);
    }

    @Override
    public Collection<String> getKeys() {
        return config.childrenMap().keySet().stream().map(key -> (String) key).collect(Collectors.toList());
    }

    @Override
    public VelocityConfiguration getSection(String key) {
        return new VelocityConfiguration(config.node((Object[]) key.split("\\.")));
    }

    @Override
    public void set(String key, Object obj) {
        try {
            config.node((Object[]) key.split("\\.")).set(obj);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(String key) {
        try {
            return config.node((Object[]) key.split("\\.")).get(Object.class);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(String key, Object def) {
        try {
            if (def == null)
                return get(key);
            return config.node((Object[]) key.split("\\.")).get(Object.class, def);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(String key) {
        return !config.node((Object[]) key.split("\\.")).virtual();
    }

    @Override
    public ConfigurationNode getBase() {
        return config;
    }
}
