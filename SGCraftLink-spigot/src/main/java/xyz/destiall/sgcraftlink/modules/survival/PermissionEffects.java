package xyz.destiall.sgcraftlink.modules.survival;

import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Material;
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
    private LuckPermsHook lp;

    public PermissionEffects(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    public void applyEffects(Player player) {
        Collection<String> permissions;
        if (lp == null)
            lp = plugin.getModule(LuckPermsHook.class);

        if (lp == null) {
            permissions = player.getEffectivePermissions()
                    .stream()
                    .filter(PermissionAttachmentInfo::getValue)
                    .map(PermissionAttachmentInfo::getPermission)
                    .collect(Collectors.toSet());
        } else {
            permissions = lp.getPermissions(player.getPlayer());
        }

        for (PotionEffectType type : PotionEffectType.values()) {
            if (type == null || type.getName() == null)
                continue;

            String name = "permeffects." + type.getName().toLowerCase();
            Set<String> effectPerms = permissions
                    .stream()
                    .filter(p -> p.startsWith(name))
                    .collect(Collectors.toSet());
            if (effectPerms.isEmpty())
                continue;

            int level = 0;
            for (String p : effectPerms) {
                try {
                    int l = Integer.parseInt(p.replace(name + ".", ""));
                    if (l > level) {
                        level = l;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            player.addPotionEffect(type.createEffect(Integer.MAX_VALUE, level));
        }
    }

    @EventHandler
    public void onMilk(PlayerItemConsumeEvent e) {
        if (isMilkBucket(e.getItem().getType())) {
            plugin.getScheduler().runTaskLater(() -> applyEffects(e.getPlayer()), 3L);
        }
    }

    public boolean isMilkBucket(Material m) {
        if (m == Material.MILK_BUCKET)
            return true;

        try {
            return m == Material.LEGACY_MILK_BUCKET;
        } catch (Throwable ignored) {}

        return false;
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
    public void disable() {}

    @Override
    public void enable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyEffects(player);
        }

        lp = plugin.getModule(LuckPermsHook.class);
        if (lp == null)
            return;

        lp.getLuckperms().getEventBus().subscribe(NodeAddEvent.class, (e) -> {
            if (e.getNode().getKey().startsWith("permeffects.")) {
                if (e.getTarget() instanceof User) {
                    String username = ((User) e.getTarget()).getUsername();
                    if (username == null)
                        return;

                    applyEffects(plugin.getServer().getPlayer(username));
                    return;
                }

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    applyEffects(player);
                }
            }
        });
    }

    @Override
    public boolean isUniversallyCompatible() {
        return true;
    }
}
