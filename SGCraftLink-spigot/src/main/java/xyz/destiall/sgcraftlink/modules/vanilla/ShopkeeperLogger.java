package xyz.destiall.sgcraftlink.modules.vanilla;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.hooks.DiscordSRVHook;

public class ShopkeeperLogger extends Module implements Listener {
    private DiscordSRVHook dsrv;
    public ShopkeeperLogger(SGCraftLinkSpigot plugin) {
        super(plugin);
        dsrv = null;
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {
        dsrv = plugin.getModule(DiscordSRVHook.class);
    }

    private String formatItem(UnmodifiableItemStack is) {
        String builder = "x" + is.getAmount() + " ";
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                builder += ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(meta.displayName()));
            } else {
                builder += is.getType().name();
            }
        } else {
            builder += is.getType().name();
        }
        return builder;
    }

    private String formatMessage(ShopkeeperTradeEvent e) {
        String builder = "[AdminShopkeeper|" + e.getShopkeeper().getName() + "] " + e.getPlayer().getName() + " traded " + formatItem(e.getOfferedItem1());

        if (e.getOfferedItem2() != null) {
            builder += " and " + formatItem(e.getOfferedItem2());
        }

        builder += " for ";
        if (e.getResultItem() != null) {
            builder += formatItem(e.getResultItem());
        } else {
            builder += "nothing :(";
        }

        return builder;
    }

    @EventHandler
    public void onTrade(ShopkeeperTradeEvent e) {
        if (dsrv == null)
            return;

        if (!(e.getShopkeeper() instanceof AdminShopkeeper))
            return;

        dsrv.sendMessageToChannel("1210765242175324180", formatMessage(e));
    }
}
