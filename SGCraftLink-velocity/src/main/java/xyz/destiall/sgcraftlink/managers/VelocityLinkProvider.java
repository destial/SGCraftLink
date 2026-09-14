package xyz.destiall.sgcraftlink.managers;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
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
        if (data == null)
            return null;
        ConfigurationNode map;
        if (data instanceof String) {
            String id = (String) data;
            map = ((VelocityConfigProvider)plugin.getConfigProvider()).getFactory().createNode();
            try {
                map.node("id").set(id);
                map.node("messages").set(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            database.set(user, map);
            save();
        } else {
            map = ((VelocityConfigProvider)plugin.getConfigProvider()).getFactory().createNode();
            Map<String, Object> hashMap = (Map<String, Object>) data;
            hashMap.forEach((key, value) -> {
                try {
                    map.node(key).set(value);
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return new VelocityConfiguration(map);
    }
}
