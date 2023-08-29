package xyz.destiall.sgcraftlink.modules;

import me.Zrips.TryMe.TryMe;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

public class TryMeModule extends Module {
    private final TryMe tryme;
    public TryMeModule(SGCraftLinkSpigot plugin) {
        super(plugin, null);
        tryme = TryMe.getInstance();
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}
}
