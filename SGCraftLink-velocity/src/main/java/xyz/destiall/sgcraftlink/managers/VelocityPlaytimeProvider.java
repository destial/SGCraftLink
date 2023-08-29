package xyz.destiall.sgcraftlink.managers;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import xyz.destiall.sgcraftlink.providers.PlaytimeProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConfiguration;

public class VelocityPlaytimeProvider extends PlaytimeProvider {
    private final YAMLConfigurationLoader provider;

    public VelocityPlaytimeProvider(PluginProvider plugin) {
        super(plugin);
        provider = YAMLConfigurationLoader.builder().setPath(databaseFile.toPath()).build();
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
