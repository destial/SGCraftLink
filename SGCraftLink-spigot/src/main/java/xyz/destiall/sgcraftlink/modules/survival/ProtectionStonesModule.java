package xyz.destiall.sgcraftlink.modules.survival;

import dev.espi.protectionstones.PSProtectBlock;
import dev.espi.protectionstones.ProtectionStones;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Collection;
import java.util.stream.Collectors;

public class ProtectionStonesModule extends Module implements Listener {
    public ProtectionStonesModule(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

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

    public Collection<ItemStack> getBlockItems() {
        return ProtectionStones.getInstance().getConfiguredBlocks().stream().map(PSProtectBlock::createItem).collect(Collectors.toSet());
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {}
}
