package xyz.destiall.sgcraftlink.items;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VanillaItem {
    private final Material type;
    private final List<String> addonLores;
    private final List<ItemFlag> addonItemFlags;
    private final String addonDisplay;
    private final int customModelData;
    private final Set<Material> addonDestroy;
    private final Set<Material> addonPlace;
    private final Set<Pair<Enchantment, Integer>> addonEnchants;

    public VanillaItem(Material type, ConfigurationSection section) {
        this.type = type;
        this.addonLores = section.getStringList("lores");
        this.addonDisplay = section.getString("display", null);
        this.customModelData = section.getInt("model", -1);
        this.addonItemFlags = section.getStringList("item-flags").stream().map(ItemFlag::valueOf).collect(Collectors.toList());
        this.addonDestroy = section.getStringList("can-destroy").stream().map(Material::matchMaterial).collect(Collectors.toSet());
        this.addonPlace = section.getStringList("can-place").stream().map(Material::matchMaterial).collect(Collectors.toSet());
        this.addonEnchants = section.getStringList("enchants").stream().map(e -> {
            int level = 0;
            String name = e;
            if (e.contains(":")) {
                level = Integer.parseInt(e.substring(e.indexOf(":")));
                name = e.substring(0, e.indexOf(":") - 1);
            }
            return new Pair<>(Enchantment.getByName(name), level);
        }).collect(Collectors.toSet());
    }

    public Material getType() {
        return type;
    }

    public ItemMeta setMeta(ItemMeta meta) {
        if (!addonLores.isEmpty()) {
            meta.setLore(addonLores.stream().map(SGCraftLinkSpigot::color).collect(Collectors.toList()));
        }
        if (addonDisplay != null) {
            meta.setDisplayName(SGCraftLinkSpigot.color(addonDisplay));
        }
        if (customModelData != -1) {
            meta.setCustomModelData(customModelData);
        }
        if (!addonItemFlags.isEmpty()) {
            meta.addItemFlags(addonItemFlags.toArray(new ItemFlag[0]));
        }
        if (!addonDestroy.isEmpty()) {
            meta.setCanDestroy(addonDestroy);
        }
        if (!addonPlace.isEmpty()) {
            meta.setCanPlaceOn(addonPlace);
        }
        if (!addonEnchants.isEmpty()) {
            for (Pair<Enchantment, Integer> enchants : addonEnchants) {
                meta.addEnchant(enchants.getKey(), enchants.getValue(), true);
            }
        }
        return meta;
    }
}
