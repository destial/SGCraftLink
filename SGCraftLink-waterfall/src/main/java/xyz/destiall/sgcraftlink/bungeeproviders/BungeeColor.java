package xyz.destiall.sgcraftlink.bungeeproviders;

import xyz.destiall.sgcraftlink.providers.ColorProvider;

public class BungeeColor extends ColorProvider {

    @Override
    public String color(String s) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
    }
}
