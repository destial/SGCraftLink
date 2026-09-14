package xyz.destiall.sgcraftlink.providers;

import java.util.Collection;
import java.util.List;

public abstract class ConfigSectionProvider<T> {
    public abstract int getInt(String key);
    public abstract int getInt(String key, int def);

    public abstract float getFloat(String key);
    public abstract float getFloat(String key, float def);

    public abstract long getLong(String key);
    public abstract long getLong(String key, long def);

    public abstract String getString(String key);
    public abstract String getString(String key, String def);

    public abstract List<String> getStringList(String key);

    public abstract double getDouble(String key);
    public abstract double getDouble(String key, double def);

    public abstract boolean getBoolean(String key);
    public abstract boolean getBoolean(String key, boolean def);

    public abstract Collection<String> getKeys();

    public abstract ConfigSectionProvider<T> getSection(String key);

    public abstract void set(String key, Object obj);

    public abstract void remove(String key);

    public abstract Object get(String key);
    public abstract Object get(String key, Object def);

    public abstract boolean contains(String key);

    public abstract T getBase();
}
