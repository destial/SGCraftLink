package xyz.destiall.sgcraftlink.modules.survival;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicReloadedEvent;
import io.lumine.mythic.bukkit.utils.Events;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.AuraSkillsManaMechanic;
import xyz.destiall.sgcraftlink.items.MythicItemAbility;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MythicItemAbilities extends Module implements Listener {
    private final MythicBukkit mythic;
    private List<MythicItemAbility> items;

    public MythicItemAbilities(SGCraftLinkSpigot plugin) {
        super(plugin, "item-abilities.yml");
        mythic = MythicBukkit.inst();

        if (plugin.getServer().getPluginManager().getPlugin("MythicCrucible") == null) {
            loadItems();
        } else {
            injectMechanics();
        }
    }

    public void loadItems() {
        items = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            Optional<MythicItem> mitem = mythic.getItemManager().getItem(key);
            mitem.ifPresent(mythicItem -> items.add(new MythicItemAbility(mythicItem, config.getConfigurationSection(key))));
        }
        plugin.getLogger().info("Registered " + items.size() + " mythic item abilities");
    }

    public void injectMechanics() {
        AuraSkillsHook as = plugin.getModule(AuraSkillsHook.class);
        if (as != null) {
            Events.subscribe(MythicConditionLoadEvent.class).handler((event) -> {
                String name = event.getConditionName().toUpperCase();
                if (name.equalsIgnoreCase("MANA") || name.equalsIgnoreCase("USEMANA")) {
                    event.register(new AuraSkillsManaMechanic.AuraSkillsManaCondition(as, event));
                }
            });
            Events.subscribe(MythicMechanicLoadEvent.class).handler((event) -> {
                String name = event.getMechanicName().toUpperCase();
                if (name.equalsIgnoreCase("MANA") || name.equalsIgnoreCase("USEMANA")) {
                    event.register(new AuraSkillsManaMechanic(as, event));
                }
            });
        }
    }

    @Override
    public void loadDefaultConfig() {
        config.set("volcanic_fire.hold", "setfire,ticks=20");
        config.set("volcanic_fire.off", "extinguish");
    }

    public MythicBukkit getMythic() {
        return mythic;
    }

    @EventHandler
    public void onReload(MythicReloadedEvent e) {
        disable();
        load();
        if (plugin.getServer().getPluginManager().getPlugin("MythicCrucible") == null) {
            loadItems();
        } else {
            injectMechanics();
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        if (items == null)
            return;
        ItemStack prevItem = e.getPlayer().getInventory().getItem(e.getPreviousSlot());
        if (prevItem != null) {
            for (MythicItemAbility i : items) {
                if (i.isSimilar(prevItem)) {
                    i.onOff(e.getPlayer());
                    break;
                }
            }
        }

        ItemStack heldItem = e.getPlayer().getInventory().getItem(e.getNewSlot());
        if (heldItem != null) {
            for (MythicItemAbility i : items) {
                if (i.isSimilar(heldItem)) {
                    i.onHold(e.getPlayer());
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventory(InventoryClickEvent e) {
        if (items == null)
            return;
        Player p = (Player) e.getWhoClicked();
        if (p.getInventory().equals(e.getClickedInventory())) {
            if (e.getSlot() == p.getInventory().getHeldItemSlot() || e.getHotbarButton() == p.getInventory().getHeldItemSlot()) {
                plugin.getScheduler().runTaskLater(() -> {
                    for (MythicItemAbility i : items) {
                        if (i.isSimilar(p.getInventory().getItemInMainHand()) || i.isSimilar(p.getInventory().getItemInOffHand())) {
                            i.onHold(p);
                            break;
                        }
                    }
                }, p, 1L);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (items == null)
            return;
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            for (MythicItemAbility i : items) {
                if (i.isSimilar(e.getItem().getItemStack())) {
                    plugin.getScheduler().runTaskLater(() -> {
                        if (i.isSimilar(p.getInventory().getItemInMainHand()) || i.isSimilar(p.getInventory().getItemInOffHand())) {
                            i.onHold(p);
                        }
                    }, p, 1L);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent e) {
        if (items == null)
            return;
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            ItemStack heldItem = e.getItem();
            if (heldItem != null) {
                for (MythicItemAbility i : items) {
                    if (i.isSimilar(heldItem)) {
                        i.onLeftClick(e.getPlayer(), e.getClickedBlock(), e.getBlockFace());
                        break;
                    }
                }
            }
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack heldItem = e.getItem();
            if (heldItem != null) {
                for (MythicItemAbility i : items) {
                    if (i.isSimilar(heldItem)) {
                        i.onRightClick(e.getPlayer(), e.getClickedBlock(), e.getBlockFace());
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (items == null)
            return;
        ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
        for (MythicItemAbility i : items) {
            if (i.isSimilar(heldItem)) {
                i.onSneak(e.getPlayer());
                break;
            }
        }

        heldItem = e.getPlayer().getInventory().getItemInOffHand();
        for (MythicItemAbility i : items) {
            if (i.isSimilar(heldItem)) {
                i.onSneak(e.getPlayer());
                break;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (items == null)
            return;
        ItemStack heldItem = e.getItemDrop().getItemStack();
        for (MythicItemAbility i : items) {
            if (i.isSimilar(heldItem)) {
                i.onDrop(e.getPlayer());
                break;
            }
        }
    }

    @EventHandler
    public void onWear(PlayerArmorChangeEvent e) {
        if (items == null)
            return;
        ItemStack prevItem = e.getOldItem();
        for (MythicItemAbility i : items) {
            if (i.isSimilar(prevItem)) {
                i.onOff(e.getPlayer());
                break;
            }
        }

        ItemStack woreItem = e.getNewItem();
        for (MythicItemAbility i : items) {
            if (i.isSimilar(woreItem)) {
                i.onWear(e.getPlayer());
                break;
            }
        }
    }

    @EventHandler
    public void onProjectile(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Projectile))
            return;

        Projectile projectile = (Projectile) e.getDamager();
        List<MetadataValue> meta = projectile.getMetadata("sgcraftlink-mmitem");
        if (meta.isEmpty())
            return;

        float damage = meta.get(0).asFloat();
        e.setDamage(damage);
    }

    @Override
    public void disable() {
        if (items == null)
            return;
        for (MythicItemAbility item : items) {
            item.clear();
        }
        items.clear();
    }
}
