package xyz.destiall.sgcraftlink.managers;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeConfiguration;
import xyz.destiall.sgcraftlink.providers.PlaytimeProvider;

public class BungeePlaytimeProvider extends PlaytimeProvider {
    private final ConfigurationProvider provider;

    public BungeePlaytimeProvider(SGCraftLinkWaterfall plugin) {
        super(plugin);
        provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        try {
            database = new BungeeConfiguration(provider.load(databaseFile));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void save() {
        try {
            provider.save((Configuration) database.getBase() , databaseFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
