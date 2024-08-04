package xyz.destiall.sgcraftlink.modules.survival;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.HashMap;
import java.util.Map;

public class CustomFood extends Module implements Listener {
    private final Map<String, Food> foods;

    public CustomFood(SGCraftLinkSpigot plugin) {
        super(plugin, "custom-food.yml");
        foods = new HashMap<>();
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("baguette.item", Material.BREAD.name());
        config.set("baguette.model", 11);
        config.set("baguette.restore", 7);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsume(FoodLevelChangeEvent e) {
        ItemStack item = e.getItem();
        if (item == null)
            return;

        int model = 0;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            model = meta.getCustomModelData();
        }

        for (Food food : foods.values()) {
            if (food.item.equals(item.getType()) && food.model == model) {
                if (food.level < 0) {
                    e.setFoodLevel(food.level);
                }
            }
        }
    }

    @Override
    public void disable() {
        foods.clear();
    }

    @Override
    public void enable() {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null)
                continue;

            Food f = new Food(Material.getMaterial(section.getString("item")), section.getInt("model", 0), section.getInt("restore", -1));
            foods.put(key, f);
        }
    }

    public static class Food {
        private final Material item;
        private final int model;
        private final int level;

        public Food(Material item, int model, int level) {
            this.item = item;
            this.model = model;
            this.level = level;
        }
    }
}
