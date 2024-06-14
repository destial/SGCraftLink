package xyz.destiall.sgcraftlink.modules.survival;

import dev.espi.protectionstones.PSProtectBlock;
import dev.espi.protectionstones.ProtectionStones;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Collection;

public class ProtectionStonesModule extends Module implements Listener {
    public ProtectionStonesModule(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @Override
    public void loadDefaultConfig() {}

    public ItemStack getProtectionStone(String name) {
        for (PSProtectBlock block : ProtectionStones.getInstance().getConfiguredBlocks()) {
            if (block.alias.equalsIgnoreCase(name)) {
                return block.createItem();
            }
        }

        return null;
    }

    public Collection<PSProtectBlock> getBlocks() {
        return ProtectionStones.getInstance().getConfiguredBlocks();
    }

    @Override
    public void disable() {}
}
