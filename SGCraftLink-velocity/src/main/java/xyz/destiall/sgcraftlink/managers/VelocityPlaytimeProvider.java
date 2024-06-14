package xyz.destiall.sgcraftlink.managers;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.destiall.sgcraftlink.providers.PlaytimeProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConfiguration;

public class VelocityPlaytimeProvider extends PlaytimeProvider {
    private final YamlConfigurationLoader provider;

    public VelocityPlaytimeProvider(PluginProvider plugin) {
        super(plugin);
        provider = YamlConfigurationLoader.builder().path(databaseFile.toPath()).build();
        try {
            database = new VelocityConfiguration(provider.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save() {
        try {
            provider.save((ConfigurationNode) database.getBase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
