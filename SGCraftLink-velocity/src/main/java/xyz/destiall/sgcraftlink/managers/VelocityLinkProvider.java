package xyz.destiall.sgcraftlink.managers;

import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.destiall.sgcraftlink.providers.LinkProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConfiguration;

import java.util.Map;

public class VelocityLinkProvider extends LinkProvider {
    private final YamlConfigurationLoader provider;
    public VelocityLinkProvider(PluginProvider plugin) {
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

    @Override
    public VelocityConfiguration getData(String user) {
        Object data = database.get(user);
        if (data == null) return null;
        ConfigurationNode map;
        if (data instanceof String) {
            String id = (String) data;
            map = BasicConfigurationNode.root();
            try {
                map.node("id").set(id);
                map.node("messages").set(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            database.set(user, map);
            save();
        } else {
            map = BasicConfigurationNode.factory().createNode();
            Map<String, Object> hashMap = (Map<String, Object>) data;
            try {
                map.node("id").set(hashMap.get("id"));
                map.node("messages").set(hashMap.get("messages"));
                map.node("ip").set(hashMap.get("ip"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new VelocityConfiguration(map);
    }
}
