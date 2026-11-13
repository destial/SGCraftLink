package xyz.destiall.sgcraftlink.items.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PacketEventsArmorStand extends PacketArmorStand {
    private WrapperPlayServerSpawnEntity handle;
    private WrapperPlayServerEntityMetadata metadata;
    private Map<Integer, EntityData<?>> dataWatcher;
    private Collection<UUID> users;

    private static final Map<Class<?>, EntityDataType<?>> REGISTRY;

    public PacketEventsArmorStand(World world, double x, double y, double z) {
        super(world, x, y, z);
        id = SpigotReflectionUtil.generateEntityId();
        handle = new WrapperPlayServerSpawnEntity(
                id,
                uuid,
                EntityTypes.ARMOR_STAND,
                new com.github.retrooper.packetevents.protocol.world.Location(x, y, z, 0f, 0f),
                0, 0, new Vector3d()
        );

        dataWatcher = new HashMap<>();
        metadata = new WrapperPlayServerEntityMetadata(id, Collections.emptyList());
        this.users = new HashSet<>();
    }

    @Override
    public void createFor(Player player) {
        super.createFor(player);
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) // ignore intellisense
            return;

        users.add(player.getUniqueId());
        try {
            user.sendPacket(handle);
            update();
            user.sendPacket(metadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        metadata.setEntityMetadata(new ArrayList<>(dataWatcher.values()));
    }

    private void setDataWatcherObject(int index, Object value) {
        if (Component.class.isAssignableFrom(value.getClass())) {
            dataWatcher.put(index, new EntityData<>(index, EntityDataTypes.ADV_COMPONENT, (Component) value));

        } else if (UUID.class.equals(value.getClass())) {
            dataWatcher.put(index, new EntityData<>(index, EntityDataTypes.OPTIONAL_UUID, Optional.of((UUID) value)));

        } else if (Vector.class.equals(value.getClass())) {
            dataWatcher.put(index, new EntityData<>(index, EntityDataTypes.VECTOR3F, new Vector3f((float) ((Vector) value).getX(), (float) ((Vector) value).getY(), (float) ((Vector) value).getZ())));

        } else if (Particle.class.equals(value.getClass())) {
            dataWatcher.put(index, new EntityData<>(
                    index,
                    EntityDataTypes.PARTICLE,
                    new com.github.retrooper.packetevents.protocol.particle.Particle<>(
                            SpigotConversionUtil.fromBukkitParticle((Enum<?>) value))));
        } else if (Double.class.equals(value.getClass())) {
            dataWatcher.put(index, new EntityData<>(index, EntityDataTypes.FLOAT, ((Number) value).floatValue()));
        } else {
            boolean updated = false;
            for (Class<?> key : REGISTRY.keySet()) {
                if (key.equals(value.getClass())) {
                    dataWatcher.put(index, new EntityData<>(index, (EntityDataType<Object>) REGISTRY.get(key), key.cast(value)));
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                for (Class<?> key : REGISTRY.keySet()) {
                    if (key.isAssignableFrom(value.getClass())) {
                        dataWatcher.put(index, new EntityData<>(index, (EntityDataType<Object>) REGISTRY.get(key), key.cast(value)));
                        break;
                    }
                }
            }
        }
        update();
    }

    @Override
    public void teleport(Location location) {
        setPos(location.getX() + offset.getX(), location.getY() + offset.getY(), location.getZ() + offset.getZ());

        EntityData<?> noGrav = dataWatcher.get(5);
        boolean onGround = noGrav == null || !Boolean.TRUE.equals(noGrav.getValue());

        WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(
                id,
                new com.github.retrooper.packetevents.protocol.world.Location(
                        this.location.getX(),
                        this.location.getY(),
                        this.location.getZ(),
                        0f, 0f
                ),
                onGround
        );

        for (UUID uuid : users) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(uuid);
            if (user == null) continue;
            try {
                user.sendPacket(teleport);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void remove() {
        WrapperPlayServerDestroyEntities remove = new WrapperPlayServerDestroyEntities(id);
        for (UUID uuid : users) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(uuid);
            if (user == null) continue;
            try {
                user.sendPacket(remove);
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
        users.clear();
        users = null;
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);

        handle.setPosition(new Vector3d(location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public void setGravity(boolean gravity) {
        setDataWatcherObject(5, !gravity);
    }

    @Override
    public void setSmall(boolean small) {
        setMask(15, (byte) 0x01, small);
    }

    @Override
    public void setInvisible(boolean invis) {
        setMask(0, (byte) 0x20, invis);
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        setDataWatcherObject(3, visible);
    }

    @Override
    public void setCustomName(String name) {
        if (name == null || name.isEmpty()) {
            dataWatcher.put(2, new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.empty()));
        } else {
            dataWatcher.put(2, new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(Component.text(name))));
        }
        update();
    }

    private void setMask(int index, byte bit, boolean enable) {
        byte current = 0;
        EntityData<?> existing = dataWatcher.get(index);
        if (existing != null && existing.getValue() instanceof Byte) {
            current = (Byte) existing.getValue();
        }
        byte updated = (byte) (enable ? (current | bit) : (current & ~bit));
        dataWatcher.put(index, new EntityData<>(index, EntityDataTypes.BYTE, updated));
        update();
    }

    static {
        REGISTRY = new HashMap<>();
        REGISTRY.put(Integer.class, EntityDataTypes.INT);
        REGISTRY.put(Boolean.class, EntityDataTypes.BOOLEAN);
        REGISTRY.put(Short.class, EntityDataTypes.SHORT);
        REGISTRY.put(Float.class, EntityDataTypes.FLOAT);
        REGISTRY.put(Byte.class, EntityDataTypes.BYTE);
        REGISTRY.put(String.class, EntityDataTypes.STRING);
        REGISTRY.put(Long.class, EntityDataTypes.LONG);
        REGISTRY.put(Component.class, EntityDataTypes.ADV_COMPONENT);
    }
}
