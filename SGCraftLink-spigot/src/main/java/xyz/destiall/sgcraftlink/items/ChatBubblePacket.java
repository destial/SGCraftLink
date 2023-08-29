package xyz.destiall.sgcraftlink.items;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatBubblePacket {
    private static final AtomicInteger entityId = new AtomicInteger(-1);
    private PacketContainer handle;
    private PacketContainer metadata;
    private Location location;
    private WrappedDataWatcher dataWatcher;
    private Vector offset;
    private List<Player> players;

    private final int id;

    public ChatBubblePacket(World world, double x, double y, double z) {
        handle = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        id = entityId.getAndDecrement();
        if (id < -100) {
            entityId.set(-1);
        }
        UUID uuid = UUID.randomUUID();

        offset = new Vector();
        location = new Location(world, x + offset.getX(), y + offset.getY(), z + offset.getZ());

        // Entity ID
        handle.getIntegers().write(0, id);
        handle.getUUIDs().write(0, uuid);
        // Entity Type
        handle.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        // Set location
        handle.getDoubles().write(0, location.getX());
        handle.getDoubles().write(1, location.getY());
        handle.getDoubles().write(2, location.getZ());

        dataWatcher = new WrappedDataWatcher();

        metadata = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, id);

        players = new ArrayList<>();
    }

    public void createFor(Player player) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, handle);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadata);
            players.add(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update() {
        try {
            Class.forName("com.comphenix.protocol.wrappers.WrappedDataValue");
            final List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();

            for (final WrappedWatchableObject entry : dataWatcher.getWatchableObjects()) {
                if (entry == null) continue;

                final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = entry.getWatcherObject();
                wrappedDataValueList.add(new WrappedDataValue(watcherObject.getIndex(), watcherObject.getSerializer(), entry.getRawValue()));
            }

            metadata.getDataValueCollectionModifier().write(0, wrappedDataValueList);
        } catch (Exception e) {
            metadata.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        }
    }

    private void setDataWatcherObject(int index, Object value) {
        WrappedDataWatcher.Serializer serializer = null;
        try {
            serializer = WrappedDataWatcher.Registry.get(value.getClass());
        } catch (Exception e) {
            if (WrappedChatComponent.class.equals(value.getClass())) {
                serializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
                value = Optional.of(((WrappedChatComponent) value).getHandle());
            } else if (UUID.class.equals(value.getClass())) {
                serializer = WrappedDataWatcher.Registry.getUUIDSerializer(false);
            } else if (Vector.class.equals(value.getClass())) {
                serializer = WrappedDataWatcher.Registry.getVectorSerializer();
                value = new Vector3F((float) ((Vector) value).getX(), (float) ((Vector) value).getY(), (float) ((Vector) value).getZ());
            } else if (Particle.class.equals(value.getClass())) {
                serializer = WrappedDataWatcher.Registry.get(Integer.class);
                value = EnumWrappers.Particle.getByName(((Particle) value).name()).getId();
            }
        }
        if (serializer == null) {
            return;
        }
        dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer), value);
    }

    public Vector getOffset() {
        return offset;
    }

    public void teleport(Location location) {
        setPos(location.getX() + offset.getX(), location.getY() + offset.getY(), location.getZ() + offset.getZ());

        PacketContainer teleport = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        teleport.getIntegers().write(0, id);
        teleport.getDoubles().write(0, this.location.getX());
        teleport.getDoubles().write(1, this.location.getY());
        teleport.getDoubles().write(2, this.location.getZ());

        for (Player player : players) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, teleport);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void remove() {
        PacketContainer remove = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        try {
            remove.getIntLists().write(0, Collections.singletonList(id));
        } catch (Exception ignored) {
            remove.getIntegerArrays().write(0, new int[]{id});
        }
        for (Player player : players) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, remove);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        metadata = null;
        handle = null;
        dataWatcher = null;
        location = null;
        offset = null;
        players.clear();
        players = null;
    }

    public static void remove(Collection<ChatBubblePacket> armorStands) {
        try {
            PacketContainer remove = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            Collection<Player> players = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            for (ChatBubblePacket as : armorStands) {
                players.addAll(as.players);
                ids.add(as.id);
                as.metadata = null;
                as.handle = null;
                as.dataWatcher = null;
                as.location = null;
                as.offset = null;
                as.players.clear();
                as.players = null;
            }
            remove.getIntLists().write(0, ids);
            for (Player player : players) {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, remove);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            for (ChatBubblePacket as : armorStands) {
                as.remove();
            }
        }
    }

    public void setPos(double x, double y, double z) {
        location.setX(x);
        location.setY(y);
        location.setZ(z);

        handle.getDoubles().write(0, location.getX());
        handle.getDoubles().write(1, location.getY());
        handle.getDoubles().write(2, location.getZ());
    }

    public void setGravity(boolean gravity) {
        setDataWatcherObject(5, !gravity);
        update();
    }

    public void setSmall(boolean small) {
        if (small) {
            setDataWatcherObject(15, (byte) (0x1 | 0x4 | 0x08 | 0x10));
        } else {
            setDataWatcherObject(15, (byte) (0x4 | 0x08 | 0x10));
        }

        update();
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setInvisible(boolean invis) {
        if (invis) {
            setDataWatcherObject(0, (byte) 0x20);
        } else {
            setDataWatcherObject(0, (byte) 0);
        }

        update();
    }

    public void setCustomNameVisible(boolean visible) {
        setDataWatcherObject(3, visible);

        update();
    }

    public void setCustomName(String name) {
        setDataWatcherObject(2, WrappedChatComponent.fromChatMessage(name)[0]);

        update();
    }
}
