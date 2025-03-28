package xyz.destiall.sgcraftlink.items.packets;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PacketArmorStand {
    protected static final AtomicInteger entityId = new AtomicInteger(-6969);
    protected static final MinecraftVersion v1_21_2 = new MinecraftVersion("1.21.2");
    protected final UUID uuid;
    protected int id;
    protected Location location;
    protected Vector offset;
    protected List<Player> players;

    public PacketArmorStand(World world, double x, double y, double z) {
        id = entityId.getAndDecrement();
        if (id < (-6969 - 100)) {
            entityId.set(-6969);
        }
        uuid = UUID.randomUUID();
        offset = new Vector();
        location = new Location(world, x + offset.getX(), y + offset.getY(), z + offset.getZ());
        players = new ArrayList<>();
    }

    public void createFor(Player player) {
        players.add(player);
    }

    public abstract void update();

    public Vector getOffset() {
        return offset;
    }

    public abstract void teleport(Location location);

    public abstract void remove();


    public void setPos(double x, double y, double z) {
        location.setX(x);
        location.setY(y);
        location.setZ(z);
    }

    public abstract void setGravity(boolean gravity);

    public abstract void setSmall(boolean small);

    public Location getLocation() {
        return location.clone();
    }

    public abstract void setInvisible(boolean invis);

    public abstract void setCustomNameVisible(boolean visible);

    public abstract void setCustomName(String name);

    public static PacketArmorStand get(World world, double x, double y, double z) {
        if (ProtocolLibrary.getProtocolManager().getMinecraftVersion().isAtLeast(v1_21_2))
            return new ProtocolLibArmorStand_1_21_2(world, x, y, z);
        else if (Bukkit.getServer().getPluginManager().isPluginEnabled("PacketWrapper"))
            return new PacketWrapperArmorStand(world, x, y, z);
        else
            return new ProtocolLibArmorStand_1_20_3(world, x, y, z);
    }
}
