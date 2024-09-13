package xyz.destiall.sgcraftlink.modules.survival;

import io.lumine.mythiccrucible.MythicCrucible;
import io.lumine.mythiccrucible.items.recipes.Recipe;
import io.lumine.mythiccrucible.items.recipes.RecipeItem;
import io.lumine.mythiccrucible.items.recipes.better.BetterRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class MythicRecipes extends Module implements Listener {
    public MythicRecipes(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public void enable() {}

    @EventHandler
    public void onCraft(CraftItemEvent e) {}

    @EventHandler
    public void onPickUp(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;

        Player player = (Player) e.getEntity();
        discoverCustomRecipes(player, e.getItem().getItemStack());
    }

    public void discoverCustomRecipes(Player player, ItemStack itemStack) {
        List<NamespacedKey> keys = new ArrayList<>();

        for (BetterRecipe recipe : MythicCrucible.inst().getItemManager().getRecipeManager().getBetterShapedRecipes()) {
            for (int row = 0; row < recipe.getRecipeMatrix().getRows(); row++) {
                for (int col = 0; col < recipe.getRecipeMatrix().getColumns(); col++) {
                    RecipeItem item = recipe.getRecipeMatrix().getItem(row, col);
                    if (item == null)
                        continue;

                    if (item.matches(itemStack)) {
                        keys.add(((Recipe) recipe).getNamespacedKey());
                    }
                }
            }
        }
        player.discoverRecipes(keys);
    }
}
