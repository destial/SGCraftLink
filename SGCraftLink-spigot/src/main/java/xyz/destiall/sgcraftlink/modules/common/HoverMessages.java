package xyz.destiall.sgcraftlink.modules.common;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.packets.PacketArmorStand;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.utils.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class HoverMessages extends Module implements Listener, CommandExecutor {
    private final NPCRegistry api;
    private final Map<Integer, String> messages;
    private final Map<UUID, Pair<Integer, PacketArmorStand>> seeing;
    public HoverMessages(SGCraftLinkSpigot plugin) {
        super(plugin, "hover-messages.yml");
        api = CitizensAPI.getNPCRegistry();
        messages = new HashMap<>();
        seeing = new ConcurrentHashMap<>();
        setCommandExecutor("hovermessages");
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("hover-distance", 3);
    }

    @Override
    public void disable() {
        messages.clear();
        for (Map.Entry<UUID, Pair<Integer, PacketArmorStand>> entry : seeing.entrySet()) {
            entry.getValue().getValue().remove();
        }
        seeing.clear();
        unsetCommandExecutor("hovermessages");
    }

    @Override
    public void enable() {
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null)
            return;

        for (String id : section.getKeys(false)) {
            String message = section.getString(id);
            messages.put(Integer.parseInt(id), message);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Entity entity = e.getPlayer().getTargetEntity(3);
        if (entity == null) {
            Pair<Integer, PacketArmorStand> pair = seeing.remove(e.getPlayer().getUniqueId());
            if (pair == null)
                return;

            pair.getValue().remove();
            return;
        }

        NPC npc = api.getNPC(entity);
        if (npc == null)
            return;

        String message = messages.get(npc.getId());
        if (message == null)
            return;

        Vector loc = null;
        try {
            loc = e.getPlayer().getTargetEntityInfo(config.getInt("hover-distance", 3)).getHitVector();
        } catch (NoSuchMethodError ex) {
            RayTraceResult rs = e.getPlayer().rayTraceEntities(config.getInt("hover-distance", 3));
            if (rs != null)
                loc = rs.getHitPosition();
        }

        if (loc == null) {
            loc = entity.getLocation().toVector();
            if (entity instanceof LivingEntity) {
                Vector bottom = entity.getLocation().toVector();
                Vector top = ((LivingEntity) entity).getEyeLocation().toVector();
                loc = bottom.add(top).divide(new Vector(2f, 2f, 2f));
            }
        }

        Pair<Integer, PacketArmorStand> pair = seeing.getOrDefault(e.getPlayer().getUniqueId(), new Pair<>(-1, null));
        if (pair.getKey() == npc.getId()) {
            pair.getValue().teleport(loc.toLocation(entity.getWorld()));
            return;
        }

        PacketArmorStand as = PacketArmorStand.get(entity.getWorld(), loc.getX(), loc.getY(), loc.getZ());
        as.setCustomName(color(message));
        as.setCustomNameVisible(true);
        as.setInvisible(true);
        as.setSmall(true);
        as.setGravity(false);
        as.createFor(e.getPlayer());
        seeing.put(e.getPlayer().getUniqueId(), new Pair<>(npc.getId(), as));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        return false;
    }
}
