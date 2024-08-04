package xyz.destiall.sgcraftlink.modules.survival;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffectType;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.hooks.LuckPermsHook;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionEffects extends Module implements Listener {
    public PermissionEffects(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    public void applyEffects(Player player) {
        Collection<String> permissions;
        LuckPermsHook lp = plugin.getModule(LuckPermsHook.class);
        if (lp == null) {
            permissions = player.getEffectivePermissions().stream().filter(PermissionAttachmentInfo::getValue).map(PermissionAttachmentInfo::getPermission).collect(Collectors.toSet());
        } else {
            permissions = lp.getPermissions(player.getPlayer());
        }

        for (PotionEffectType type : Registry.EFFECT) {
            String name = "permeffects." + type.getName().toLowerCase();
            Set<String> effectPerms = permissions.stream().filter(p -> p.startsWith(name)).collect(Collectors.toSet());
            if (effectPerms.isEmpty())
                continue;

            int level = 0;
            for (String p : effectPerms) {
                try {
                    int l = Integer.parseInt(p.replace(name + ".", ""));
                    if (l > level) {
                        level = l;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            player.addPotionEffect(type.createEffect(Integer.MAX_VALUE, level));
        }
    }

    @EventHandler
    public void onMilk(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.MILK_BUCKET || e.getItem().getType() == Material.LEGACY_MILK_BUCKET) {
            plugin.getScheduler().runTaskLater(() -> applyEffects(e.getPlayer()), 3L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        plugin.getScheduler().runTaskLater(() -> applyEffects(e.getPlayer()), 3L);
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        plugin.getScheduler().runTaskLater(() -> applyEffects(e.getPlayer()), 3L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getScheduler().runTaskLater(() -> applyEffects(e.getPlayer()), 3L);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public void enable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyEffects(player);
        }
    }
}
