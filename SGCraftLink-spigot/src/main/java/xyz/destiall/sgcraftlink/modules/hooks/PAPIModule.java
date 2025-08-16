package xyz.destiall.sgcraftlink.modules.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class PAPIModule extends Module {
    private PAPIHook papiHook;

    public PAPIModule(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @SuppressWarnings("all")
    @Override
    public void disable() {
        try {
            papiHook.unregister();
        } catch (Throwable e) {
            PlaceholderAPI.registerExpansion(papiHook);
        }
    }

    @SuppressWarnings("all")
    @Override
    public void enable() {
        papiHook = new PAPIHook(plugin);
        try {
            papiHook.register();
        } catch (Throwable e) {
            PlaceholderAPI.registerExpansion(papiHook);
        }
    }
}
