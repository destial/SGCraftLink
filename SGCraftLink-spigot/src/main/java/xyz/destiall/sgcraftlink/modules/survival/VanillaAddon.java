package xyz.destiall.sgcraftlink.modules.survival;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.VanillaItem;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.testPaperOr1_8Above;

public class VanillaAddon extends Module implements Listener {
    private Map<Material, VanillaItem> addons;
    private NamespacedKey namespacedKey;
    public VanillaAddon(SGCraftLinkSpigot plugin) {
        super(plugin, "vanilla-addons.yml");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHotBar(PlayerItemHeldEvent e) {
        if (!config.getBoolean("enabled", false))
            return;

        ItemStack stack = e.getPlayer().getInventory().getItem(e.getNewSlot());
        if (changeMeta(stack)) {
            e.getPlayer().getInventory().setItem(e.getNewSlot(), stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickUp(EntityPickupItemEvent e) {
        ItemStack itemStack = e.getItem().getItemStack();
        if (changeMeta(itemStack))
            e.getItem().setItemStack(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(BlockDropItemEvent e) {
        if (!isEnabled())
            return;

        for (Item entity : e.getItems()) {
            ItemStack itemStack = entity.getItemStack();
            if (changeMeta(itemStack))
                entity.setItemStack(itemStack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack itemStack = e.getItemDrop().getItemStack();
        if (changeMeta(itemStack))
            e.getItemDrop().setItemStack(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreativeInventory(InventoryCreativeEvent e) {
        ItemStack itemStack = e.getCursor();
        if (changeMeta(itemStack))
            e.setCursor(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent e) {
        List<ItemStack> drops = e.getDrops();
        for (ItemStack itemStack : drops) {
            changeMeta(itemStack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLootGeneration(LootGenerateEvent e) {
        List<ItemStack> loot = e.getLoot();
        boolean dirty = false;
        for (ItemStack itemStack : loot) {
            if (changeMeta(itemStack))
                dirty = true;
        }
        if (dirty)
            e.setLoot(loot);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryInteract(InventoryClickEvent e) {
        ItemStack currentItem = e.getCurrentItem();
        if (currentItem != null && changeMeta(currentItem)) {
            e.setCurrentItem(currentItem);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreCraft(PrepareItemCraftEvent e) {
        ItemStack itemStack = e.getInventory().getResult();
        if (itemStack != null && changeMeta(itemStack))
            e.getInventory().setResult(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmelt(FurnaceSmeltEvent e) {
        ItemStack itemStack = e.getResult();
        if (changeMeta(itemStack))
            e.setResult(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent e) {
        Entity caught = e.getCaught();
        if (caught == null)
            return;

        if (caught instanceof Item) {
            Item item = (Item) caught;
            ItemStack itemStack = item.getItemStack();
            if (changeMeta(itemStack))
                item.setItemStack(itemStack);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVillagerTrade(VillagerAcquireTradeEvent e) {
        MerchantRecipe recipe = e.getRecipe();
        List<ItemStack> itemStacks = e.getRecipe().getIngredients();
        boolean dirty = false;
        for (ItemStack itemStack : itemStacks) {
            if (changeMeta(itemStack))
                dirty = true;
        }

        ItemStack result = e.getRecipe().getResult();
        if (changeMeta(result))
            dirty = true;

        if (dirty) {
            MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier(), recipe.getDemand(), recipe.getSpecialPrice());
            newRecipe.setIngredients(itemStacks);
            e.setRecipe(newRecipe);
        }
    }

    // @EventHandler
    public void onRecipeBook(PlayerRecipeBookClickEvent e) {
        /* TODO:
           When clicking the recipe book, it will not automatically add ingredients that have custom data.
           Figure out a way that replaces the ingredients with our new ingredients.
        */

        Recipe recipe = plugin.getServer().getRecipe(e.getRecipe());
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
            boolean dirty = false;
            for (Map.Entry<Character, RecipeChoice> choice : shapedRecipe.getChoiceMap().entrySet()) {
                if (choice.getValue() == null)
                    continue;

                ItemStack itemStack = choice.getValue().getItemStack().clone();
                if (isAddon(itemStack)) {
                    dirty = true;
                    break;
                }
            }
            ItemStack result = recipe.getResult();
            if (isAddon(result))
                dirty = true;

            if (dirty) {
                CraftingInventory craftingInventory = (CraftingInventory) e.getPlayer().getOpenInventory().getTopInventory();
                e.getPlayer().getInventory().addItem(Arrays.stream(craftingInventory.getMatrix()).filter(Objects::nonNull).toArray(ItemStack[]::new));
                String[] rows = shapedRecipe.getShape();
                ItemStack[] matrix = new ItemStack[craftingInventory.getMatrix().length];
                int j = -1;
                for (String patternRow : rows) {
                    for (int index = 0; index < patternRow.length(); index++) {
                        char c = patternRow.charAt(index);
                        RecipeChoice choice = shapedRecipe.getChoiceMap().get(c);
                        j++;
                        if (choice == null)
                            continue;

                        ItemStack itemStack = choice.getItemStack().clone();
                        changeMeta(itemStack);
                        if (e.getPlayer().getInventory().removeItem(itemStack).isEmpty()) {
                            matrix[j] = itemStack;
                        }
                    }
                }

                craftingInventory.setMatrix(matrix);
            }
            return;
        }

        if (recipe instanceof ShapelessRecipe) {
            ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
            boolean dirty = false;
            for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
                ItemStack itemStack = choice.getItemStack();
                if (isAddon(itemStack)) {
                    dirty = true;
                    break;
                }
            }
            ItemStack result = recipe.getResult();
            if (isAddon(result))
                dirty = true;

            if (dirty) {
                CraftingInventory craftingInventory = (CraftingInventory) e.getPlayer().getOpenInventory().getTopInventory();
                e.getPlayer().getInventory().addItem(Arrays.stream(craftingInventory.getMatrix()).filter(Objects::nonNull).toArray(ItemStack[]::new));
                List<ItemStack> ingredients = new ArrayList<>();
                for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
                    ItemStack itemStack = choice.getItemStack().clone();
                    changeMeta(itemStack);
                    if (e.getPlayer().getInventory().removeItem(itemStack).isEmpty()) {
                        ingredients.add(itemStack);
                    }
                }

                craftingInventory.setMatrix(ingredients.toArray(new ItemStack[0]));
            }
        }
    }

    public boolean isAddon(ItemStack itemStack) {
        if (itemStack == null || !addons.containsKey(itemStack.getType()))
            return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return false;

        return !meta.hasCustomModelData() && !meta.hasLore() && !meta.hasDisplayName() && !meta.hasPlaceableKeys() && !meta.hasDestroyableKeys() && !meta.hasAttributeModifiers() && !meta.getPersistentDataContainer().has(namespacedKey);
    }

    public boolean changeMeta(ItemStack itemStack) {
        if (!isAddon(itemStack))
            return false;

        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BYTE, (byte) 1);
        VanillaItem item = addons.get(itemStack.getType());
        itemStack.setItemMeta(item.setMeta(meta));
        return true;
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
        config.set("addons." + Material.COAL + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
        config.set("addons." + Material.DIAMOND + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
        config.set("addons." + Material.EMERALD + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
        try {
            testPaperOr1_8Above();
            config.set("addons." + Material.LAPIS_LAZULI + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
            config.set("addons." + Material.RAW_GOLD + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
            config.set("addons." + Material.RAW_COPPER + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
            config.set("addons." + Material.RAW_IRON + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
            config.set("addons." + Material.NETHERITE_SCRAP + ".lores", Arrays.asList("&f", "&eRarity: &f&lCOMMON"));
        } catch (Throwable e) {}
        config.set("addons." + Material.NETHER_STAR + ".lores", Arrays.asList("&f", "&eRarity: &a&lUNCOMMON"));
        config.set("addons." + Material.DRAGON_EGG + ".lores", Arrays.asList("&f", "&eRarity: &a&lUNCOMMON"));
    }

    @Override
    public void disable() {
        if (!isEnabled())
            return;

        addons.clear();
    }

    @Override
    public void enable() {
        namespacedKey = new NamespacedKey(plugin, "vanilla-addon");

        addons = new HashMap<>();
        ConfigurationSection addonSection = config.getConfigurationSection("addons");
        if (addonSection == null)
            return;

        for (String key : addonSection.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null)
                continue;

            ConfigurationSection section = addonSection.getConfigurationSection(key);
            if (section == null)
                continue;

            VanillaItem vanillaItem = new VanillaItem(material, section);
            addons.put(material, vanillaItem);
        }

        plugin.getLogger().info("Registered " + addons.size() + " vanilla addons " + new JSONArray(addons.keySet().stream().map(Material::name).collect(Collectors.toList())).toString().replace("\"", ""));
    }
}
