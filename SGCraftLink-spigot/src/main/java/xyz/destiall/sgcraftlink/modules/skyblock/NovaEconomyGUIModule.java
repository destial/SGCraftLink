package xyz.destiall.sgcraftlink.modules.skyblock;

import me.gypopo.economyshopgui.EconomyShopGUI;
import me.gypopo.economyshopgui.methodes.CreateItem;
import me.gypopo.economyshopgui.objects.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.xenondevs.nova.api.Nova;
import xyz.xenondevs.nova.api.item.NovaItem;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NovaEconomyGUIModule extends Module {
    private CreateItem overrideCreateItem;
    private CreateItem previousCreateItem;

    public NovaEconomyGUIModule(SGCraftLinkSpigot plugin) {
        super(plugin, "nova-worth.yml");
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
        saveConfig();
        return -1D;
    }

    public Double getBuyingPrice(NovaItem novaItem) {
        String key = novaItem.getId().toNamespacedKey().asString();
        if (config.contains(key + ".buy")) {
            return config.getDouble(key + ".buy");
        }
        config.set(key + ".buy", -1D);
        saveConfig();
        return -1D;
    }

    public class NovaExtendedCreateItem extends CreateItem {
        private final Map<String, ShopItem> shopItems;

        public NovaExtendedCreateItem(EconomyShopGUI plugin) {
            super(plugin);
            this.shopItems = new HashMap<>();
            Set<String> keys = new HashSet<>(config.getKeys(false));
            for (String key : keys) {
                NovaItem novaItem = Nova.getNova().getItemRegistry().getOrNull(key);
                if (novaItem == null) {
                    plugin.getLogger().warning("Nova item " + key + " does not exist anymore! Removing as shop item...");
                    config.set(key, null);
                    continue;
                }

                NovaExtendedShopItem shopItem = new NovaExtendedShopItem(matchShopItem(new ItemStack(Material.STONE)));
                shopItem.init(novaItem);
                shopItems.put(key, shopItem);
            }
        }

        @Override
        public ShopItem matchShopItem(ItemStack itemToLookFor) {
            NovaItem novaItem = Nova.getNova().getItemRegistry().getOrNull(itemToLookFor);
            if (novaItem == null)
                return super.matchShopItem(itemToLookFor);

            String key = novaItem.getId().toNamespacedKey().asString();
            shopItems.computeIfAbsent(key, (k) -> {
                config.set(key + ".sell", -1D);
                config.set(key + ".buy", -1D);
                saveConfig();
                NovaExtendedShopItem shopItem = new NovaExtendedShopItem(matchShopItem(new ItemStack(Material.STONE)));
                shopItem.init(novaItem);
                plugin.getLogger().warning("Creating NovaShopItem = " + key);
                return shopItem;
            });

            return shopItems.get(key);
        }

        @Override
        public ShopItem matchShopItem(ItemStack itemToLookFor, Player player, String root) {
            NovaItem novaItem = Nova.getNova().getItemRegistry().getOrNull(itemToLookFor);
            if (novaItem == null)
                return super.matchShopItem(itemToLookFor, player, root);

            String key = novaItem.getId().toNamespacedKey().asString();
            shopItems.computeIfAbsent(key, (k) -> {
                config.set(key + ".sell", -1D);
                config.set(key + ".buy", -1D);
                saveConfig();
                NovaExtendedShopItem shopItem = new NovaExtendedShopItem(matchShopItem(new ItemStack(Material.STONE)));
                shopItem.init(novaItem);
                plugin.getLogger().warning("Creating NovaShopItem = " + key);
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

    public class NovaExtendedShopItem extends ShopItem {
        private NovaItem novaItem;
        public NovaExtendedShopItem(ShopItem placeholder) {
            super(placeholder);
        }

        public void init(NovaItem novaItem) {
            this.novaItem = novaItem;
        }

        @Override
        public double getBuyPrice() {
            return getBuyingPrice(novaItem);
        }

        @Override
        public double getSellPrice() {
            return getSellingPrice(novaItem);
        }

        @Override
        public double getBuyPrice(Player player) {
            return getBuyingPrice(novaItem);
        }

        @Override
        public double getSellPrice(Player player) {
            return getSellingPrice(novaItem);
        }

        @Override
        public String getName() {
            return novaItem.getId().getName();
        }

        @Override
        public boolean match(ItemStack item) {
            NovaItem other = Nova.getNova().getItemRegistry().getOrNull(item);
            if (other == null)
                return false;
            return other.getId().toNamespacedKey().asString().equals(novaItem.getId().toNamespacedKey().asString());
        }
    }

    public void inject() {
        plugin.getLogger().warning("Injecting Nova into EconomyGUIShop!");
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
        plugin.getLogger().warning("Reverting Nova from EconomyGUIShop!");
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

    @Override
    public void enable() {
        inject();
    }
}
