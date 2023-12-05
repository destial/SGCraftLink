package xyz.destiall.sgcraftlink.managers;

import com.google.common.io.ByteStreams;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class VelocityConfigProvider extends ConfigProvider<VelocityConfiguration> {
    private final SGCraftLinkVelocity plugin;
    private VelocityConfiguration config;

    public VelocityConfigProvider(SGCraftLinkVelocity plugin, String name) {
        super(name);
        this.plugin = plugin;
        create();
    }

    @Override
    public VelocityConfigProvider createConfig(String name) {
        return new VelocityConfigProvider(plugin, name);
    }

    @Override
    public void create() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File configFile = new File(getDataFolder(), name);
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                try (InputStream is = getResourceAsStream(name);
                     OutputStream os = Files.newOutputStream(configFile.toPath())) {
                    ByteStreams.copy(is, os);
                }
            } catch (IOException e) {
                plugin.getLoggerProvider().error("Unable to load configuration file " + name);
            }
        }

        try {
            config = new VelocityConfiguration(YAMLConfigurationLoader.builder().setPath(configFile.toPath()).build().load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public VelocityConfiguration getConfig() {
        return config;
    }

    @Override
    public void save() {
        try {
            YAMLConfigurationLoader.builder().setPath(new File(getDataFolder(), name).toPath()).build().save(config.getBase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
