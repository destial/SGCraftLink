package xyz.destiall.sgcraftlink.items.packets;

import com.comphenix.packetwrapper.wrappers.play.clientbound.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.wrappers.play.clientbound.WrapperPlayServerEntityMetadata;
import com.comphenix.packetwrapper.wrappers.play.clientbound.WrapperPlayServerEntityTeleport;
import com.comphenix.packetwrapper.wrappers.play.clientbound.WrapperPlayServerSpawnEntity;
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

public class PacketWrapperArmorStand extends PacketArmorStand {
    private WrapperPlayServerSpawnEntity handle;
    private WrapperPlayServerEntityMetadata metadata;
    private WrappedDataWatcher dataWatcher;

    public PacketWrapperArmorStand(World world, double x, double y, double z) {
        super(world, x, y, z);
        handle = new WrapperPlayServerSpawnEntity();

        // Entity ID
        handle.setId(id);
        handle.setUuid(uuid);
        handle.setType(EntityType.ARMOR_STAND);
        handle.setX(location.getX());
        handle.setY(location.getY());
        handle.setZ(location.getZ());

        dataWatcher = new WrappedDataWatcher();

        metadata = new WrapperPlayServerEntityMetadata();
        metadata.setId(id);
    }

    @Override
    public void createFor(Player player) {
        super.createFor(player);
        try {
            handle.sendPacket(player);
            metadata.sendPacket(player);
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

        metadata.setPackedItems(wrappedDataValueList);
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
        update();
    }

    @Override
    public void teleport(Location location) {
        setPos(location.getX() + offset.getX(), location.getY() + offset.getY(), location.getZ() + offset.getZ());

        WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport();
        teleport.setId(id);
        teleport.setX(this.location.getX());
        teleport.setY(this.location.getY());
        teleport.setZ(this.location.getZ());

        for (Player player : players) {
            try {
                teleport.sendPacket(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void remove() {
        WrapperPlayServerEntityDestroy remove = new WrapperPlayServerEntityDestroy();
        remove.setEntityIds(IntList.of(id));
        for (Player player : players) {
            try {
                remove.sendPacket(player);
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

        handle.setX(location.getX());
        handle.setY(location.getY());
        handle.setZ(location.getZ());
    }

    @Override
    public void setGravity(boolean gravity) {
        setDataWatcherObject(5, !gravity);
    }

    @Override
    public void setSmall(boolean small) {
        if (small) {
            setDataWatcherObject(15, (byte) (0x1 | 0x4 | 0x08 | 0x10));
        } else {
            setDataWatcherObject(15, (byte) (0x4 | 0x08 | 0x10));
        }
    }

    @Override
    public void setInvisible(boolean invis) {
        if (invis) {
            setDataWatcherObject(0, (byte) 0x20);
        } else {
            setDataWatcherObject(0, (byte) 0);
        }
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        setDataWatcherObject(3, visible);
    }

    @Override
    public void setCustomName(String name) {
        setDataWatcherObject(2, WrappedChatComponent.fromChatMessage(name)[0]);
    }
}
