package xyz.destiall.sgcraftlink.modules.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class PacketEventsHook extends Module {
    public PacketEventsHook(SGCraftLinkSpigot plugin) {
        super(plugin);
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {}

    @Override
    public boolean isPersistent() {
        return true;
    }
}
