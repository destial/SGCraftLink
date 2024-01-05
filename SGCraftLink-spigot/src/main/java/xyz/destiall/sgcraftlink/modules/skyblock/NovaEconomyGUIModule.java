package xyz.destiall.sgcraftlink.modules.skyblock;

import me.gypopo.economyshopgui.EconomyShopGUI;
import me.gypopo.economyshopgui.methodes.CreateItem;
import me.gypopo.economyshopgui.objects.ShopItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.xenondevs.nova.api.Nova;
import xyz.xenondevs.nova.api.item.NovaItem;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class NovaEconomyGUIModule extends Module {
    private CreateItem overrideCreateItem;
    private CreateItem previousCreateItem;

    public NovaEconomyGUIModule(SGCraftLinkSpigot plugin) {
        super(plugin, "nova-worth.yml");
        if (!config.getBoolean("enabled"))
            return;
        inject();
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
    }

    public Double getSellingPrice(NovaItem novaItem) {
        String key = novaItem.getId().toNamespacedKey().asString();
        if (config.contains(key + ".sell")) {
            return config.getDouble(key + ".sell");
        }
        config.set(key + ".sell", -1D);
        save();
        return -1D;
    }

    public Double getBuyingPrice(NovaItem novaItem) {
        String key = novaItem.getId().toNamespacedKey().asString();
        if (config.contains(key + ".buy")) {
            return config.getDouble(key + ".buy");
        }
        config.set(key + ".buy", -1D);
        save();
        return -1D;
    }

    public class NovaExtendedCreateItem extends CreateItem {
        private final Map<String, ShopItem> shopItems;

        public NovaExtendedCreateItem(EconomyShopGUI plugin) {
            super(plugin);
            this.shopItems = new HashMap<>();
        }

        @Override
        public ShopItem matchShopItem(ItemStack itemToLookFor) {
            NovaItem novaItem = Nova.getNova().getItemRegistry().getOrNull(itemToLookFor);
            if (novaItem == null)
                return super.matchShopItem(itemToLookFor);

            String key = novaItem.getId().toNamespacedKey().asString();
            shopItems.computeIfAbsent(key, (k) -> {
                config.set(key + ".sell", -1);
                config.set(key + ".buy", -1);
                save();
                NovaExtendedShopItem shopItem = new NovaExtendedShopItem(matchShopItem(new ItemStack(Material.STONE)));
                shopItem.init(novaItem);
                return shopItem;
            });

            return shopItems.get(key);
        }

        @Override
        public double getShopItemSellPrice(ItemStack itemToSell, String itemPath) {
            NovaItem novaItem = Nova.getNova().getItemRegistry().getOrNull(itemToSell);
            if (novaItem == null)
                return super.getShopItemSellPrice(itemToSell, itemPath);

            return getSellingPrice(novaItem);
        }

        @Override
        public boolean isShopItem(ItemStack itemToMatch, ItemStack shopItem) {
            NovaItem novaItem = Nova.getNova().getItemRegistry().getOrNull(itemToMatch);
            if (novaItem == null)
                return super.isShopItem(itemToMatch, shopItem);
            String key = novaItem.getId().toNamespacedKey().asString();
            return config.contains(key);
        }
    }

    public static class NovaExtendedShopItem extends ShopItem {
        public NovaExtendedShopItem(ShopItem placeholder) {
            super(placeholder);
        }

        public void init(NovaItem novaItem) {

        }
    }

    public void inject() {
        EconomyShopGUI economyShopGUI = EconomyShopGUI.getInstance();
        previousCreateItem = economyShopGUI.createItem;
        overrideCreateItem = new NovaExtendedCreateItem(economyShopGUI);
        Class<?> economyShopGUIClazz = economyShopGUI.getClass();
        try {
            Field field = economyShopGUIClazz.getDeclaredField("createItem");
            field.setAccessible(true);
            field.set(economyShopGUI, overrideCreateItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void revert() {
        EconomyShopGUI economyShopGUI = EconomyShopGUI.getInstance();
        Class<?> economyShopGUIClazz = economyShopGUI.getClass();
        try {
            if (economyShopGUI.createItem == overrideCreateItem) {
                Field field = economyShopGUIClazz.getDeclaredField("createItem");
                field.setAccessible(true);
                field.set(economyShopGUI, previousCreateItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {
        if (!config.getBoolean("enabled"))
            return;
        revert();
    }
}
