package xyz.destiall.sgcraftlink.items;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import io.lumine.mythic.core.utils.jnbt.CompoundTag;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Fire;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.Scheduler;
import xyz.destiall.sgcraftlink.modules.hooks.EconomyHook;
import xyz.destiall.sgcraftlink.items.utils.ExpiryConcurrentHashMap;
import xyz.destiall.sgcraftlink.modules.survival.AuraSkillsHook;
import xyz.destiall.sgcraftlink.modules.survival.MythicHook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class MythicItemAbility {
    private final SGCraftLinkSpigot plugin;
    private final MythicItem mmItem;
    private final ItemStack baseItem;
    private final Collection<Map<String, String>> hold;
    private final Collection<Map<String, String>> off;
    private final Collection<Map<String, String>> leftClick;
    private final Collection<Map<String, String>> rightClick;
    private final Collection<Map<String, String>> wear;
    private final Collection<Map<String, String>> drop;
    private final Collection<Map<String, String>> sneak;

    private final Map<UUID, Map<String, Scheduler.Task>> tasks;

    public MythicItemAbility(MythicItem item, ConfigurationSection section) {
        plugin = ((SGCraftLinkSpigot) SGCraftLinkSpigot.getProvidingPlugin(SGCraftLinkSpigot.class));
        mmItem = item;
        baseItem = BukkitAdapter.adapt(item.generateItemStack(1));
        hold = new ArrayList<>();
        if (section.contains("hold")) {
            if (section.isString("hold")) {
                hold.add(parse(section.getString("hold", null)));
            } else {
                for (String h : section.getStringList("hold")) {
                    hold.add(parse(h));
                }
            }
        }

        if (!hold.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " hold events: " + new JSONArray(hold));

        off = new ArrayList<>();
        if (section.contains("off")) {
            if (section.isString("off")) {
                off.add(parse(section.getString("off", null)));
            } else {
                for (String h : section.getStringList("off")) {
                    off.add(parse(h));
                }
            }
        }

        if (!off.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " off-hold events: " + new JSONArray(off));

        leftClick = new ArrayList<>();
        if (section.contains("left-click")) {
            if (section.isString("left-click")) {
                leftClick.add(parse(section.getString("left-click", null)));
            } else {
                for (String h : section.getStringList("left-click")) {
                    leftClick.add(parse(h));
                }
            }
        }

        if (!leftClick.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " left-click events: " + new JSONArray(leftClick));

        rightClick = new ArrayList<>();
        if (section.contains("right-click")) {
            if (section.isString("right-click")) {
                rightClick.add(parse(section.getString("right-click", null)));
            } else {
                for (String h : section.getStringList("right-click")) {
                    rightClick.add(parse(h));
                }
            }
        }

        if (!rightClick.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " right-click events: " + new JSONArray(rightClick));

        drop = new ArrayList<>();
        if (section.contains("drop")) {
            if (section.isString("drop")) {
                drop.add(parse(section.getString("drop", null)));
            } else {
                for (String h : section.getStringList("drop")) {
                    drop.add(parse(h));
                }
            }
        }

        if (!drop.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " drop events: " + new JSONArray(drop));

        wear = new ArrayList<>();
        if (section.contains("wear")) {
            if (section.isString("wear")) {
                wear.add(parse(section.getString("wear", null)));
            } else {
                for (String h : section.getStringList("wear")) {
                    wear.add(parse(h));
                }
            }
        }

        if (!wear.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " wear events: " + new JSONArray(wear));

        sneak = new ArrayList<>();
        if (section.contains("sneak")) {
            if (section.isString("sneak")) {
                sneak.add(parse(section.getString("sneak", null)));
            } else {
                for (String h : section.getStringList("sneak")) {
                    wear.add(parse(h));
                }
            }
        }
        if (!sneak.isEmpty())
            plugin.getLogger().info("Loading " + item.getInternalName() + " sneak events: " + new JSONArray(sneak));

        tasks = new ConcurrentHashMap<>();
    }

    public void onOff(Player player) {
        commonTrigger(player, off, "off_");

        if (tasks.containsKey(player.getUniqueId())) {
            Map<String, Scheduler.Task> tasks = this.tasks.get(player.getUniqueId());
            tasks.entrySet().removeIf(entry -> entry.getKey().startsWith("hold_") || entry.getKey().startsWith("wear_"));
            if (tasks.isEmpty()) {
                this.tasks.remove(player.getUniqueId());
            }
        }
    }

    public void onHold(Player player) {
        commonTrigger(player, hold, "hold_");
    }

    public void onLeftClick(Player player, Block lookingAt, BlockFace face) {
        commonTrigger(player, leftClick, lookingAt, face, "leftclick_");
    }

    public void onRightClick(Player player, Block lookingAt, BlockFace face) {
        commonTrigger(player, rightClick, lookingAt, face, "rightclick_");
    }

    public void onWear(Player player) {
        commonTrigger(player, wear, "wear_", true);
    }

    public void onDrop(Player player) {
        commonTrigger(player, drop, "drop_");
    }

    public void onSneak(Player player) {
        commonTrigger(player, sneak, "sneak_");
    }

    private void commonTrigger(Player player, Collection<Map<String, String>> trigger, String key) {
        commonTrigger(player, trigger, key, false);
    }

    private void commonTrigger(Player player, Collection<Map<String, String>> trigger, String key, boolean wear) {
        commonTrigger(player, trigger, null, null, key, wear);
    }

    private void commonTrigger(Player player, Collection<Map<String, String>> trigger, Block lookingAt, BlockFace face, String key) {
        commonTrigger(player, trigger, lookingAt, face, key, false);
    }

    private void commonTrigger(Player player, Collection<Map<String, String>> trigger, Block lookingAt, BlockFace face, String key, boolean wear) {
        for (Map<String, String> abilities : trigger) {
            long now = System.currentTimeMillis();
            String time = "_" + now;
            if (abilities.containsKey("shift")) {
                if (!player.isSneaking())
                    continue;
            }

            int ticks;
            if (abilities.containsKey("ticks")) {
                ticks = Integer.parseInt(abilities.get("ticks"));
            } else {
                ticks = -1;
            }

            Location targetLocation;
            if (abilities.containsKey("targetblock")) {
                int distance;
                try {
                    distance = Integer.parseInt(abilities.get("targetblock"));
                } catch (Exception e) {
                    distance = 50;
                }
                Block block = player.getTargetBlockExact(distance);
                if (block != null) {
                    targetLocation = block.getLocation();
                } else {
                    targetLocation = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(distance)).getBlock().getLocation();
                }
            } else {
                targetLocation = player.getLocation();
            }

            Collection<Player> affected = new HashSet<>();
            if (abilities.containsKey("radius")) {
                int radius = Integer.parseInt(abilities.get("radius"));
                for (Entity entity : player.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
                    if (entity instanceof Player) {
                        affected.add((Player) entity);
                    }
                }
            } else {
                affected.add(player);
            }

            if (abilities.containsKey("notself") && affected.size() > 1) {
                affected.remove(player);
            }

            int delay;
            if (abilities.containsKey("delay")) {
                delay = Integer.parseInt(abilities.get("delay"));
            } else {
                 delay = 0;
            }

            long millisCd;
            if (abilities.containsKey("cooldown")) {
                millisCd = Long.parseLong(abilities.get("cooldown"));
            } else {
                millisCd = -1L;
            }

            double mana;
            if (abilities.containsKey("mana")) {
                mana = Double.parseDouble(abilities.get("mana"));
            } else {
                mana = -1;
            }

            if (abilities.containsKey("effect")) {
                if (cooldown(key + "effect", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String entry = abilities.get("effect");
                String[] args = entry.split(":"); // effect_name:duration:power
                PotionEffectType type = PotionEffectType.getByName(args[0].toUpperCase());
                if (type != null) {
                    boolean inf = args[1].equalsIgnoreCase("inf");
                    int duration;
                    if (!inf) {
                        duration = Integer.parseInt(args[1]);
                    } else {
                        duration = 1;
                    }
                    int power;
                    if (args.length == 3) {
                        power = Integer.parseInt(args[2]);
                    } else {
                        power = 0;
                    }
                    onTrigger(key + "effect" + time, affected, ticks, delay, (p) -> p.addPotionEffect(type.createEffect(inf ? -1 : duration, power).withAmbient(true).withParticles(false)), wear);
                } else {
                    plugin.getLogger().warning("Unable to parse potion effect: " + args[0]);
                }
            }

            if (abilities.containsKey("cleareffect")) {
                if (cooldown(key + "cleareffect", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String effect = abilities.get("cleareffect");
                if (effect != null) {
                    PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
                    if (type != null) {
                        onTrigger(key + "cleareffect" + time, affected, ticks, delay, (p) -> p.removePotionEffect(type));
                    } else {
                        plugin.getLogger().warning("Unable to parse potion effect: " + effect);
                    }
                } else {
                    onTrigger(key + "cleareffect" + time, affected, ticks, delay, LivingEntity::clearActivePotionEffects);
                }
            }

            if (abilities.containsKey("sound")) {
                if (cooldown(key + "sound", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String entry = abilities.get("sound");
                String[] args = entry.split(":"); //sound_name:volume:pitch
                try {
                    Sound sound = Sound.valueOf(args[0].toUpperCase());
                    float pitch;
                    float volume;
                    if (args.length >= 2) {
                        volume = Float.parseFloat(args[1]);
                        if (args.length == 3) {
                            pitch = Float.parseFloat(args[2]);
                        } else {
                            pitch = 1f;
                        }
                    } else {
                        pitch = 1f;
                        volume = 1f;
                    }

                    onTrigger(key + "sound" + time, affected, ticks, delay, (p) -> p.playSound(p.getLocation(), sound, volume, pitch), wear);
                } catch (Exception e) {
                    plugin.getLogger().warning("Unable to parse sound: " + args[0]);
                }
            }

            if (abilities.containsKey("money")) {
                if (cooldown(key + "money", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                float money = Float.parseFloat(abilities.get("money"));
                EconomyHook economy = plugin.getModule(EconomyHook.class);
                if (economy != null) {
                    onTrigger(key + "money" + time, affected, ticks, delay, (p) -> economy.deposit(p, money), wear);
                }
            }

            if (abilities.containsKey("light_fire") && (trigger == rightClick || trigger == leftClick)) {
                if (cooldown(key + "light_fire", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                AtomicInteger age = new AtomicInteger(100);
                if (!abilities.get("light_fire").equals("null")) {
                    try {
                        age.set(Integer.parseInt(abilities.get("light_fire")));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Unknown fire age = " + abilities.get("light_fire") + ", defaulting to 100 ticks!");
                    }
                }
                onTrigger(key + "light_fire" + time, player, ticks, delay, (p) -> {
                    if (lookingAt == null)
                        return;

                    Block target = lookingAt.getRelative(face);
                    if (target.isEmpty()) {
                        Fire fire = (Fire) plugin.getServer().createBlockData(Material.FIRE);
                        fire.setAge(age.get());
                        target.setBlockData(fire);
                    }
                });
            }

            if (abilities.containsKey("particle")) {
                String entry = abilities.get("particle");
                String[] args = entry.split(":"); //particle_type:location(world|x|y|z):count
                try {
                    if (cooldown(key + "particle", player, now, millisCd)) {
                        continue;
                    }
                    if (mana(player, mana)) {
                        continue;
                    }
                    Particle particle = Particle.valueOf(args[0].toUpperCase());
                    Location location;
                    int amount;
                    if (args.length >= 2) {
                        location = null;
                        String locationEntry = args[1];
                        String[] locationArgs = locationEntry.split("\\|"); // world|x|y|z || x|y|z
                        World world;
                        float x, y, z;
                        if (locationArgs.length == 4) {
                            world = locationArgs[0].equalsIgnoreCase("{player-world}") ? player.getWorld() : plugin.getServer().getWorld(locationArgs[0]);
                            x = Float.parseFloat(locationArgs[1]);
                            y = Float.parseFloat(locationArgs[2]);
                            z = Float.parseFloat(locationArgs[3]);
                            location = new Location(world, x, y, z);
                        } else if (locationArgs.length == 3) {
                            world = player.getWorld();
                            x = Float.parseFloat(locationArgs[0]);
                            y = Float.parseFloat(locationArgs[1]);
                            z = Float.parseFloat(locationArgs[2]);
                            location = new Location(world, player.getX() + x, player.getY() + y, player.getZ() + z);
                        }
                        if (args.length == 3) {
                            amount = Integer.parseInt(args[2]);
                        } else {
                            amount = 1;
                        }
                    } else {
                        amount = 1;
                        location = targetLocation;
                    }

                    Location finalLocation = location;
                    onTrigger(key + "particle" + time, affected, ticks, delay, (p) -> p.spawnParticle(particle, finalLocation, amount), wear);
                } catch (Exception e) {
                    plugin.getLogger().warning("Unable to parse particle: " + args[0]);
                }
            }

            if (abilities.containsKey("shoot_particle")) {
                String entry = abilities.get("shoot_particle");
                String[] args = entry.split(":"); //particle_type:count:interval:gravity
                try {
                    if (cooldown(key + "shoot_particle", player, now, millisCd)) {
                        continue;
                    }
                    if (mana(player, mana)) {
                        continue;
                    }
                    Particle particle = Particle.valueOf(args[0].toUpperCase());
                    int count = Integer.parseInt(args[1]);
                    float interval = Float.parseFloat(args[2]);
                    float gravity;
                    if (args.length == 4) {
                        gravity = Float.parseFloat(args[3]);
                    } else {
                        gravity = 0;
                    }
                    Vector gravityVector = new Vector(0, gravity, 0);
                    Vector direction = player.getLocation().getDirection().normalize().multiply(interval);
                    Location point = player.getEyeLocation();
                    onTrigger(key + "shoot_particle" + time, affected, ticks, delay, (p) -> {
                        point.add(direction.add(gravityVector));
                        point.getWorld().spawnParticle(particle, point, count);
                        Location previous = point.clone().subtract(direction);

                        if (abilities.containsKey("inflict")) {
                            float amount = Float.parseFloat(abilities.get("inflict"));
                            RayTraceResult ray = point.getWorld().rayTrace(previous, direction, direction.length(), FluidCollisionMode.ALWAYS, true, 0.5f, e -> e instanceof LivingEntity);
                            if (ray != null && ray.getHitEntity() != null) {
                                ((LivingEntity) ray.getHitEntity()).damage(amount, p);
                            }
                        }

                        if (point.getBlock().isSolid()) {
                            Map<String, Scheduler.Task> map = tasks.get(p.getUniqueId());
                            if (map != null && map.containsKey(key + "shoot_particle" + time)) {
                                map.get(key + "shoot_particle" + time).cancel();
                                map.remove(key + "shoot_particle");
                            }
                        }
                    }, wear);
                } catch (Exception e) {
                    plugin.getLogger().warning("Unable to parse particle: " + args[0]);
                }
            }

            if (abilities.containsKey("remove")) {
                String remove = abilities.get("remove");
                boolean all = remove.equalsIgnoreCase("null");
                if (cooldown(key + "remove", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                if (all) {
                    onTrigger(key + "remove" + time, player, ticks, delay, (p) -> {
                        if (wear) {
                            ItemStack[] armors = p.getInventory().getArmorContents();
                            boolean dirty = false;
                            for (int i = 0; i < armors.length; i++) {
                                ItemStack armor = armors[i];
                                if (armor == null)
                                    continue;

                                if (isSimilar(armor)) {
                                    armors[i] = null;
                                    dirty = true;
                                }
                            }
                            if (dirty) {
                                p.getInventory().setArmorContents(armors);
                            }
                            return;
                        }
                        p.getInventory().removeItemAnySlot(baseItem);
                    });
                } else {
                    int amount = Integer.parseInt(remove);
                    onTrigger(key + "remove" + time, player, ticks, delay, (p) -> {
                        if (wear) {
                            ItemStack[] armors = p.getInventory().getArmorContents();
                            boolean dirty = false;
                            for (int i = 0; i < armors.length; i++) {
                                ItemStack armor = armors[i];
                                if (armor == null)
                                    continue;

                                if (isSimilar(armor)) {
                                    armor.setAmount(armor.getAmount() - amount);
                                    dirty = true;
                                }
                            }
                            if (dirty) {
                                p.getInventory().setArmorContents(armors);
                            }
                            return;
                        }

                        if (isSimilar(p.getInventory().getItemInMainHand())) {
                            p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - amount);

                        } else if (isSimilar(p.getInventory().getItemInOffHand())) {
                            p.getInventory().getItemInOffHand().setAmount(p.getInventory().getItemInOffHand().getAmount() - amount);
                        }
                    });
                }
            }

            if (abilities.containsKey("playercmd")) {
                if (cooldown(key + "playercmd", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String cmd = abilities.get("playercmd");
                onTrigger(key + "playercmd" + time, affected, ticks, delay, (p) -> p.performCommand(cmd.replace("{player}", p.getName())), wear);
            }

            if (abilities.containsKey("servercmd")) {
                if (cooldown(key + "servercmd", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String cmd = abilities.get("servercmd");
                onTrigger(key + "servercmd" + time, player, ticks, delay, (p) -> plugin.dispatchCommand(cmd.replace("{player}", p.getName())), wear);
            }

            if (abilities.containsKey("message")) {
                if (cooldown(key + "message", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String msg = abilities.get("message");
                onTrigger(key + "message" + time, affected, ticks, delay, (p) -> p.sendMessage(color(msg)));
            }

            if (abilities.containsKey("json")) {
                if (cooldown(key + "json", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String msg = abilities.get("json");
                onTrigger(key + "json" + time, affected, ticks, delay, (p) -> p.sendMessage(GsonComponentSerializer.gson().deserialize(msg)));
            }

            if (abilities.containsKey("setfire")) {
                if (cooldown(key + "setfire", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                onTrigger(key + "setfire" + time, affected, ticks, delay, (p) -> p.setFireTicks(ticks != -1 ? ticks : 50), wear);
            } else if (abilities.containsKey("extinguish")) {
                if (cooldown(key + "extinguish", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                onTrigger(key + "extinguish" + time, affected, ticks, delay, (p) -> p.setFireTicks(0), wear);
            }

            if (abilities.containsKey("summon")) {
                String entityType = abilities.get("summon");
                if (entityType.startsWith("mythicmobs:")) {
                    String name = entityType.substring("mythicmobs:".length());
                    MythicHook mm = plugin.getModule(MythicHook.class);
                    assert mm != null;
                    MythicMob mob = mm.getMythic().getMobManager().getMythicMob(name).orElse(null);
                    if (mob == null) {
                        plugin.getLogger().warning("Unable to summon MythicMob " + name + ". Doesn't exist!");
                    } else {
                        if (cooldown(key + "summon", player, now, millisCd)) {
                            continue;
                        }
                        if (mana(player, mana)) {
                            continue;
                        }
                        onTrigger(key + "summon" + time, player, ticks, delay, (p) -> {
                            mob.spawn(BukkitAdapter.adapt(p.getLocation()), 1);
                        }, wear);
                    }

                } else {
                    EntityType type = EntityType.valueOf(entityType.toUpperCase());
                    if (cooldown(key + "summon", player, now, millisCd)) {
                        continue;
                    }
                    if (mana(player, mana)) {
                        continue;
                    }
                    onTrigger(key + "summon" + time, player, ticks, delay, (p) -> {
                        p.getWorld().spawnEntity(targetLocation, type, CreatureSpawnEvent.SpawnReason.CUSTOM, (e) -> {});
                    }, wear);
                }
            }

            if (abilities.containsKey("damage")) {
                double damage = Double.parseDouble(abilities.get("damage"));
                if (cooldown(key + "damage", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                onTrigger(key + "damage" + time, affected, ticks, delay, (p) -> p.damage(damage), wear);
            }

            if (abilities.containsKey("heal")) {
                double heal = Double.parseDouble(abilities.get("heal"));
                if (cooldown(key + "heal", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                onTrigger(key + "heal" + time, affected, ticks, delay, (p) -> p.setHealth(p.getHealth() + heal), wear);
            }

            if (abilities.containsKey("lightning")) {
                String target = abilities.get("lightning");
                if (cooldown(key + "lightning", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                onTrigger(key + "lightning" + time, affected, ticks, delay, (p) -> {
                    LightningStrike strike;
                    if (target.equalsIgnoreCase("targetblock")) {
                        int distance;
                        try {
                            distance = Integer.parseInt(abilities.get("targetblock"));
                        } catch (Exception e) {
                            distance = 50;
                        }
                        Block targetBlock = p.getTargetBlockExact(distance);
                        if (targetBlock == null) {
                            targetBlock = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(distance)).getBlock();
                        }
                        strike = p.getWorld().strikeLightning(targetBlock.getLocation());
                    } else {
                        strike = p.getWorld().strikeLightning(p.getLocation());
                    }

                    strike.setCausingPlayer(p);
                    strike.setFlashCount(3);
                    strike.setLifeTicks(1);

                }, wear);
            }

            if (abilities.containsKey("lightningeffect")) {
                String target = abilities.get("lightningeffect");
                if (cooldown(key + "lightningeffect", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                onTrigger(key + "lightningeffect" + time, affected, ticks, delay, (p) -> {
                    LightningStrike strike;
                    if (target.equalsIgnoreCase("targetblock")) {
                        int distance;
                        try {
                            distance = Integer.parseInt(abilities.get("targetblock"));
                        } catch (Exception e) {
                            distance = 50;
                        }
                        Block targetBlock = p.getTargetBlockExact(distance);
                        if (targetBlock == null) {
                            targetBlock = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(distance)).getBlock();
                        }
                        strike = p.getWorld().strikeLightning(targetBlock.getLocation());
                    } else {
                        strike = p.getWorld().strikeLightning(p.getLocation());
                    }
                    strike.setCausingPlayer(p);
                    strike.setFlashCount(3);
                    strike.setLifeTicks(1);

                }, wear);
            }

            if (abilities.containsKey("projectile")) { //type:velocity:offset:damage
                String entry = abilities.get("projectile");
                String[] args = entry.split(":");
                try {
                    EntityType type = EntityType.valueOf(args[0].toUpperCase());
                    Class<? extends Entity> clazz = type.getEntityClass();
                    if (clazz == null) {
                        plugin.getLogger().warning("EntityType cannot be unknown!");
                        continue;
                    }
                    if (!clazz.isAssignableFrom(Projectile.class)) {
                        plugin.getLogger().warning("EntityType must be a projectile! (e.g. arrow, snowball, fireball");
                        continue;
                    }
                    if (cooldown(key + "projectile", player, now, millisCd)) {
                        continue;
                    }
                    if (mana(player, mana)) {
                        continue;
                    }

                    float speed = Float.parseFloat(args[1]);
                    float offset = Float.parseFloat(args[2]);
                    float damage;
                    if (args.length == 4) {
                        damage = Float.parseFloat(args[3]);
                    } else {
                        damage = 1f;
                    }
                    onTrigger(key + "projectile" + time, affected, ticks, delay, (p) -> {
                        Vector direction = player.getLocation().getDirection().normalize().multiply(offset);
                        Location point = player.getEyeLocation().add(direction);
                        p.getWorld().spawn(point, type.getEntityClass(), CreatureSpawnEvent.SpawnReason.CUSTOM, (e) -> {
                            e.setVelocity(direction.normalize().multiply(speed));
                            e.setMetadata("sgcraftlink-mmitem", new FixedMetadataValue(plugin, damage));
                            ((Projectile)e).setShooter(p);
                            ((Projectile)e).setHasBeenShot(false);
                            ((Projectile)e).setHasLeftShooter(false);
                        });
                    }, wear);
                } catch (Exception e) {
                    plugin.getLogger().warning("Unable to parse EntityType " + args[0] + "!");
                }
            }

            if (abilities.containsKey("dash")) { // speed:yoffset
                if (cooldown(key + "dash", player, now, millisCd)) {
                    continue;
                }
                if (mana(player, mana)) {
                    continue;
                }
                String entry = abilities.get("dash");
                String[] args = entry.split(":");
                float speed = Float.parseFloat(args[0]);
                float yOffset;
                if (args.length == 2) {
                    yOffset = Float.parseFloat(args[1]);
                } else {
                    yOffset = 0;
                }
                onTrigger(key + "dash" + time, player, ticks, delay, (p) -> {
                    Location start = p.getLocation().add(0, 0.5, 0);
                    Vector direction = p.getLocation().getDirection().setY(yOffset).normalize().multiply(speed);
                    p.setVelocity(p.getVelocity().add(direction));
                    if (abilities.containsKey("inflict")) {
                        float amount = Float.parseFloat(abilities.get("inflict"));
                        RayTraceResult ray = p.getWorld().rayTrace(start, p.getVelocity(), (int) p.getVelocity().length() * 2, FluidCollisionMode.ALWAYS, true, 1, e -> e instanceof LivingEntity);
                        if (ray != null && ray.getHitEntity() != null) {
                            LivingEntity entity = (LivingEntity) ray.getHitEntity();
                            entity.damage(amount);
                        }
                    }
                }, wear);
            }
        }
    }

    private boolean mana(Player player, double mana) {
        if (mana <= 0)
            return false;

        AuraSkillsHook hook = plugin.getModule(AuraSkillsHook.class);
        if (hook == null)
            return false;

        double current = hook.getMana(player);
        if (current < mana)
            return true;

        hook.setMana(player, current - mana);
        return false;
    }

    private boolean cooldown(String key, Player player, long now, long millisCd) {
        if (millisCd != -1) {
            Map<String, Scheduler.Task> map = tasks.get(player.getUniqueId());
            if (map != null) {
                long latest = -1;
                for (String k : map.keySet()) {
                    if (k.startsWith(key)) {
                        long previous = Long.parseLong(k.substring((key).length() + 1));
                        if (latest < previous) {
                            latest = previous;
                        }
                    }
                }
                if ((now - latest) <= millisCd) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onTrigger(String key, Player player, int ticks, int delay, Consumer<Player> runnable) {
        onTrigger(key, Collections.singletonList(player), ticks, delay, runnable, false);
    }

    private void onTrigger(String key, Player player, int ticks, int delay, Consumer<Player> runnable, final boolean wear) {
        onTrigger(key, Collections.singletonList(player), ticks, delay, runnable, wear);
    }

    private void onTrigger(String key, Collection<Player> players, int ticks, int delay, Consumer<Player> runnable) {
        onTrigger(key, players, ticks, delay, runnable, false);
    }

    private void onTrigger(String key, Collection<Player> players, int ticks, int delay, Consumer<Player> runnable, final boolean wear) {
        for (Player player : players) {
            if (ticks != -1) {
                Map<String, Scheduler.Task> taskMap = tasks.get(player.getUniqueId());
                if (taskMap != null && taskMap.containsKey(key))
                    continue;

                if (taskMap == null) {
                    taskMap = new ExpiryConcurrentHashMap<>(60000);
                    tasks.put(player.getUniqueId(), taskMap);
                }

                Map<String, Scheduler.Task> finalTaskMap = taskMap;
                Scheduler.Task holdTask = new Scheduler.TaskRunnable() {
                    @Override
                    public void run() {
                        if (wear) {
                            ItemStack[] armors = player.getInventory().getArmorContents();
                            for (int i = 0; i < armors.length; i++) {
                                ItemStack armor = armors[i];
                                if (armor == null)
                                    continue;

                                if (isSimilar(armor)) {
                                    runnable.accept(player);
                                }
                            }
                            return;
                        }
                        if (isSimilar(player.getInventory().getItemInMainHand()) || isSimilar(player.getInventory().getItemInOffHand())) {
                            runnable.accept(player);
                        }
                    }

                    @Override
                    public synchronized void cancel() throws IllegalStateException {
                        finalTaskMap.remove(key);
                        super.cancel();
                    }
                }.runTaskTimer(plugin.getScheduler(), player, delay, ticks);
                taskMap.put(key, holdTask);
            } else if (delay != 0) {
                Map<String, Scheduler.Task> taskMap = tasks.get(player.getUniqueId());
                if (taskMap != null && taskMap.containsKey(key))
                    continue;

                if (taskMap == null) {
                    taskMap = new ExpiryConcurrentHashMap<>(60000);
                    tasks.put(player.getUniqueId(), taskMap);
                }

                Map<String, Scheduler.Task> finalTaskMap = taskMap;
                Scheduler.Task holdTask = new Scheduler.TaskRunnable() {
                    @Override
                    public void run() {
                        if (wear) {
                            ItemStack[] armors = player.getInventory().getArmorContents();
                            for (int i = 0; i < armors.length; i++) {
                                ItemStack armor = armors[i];
                                if (armor == null)
                                    continue;

                                if (isSimilar(armor)) {
                                    runnable.accept(player);
                                }
                            }
                            return;
                        }
                        if (isSimilar(player.getInventory().getItemInMainHand()) || isSimilar(player.getInventory().getItemInOffHand())) {
                            runnable.accept(player);
                        }
                    }

                    @Override
                    public synchronized void cancel() throws IllegalStateException {
                        finalTaskMap.remove(key);
                        super.cancel();
                    }
                }.runTaskLater(plugin.getScheduler(), player, delay);
                taskMap.put(key, holdTask);
            } else {
                Map<String, Scheduler.Task> taskMap = tasks.get(player.getUniqueId());
                if (taskMap != null && taskMap.containsKey(key))
                    continue;

                if (taskMap == null) {
                    taskMap = new ExpiryConcurrentHashMap<>(60000);
                    tasks.put(player.getUniqueId(), taskMap);
                }

                runnable.accept(player);

                taskMap.remove(key);
            }
        }
    }

    private Map<String, String> parse(String string) {
        Map<String, String> map = new HashMap<>();
        if (string != null) {
            String[] args = string.split(",");
            for (String s : args) {
                String[] entry = s.split("=");
                if (entry.length == 2) {
                    map.put(entry[0], entry[1]);
                } else {
                    map.put(entry[0], "null");
                }
            }
        }
        return map;
    }

    public void clear() {
        for (Map<String, Scheduler.Task> tasks : tasks.values()) {
            for (Scheduler.Task t : tasks.values()) {
                t.cancel();
            }
            tasks.clear();
        }
        this.tasks.clear();
    }

    public boolean isSimilar(ItemStack item) {
        CompoundTag tagComp = MythicBukkit.inst().getVolatileCodeHandler().getItemHandler().getNBTData(item);
        if (tagComp.containsKey("MYTHIC_TYPE")) {
            String mythicType = mmItem.getInternalName();
            return tagComp.getString("MYTHIC_TYPE").equals(mythicType);
        }
        return baseItem.isSimilar(item);
    }
}
