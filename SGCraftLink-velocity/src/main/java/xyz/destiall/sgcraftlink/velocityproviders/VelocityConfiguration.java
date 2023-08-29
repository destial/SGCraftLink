package xyz.destiall.sgcraftlink.velocityproviders;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
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
        return config.getNode(key).getInt();
    }

    @Override
    public int getInt(String key, int def) {
        return config.getNode(key).getInt(def);
    }

    @Override
    public float getFloat(String key) {
        return config.getNode(key).getFloat();
    }

    @Override
    public float getFloat(String key, float def) {
        return config.getNode(key).getFloat(def);
    }

    @Override
    public long getLong(String key) {
        return config.getNode(key).getLong();
    }

    @Override
    public long getLong(String key, long def) {
        return config.getNode(key).getLong(def);
    }

    @Override
    public String getString(String key) {
        return config.getNode(key).getString();
    }

    @Override
    public String getString(String key, String def) {
        return config.getNode(key).getString(def);
    }

    @Override
    public List<String> getStringList(String key) {
        try {
            return config.getNode(key).getList(TypeToken.of(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public double getDouble(String key) {
        return config.getNode(key).getDouble();
    }

    @Override
    public double getDouble(String key, double def) {
        return config.getNode(key).getDouble(def);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.getNode(key).getBoolean();
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return config.getNode(key).getBoolean(def);
    }

    @Override
    public Collection<String> getKeys() {
        return config.getChildrenList().stream().map(node -> (String) node.getKey()).collect(Collectors.toList());
    }

    @Override
    public VelocityConfiguration getSection(String key) {
        return new VelocityConfiguration(config.getNode(key));
    }

    @Override
    public void set(String key, Object obj) {
        config.getNode(key).setValue(obj);
    }

    @Override
    public Object get(String key) {
        return config.getNode(key).getValue();
    }

    @Override
    public Object get(String key, Object def) {
        return config.getNode(key).getValue(def);
    }

    @Override
    public boolean contains(String key) {
        return !config.getNode(key).isVirtual();
    }

    @Override
    public ConfigurationNode getBase() {
        return config;
    }
}
