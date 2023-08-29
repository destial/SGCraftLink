package xyz.destiall.sgcraftlink.providers;

import java.io.File;
import java.io.InputStream;

public abstract class ConfigProvider<T extends ConfigSectionProvider<?>> {
    protected final String name;
    public ConfigProvider(String name) {
        this.name = name;
    }

    public abstract ConfigProvider<?> createConfig(String name);

    public abstract void create();

    public abstract String getString(String path);

    public abstract File getDataFolder();

    public abstract InputStream getResourceAsStream(String path);

    public abstract T getConfig();

    public abstract void save();
}
