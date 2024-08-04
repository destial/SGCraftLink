package xyz.destiall.sgcraftlink.managers;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeConfiguration;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.ConfigSectionProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class BungeeConfigProvider extends ConfigProvider<BungeeConfiguration> {
    private final SGCraftLinkWaterfall plugin;
    private final ConfigurationProvider provider;
    private BungeeConfiguration config;

    public BungeeConfigProvider(SGCraftLinkWaterfall plugin, String name) {
        super(name);
        this.plugin = plugin;
        provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        create();
    }

    @Override
    public BungeeConfigProvider createConfig(String name) {
        return new BungeeConfigProvider(plugin, name);
    }

    @Override
    public ConfigSectionProvider<?> createSection() {
        return new BungeeConfiguration(new Configuration());
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
            config = new BungeeConfiguration(provider.load(configFile));
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
        return plugin.getResourceAsStream(path);
    }

    @Override
    public BungeeConfiguration getConfig() {
        return config;
    }

    @Override
    public void save() {
        try {
            provider.save(config.getBase(), new File(getDataFolder(), name));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
