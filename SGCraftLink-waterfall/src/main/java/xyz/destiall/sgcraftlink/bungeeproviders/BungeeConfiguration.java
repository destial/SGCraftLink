package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.config.Configuration;
import xyz.destiall.sgcraftlink.providers.ConfigSectionProvider;

import java.util.Collection;
import java.util.List;

public class BungeeConfiguration extends ConfigSectionProvider<Configuration> {
    private final Configuration config;
    public BungeeConfiguration(Configuration config) {
        this.config = config;
    }

    @Override
    public int getInt(String key) {
        return config.getInt(key);
    }

    @Override
    public int getInt(String key, int def) {
        return config.getInt(key, def);
    }

    @Override
    public float getFloat(String key) {
        return config.getFloat(key);
    }

    @Override
    public float getFloat(String key, float def) {
        return config.getFloat(key, def);
    }

    @Override
    public long getLong(String key) {
        return config.getLong(key);
    }

    @Override
    public long getLong(String key, long def) {
        return config.getLong(key, def);
    }

    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    @Override
    public String getString(String key, String def) {
        return config.getString(key, def);
    }

    @Override
    public List<String> getStringList(String key) {
        return config.getStringList(key);
    }

    @Override
    public double getDouble(String key) {
        return config.getDouble(key);
    }

    @Override
    public double getDouble(String key, double def) {
        return config.getDouble(key, def);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return config.getBoolean(key, def);
    }

    @Override
    public Collection<String> getKeys() {
        return config.getKeys();
    }

    @Override
    public ConfigSectionProvider<Configuration> getSection(String key) {
        return new BungeeConfiguration(config.getSection(key));
    }

    @Override
    public Configuration getBase() {
        return config;
    }

    @Override
    public void set(String key, Object obj) {
        config.set(key, obj);
    }

    @Override
    public Object get(String key) {
        return config.get(key);
    }

    @Override
    public Object get(String key, Object def) {
        return config.get(key, def);
    }

    @Override
    public boolean contains(String key) {
        return config.contains(key);
    }
}
