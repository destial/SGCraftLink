package xyz.destiall.sgcraftlink.managers;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeConfiguration;
import xyz.destiall.sgcraftlink.providers.LinkProvider;

public class BungeeLinkProvider extends LinkProvider {
    private final ConfigurationProvider provider;

    public BungeeLinkProvider(SGCraftLinkWaterfall plugin) {
        super(plugin);
        provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        try {
            database = new BungeeConfiguration(provider.load(databaseFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save() {
        try {
            provider.save((Configuration) database.getBase(), databaseFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public BungeeConfiguration getData(String user) {
        Object data = database.get(user);
        if (data == null) return null;
        Configuration map;
        if (data instanceof String) {
            String id = (String) data;
            map = new Configuration();
            map.set("id", id);
            map.set("messages", 0);
            database.set(user, map);
            save();
        } else {
            map = (Configuration) data;
        }
        return new BungeeConfiguration(map);
    }
}
