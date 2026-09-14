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
        return config.node(asPath(key)).getInt();
    }

    @Override
    public int getInt(String key, int def) {
        return config.node(asPath(key)).getInt(def);
    }

    @Override
    public float getFloat(String key) {
        return config.node(asPath(key)).getFloat();
    }

    @Override
    public float getFloat(String key, float def) {
        return config.node(asPath(key)).getFloat(def);
    }

    @Override
    public long getLong(String key) {
        return config.node(asPath(key)).getLong();
    }

    @Override
    public long getLong(String key, long def) {
        return config.node(asPath(key)).getLong(def);
    }

    @Override
    public String getString(String key) {
        return config.node(asPath(key)).getString();
    }

    @Override
    public String getString(String key, String def) {
        if (def == null)
            return getString(key);
        return config.node(asPath(key)).getString(def);
    }

    @Override
    public List<String> getStringList(String key) {
        try {
            return config.node(asPath(key)).getList(TypeToken.get(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public double getDouble(String key) {
        return config.node(asPath(key)).getDouble();
    }

    @Override
    public double getDouble(String key, double def) {
        return config.node(asPath(key)).getDouble(def);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.node(asPath(key)).getBoolean();
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return config.node(asPath(key)).getBoolean(def);
    }

    @Override
    public Collection<String> getKeys() {
        return config.childrenMap().keySet().stream().map(key -> (String) key).collect(Collectors.toList());
    }

    @Override
    public VelocityConfiguration getSection(String key) {
        return new VelocityConfiguration(config.node(asPath(key)));
    }

    @Override
    public void set(String key, Object obj) {
        try {
            ConfigurationNode node = config.node(asPath(key));
            if (obj instanceof VelocityConfiguration) {
                node.set(((VelocityConfiguration) obj).config);
            } else {
                node.set(obj);
            }
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String key) {
        set(key, null);
    }

    @Override
    public Object get(String key) {
        try {
            return config.node(asPath(key)).get(Object.class);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(String key, Object def) {
        try {
            if (def == null)
                return get(key);
            return config.node(asPath(key)).get(Object.class, def);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(String key) {
        return !config.node(asPath(key)).virtual();
    }

    @Override
    public ConfigurationNode getBase() {
        return config;
    }

    private Object[] asPath(String key) {
        return key.split("\\.");
    }
}
