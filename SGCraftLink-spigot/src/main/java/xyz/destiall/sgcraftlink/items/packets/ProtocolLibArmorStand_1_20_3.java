package xyz.destiall.sgcraftlink.items.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProtocolLibArmorStand_1_20_3 extends PacketArmorStand {
    private PacketContainer handle;
    private PacketContainer metadata;
    private WrappedDataWatcher dataWatcher;

    public ProtocolLibArmorStand_1_20_3(World world, double x, double y, double z) {
        super(world, x, y, z);
        handle = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);

        // Entity ID
        handle.getIntegers().write(0, id);
        handle.getUUIDs().write(0, uuid);
        handle.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        handle.getDoubles().write(0, location.getX());
        handle.getDoubles().write(1, location.getY());
        handle.getDoubles().write(2, location.getZ());

        dataWatcher = new WrappedDataWatcher();

        metadata = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, id);
    }

    @Override
    public void createFor(Player player) {
        super.createFor(player);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, handle);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        final List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();

        for (final WrappedWatchableObject entry : dataWatcher.getWatchableObjects()) {
            if (entry == null) continue;

            final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = entry.getWatcherObject();
            wrappedDataValueList.add(new WrappedDataValue(watcherObject.getIndex(), watcherObject.getSerializer(), entry.getRawValue()));
        }

        metadata.getDataValueCollectionModifier().write(0, wrappedDataValueList);
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

    @Override
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

    @Override
    public void remove() {
        PacketContainer remove = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        remove.getIntLists().write(0, IntList.of(id));
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

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);

        handle.getDoubles().write(0, location.getX());
        handle.getDoubles().write(1, location.getY());
        handle.getDoubles().write(2, location.getZ());
    }

    @Override
    public void setGravity(boolean gravity) {
        setDataWatcherObject(5, !gravity);
        update();
    }

    @Override
    public void setSmall(boolean small) {
        if (small) {
            setDataWatcherObject(15, (byte) (0x1 | 0x4 | 0x08 | 0x10));
        } else {
            setDataWatcherObject(15, (byte) (0x4 | 0x08 | 0x10));
        }

        update();
    }

    @Override
    public void setInvisible(boolean invis) {
        if (invis) {
            setDataWatcherObject(0, (byte) 0x20);
        } else {
            setDataWatcherObject(0, (byte) 0);
        }

        update();
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        setDataWatcherObject(3, visible);

        update();
    }

    @Override
    public void setCustomName(String name) {
        setDataWatcherObject(2, WrappedChatComponent.fromChatMessage(name)[0]);

        update();
    }
}