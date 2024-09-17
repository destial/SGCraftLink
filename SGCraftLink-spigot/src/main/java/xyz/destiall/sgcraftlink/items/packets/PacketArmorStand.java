package xyz.destiall.sgcraftlink.items.packets;

import com.comphenix.packetwrapper.wrappers.play.clientbound.WrapperPlayServerEntityDestroy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PacketArmorStand {
    protected static final AtomicInteger entityId = new AtomicInteger(-6969);
    protected final UUID uuid;
    protected final int id;
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

    public static void remove(Collection<PacketArmorStand> armorStands) {
        try {
            WrapperPlayServerEntityDestroy remove = new WrapperPlayServerEntityDestroy();
            Collection<Player> players = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            for (PacketArmorStand as : armorStands) {
                players.addAll(as.players);
                ids.add(as.id);
                as.location = null;
                as.offset = null;
                as.players.clear();
                as.players = null;
            }
            remove.setEntityIds(new IntArrayList(ids));
            for (Player player : players) {
                try {
                    remove.sendPacket(player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            for (PacketArmorStand as : armorStands) {
                as.remove();
            }
        }
    }

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
}
