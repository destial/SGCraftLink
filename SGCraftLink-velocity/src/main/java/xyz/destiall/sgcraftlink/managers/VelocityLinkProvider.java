package xyz.destiall.sgcraftlink.managers;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import xyz.destiall.sgcraftlink.providers.LinkProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConfiguration;

public class VelocityLinkProvider extends LinkProvider {
    private final YAMLConfigurationLoader provider;
    public VelocityLinkProvider(PluginProvider plugin) {
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

    @Override
    public VelocityConfiguration getData(String user) {
        Object data = database.get(user);
        if (data == null) return null;
        ConfigurationNode map;
        if (data instanceof String) {
            String id = (String) data;
            map = ConfigurationNode.root();
            map.getNode("id").setValue(id);
            map.getNode("messages").setValue(0);
            database.set(user, map);
            save();
        } else {
            map = (ConfigurationNode) data;
        }
        return new VelocityConfiguration(map);
    }
}
