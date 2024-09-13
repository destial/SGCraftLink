package xyz.destiall.sgcraftlink.modules.vanilla;

import com.Zrips.CMI.Modules.Economy.Economy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.CasinoSlot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;
import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.testPaperOr1_8Above;

public class Casino extends Module implements Listener, CommandExecutor, TabExecutor {
    private Map<Material, Double> rewards;
    private List<CasinoSlot> slots;
    private Material currency = Material.DIAMOND;
    private boolean vault = false;
    private String currencyName;
    private boolean dirty = false;
    private int minimum = 5;

    public Casino(SGCraftLinkSpigot plugin) {
        super(plugin, "casino.yml");
        setCommandExecutor("casino");
    }

    public boolean deleteSlot(int id) {
        if (slots.removeIf(slot -> slot.getId() == id)) {
            dirty = true;
            config.set("slots." + id, null);
            return true;
        }
        return false;
    }

    public CasinoSlot createSlot(Block center, BlockFace left, BlockFace right) {
        CasinoSlot slot = new CasinoSlot(this, center, left, right);
        slots.add(slot);
        dirty = true;

        return slot;
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
        config.set("vault.enabled", false);
        config.set("vault.currency", "credits");
        config.set("currency", (currency = Material.DIAMOND).name());
        config.set("minimum", minimum);

        Configuration rewards = new YamlConfiguration();
        rewards.set(Material.DIAMOND_BLOCK.name(), 10);
        rewards.set(Material.EMERALD_BLOCK.name(), 5);
        rewards.set(Material.GOLD_BLOCK.name(), 3);
        rewards.set(Material.IRON_BLOCK.name(), 2);

        try {
            testPaperOr1_8Above();
            rewards.set(Material.COPPER_BLOCK.name(), 1.5);
        } catch (Throwable e) {}

        config.set("block-rewards-multiplier", rewards);
    }

    public Map<Material, Double> getRewards() {
        return rewards;
    }

    public List<CasinoSlot> getSlots() {
        return slots;
    }

    public Material getCurrency() {
        return currency;
    }

    public String getCurrencyName() {
        if (currencyName == null) {
            if (vault) {
                currencyName = config.getString("vault.currency", "credits");
                return currencyName;
            }

            currencyName = (currency.name().charAt(0) + currency.name().toLowerCase().substring(1)).replace("_", " ");
        }
        return currencyName;
    }

    @Override
    public void disable() {
        unsetCommandExecutor("casino");

        if (!dirty)
            return;

        ConfigurationSection slotsSection = config.getConfigurationSection("slots");
        if (slotsSection == null) {
            slotsSection = config.createSection("slots");
        }
        for (CasinoSlot slot : slots) {
            slotsSection.set(String.valueOf(slot.getId()), slot.toConfig());
        }

        saveConfig();
    }

    @Override
    public void enable() {
        vault = config.getBoolean("vault.enabled", false);
        currency = Material.getMaterial(config.getString("currency", Material.DIAMOND.name()));
        minimum = config.getInt("minimum", minimum);
        rewards = new HashMap<>();

        ConfigurationSection rewardsSection = config.getConfigurationSection("block-rewards-multiplier");

        if (rewardsSection != null) {
            Set<String> keys = rewardsSection.getKeys(false);
            for (String key : keys) {
                Material material = Material.getMaterial(key);
                double amount = rewardsSection.getDouble(key);

                rewards.put(material, amount);
            }
        }

        slots = new ArrayList<>();
        ConfigurationSection slotsSection = config.getConfigurationSection("slots");
        if (slotsSection != null) {
            Set<String> keys = slotsSection.getKeys(false);
            for (String key : keys) {
                int id = Integer.parseInt(key);
                ConfigurationSection section = slotsSection.getConfigurationSection(key);

                CasinoSlot slot = new CasinoSlot(this, id, section);
                slots.add(slot);
            }
        }

        plugin.getLogger().info("Registered " + slots.size() + " casino slots " + new JSONArray(slots.stream().map(CasinoSlot::getId).collect(Collectors.toList())).toString().replace("\"", ""));

    }

    public boolean isVault() {
        return vault;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&cUsage:"));
            sender.sendMessage(color("&c - /casino create [look-at-center-block]"));
            sender.sendMessage(color("&c - /casino delete [slot-id]"));
            sender.sendMessage(color("&c - /casino start [slot-id] [credits] <player>"));
            return false;
        }

        String type = args[0].toLowerCase();
        switch (type) {
            case "create": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color("&cYou must be a player to use this command!"));
                    return false;
                }

                Player player = (Player) sender;
                Block center = player.getTargetBlockExact(5);
                if (center == null) {
                    player.sendMessage(color("&cYou need to look at the center block of the slot!"));
                    return false;
                }

                Vector dir = player.getLocation().getDirection();
                dir = dir.setY(0).normalize();
                Block left, right;

                if (Math.abs(dir.getX()) < Math.abs(dir.getZ())) {
                    if (dir.getZ() > 0) {
                        left = center.getRelative(1, 0, 0);
                        right = center.getRelative(-1, 0, 0);
                    } else {
                        left = center.getRelative(-1, 0, 0);
                        right = center.getRelative(1, 0, 0);
                    }
                } else if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
                    if (dir.getX() < 0) {
                        left = center.getRelative(0, 0, 1);
                        right = center.getRelative(0, 0, -1);
                    } else {
                        left = center.getRelative(0, 0, -1);
                        right = center.getRelative(0, 0, 1);
                    }
                } else {
                    player.sendMessage(color("&cYou need to face the direction of the left and right blocks!"));
                    return false;
                }

                CasinoSlot slot = createSlot(center, center.getFace(left), center.getFace(right));
                player.sendMessage(color("&aCreated slot with id " + slot.getId()));

                Material lowestValue = null;
                double lowest = 0;
                for (Map.Entry<Material, Double> entry : rewards.entrySet()) {
                    if (lowestValue == null) {
                        lowestValue = entry.getKey();
                        lowest = entry.getValue();
                        continue;
                    }

                    if (entry.getValue() < lowest) {
                        lowestValue = entry.getKey();
                        lowest = entry.getValue();
                    }
                }

                if (lowestValue != null) {
                    left.setType(lowestValue);
                    center.setType(lowestValue);
                    right.setType(lowestValue);
                }
                break;
            }
            case "delete": {
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /casino delete [slot-id]"));
                    return false;
                }

                int id = -1;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    sender.sendMessage(color("&cSlot id must be a number!"));
                }

                if (deleteSlot(id)) {
                    sender.sendMessage(color("&aDeleted casino slot of id " + id));
                    return false;
                }

                sender.sendMessage(color("&cCasino slot id " + id + " not found!"));
                break;
            }
            case "start": {
                if (args.length < 3) {
                    sender.sendMessage(color("&cUsage: /casino start [slot-id] [credits] <player>"));
                    return false;
                }

                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    sender.sendMessage(color("&cSlot id must be a number!"));
                    return false;
                }

                int finalId = id;
                CasinoSlot slot = slots.stream().filter(s -> s.getId() == finalId).findFirst().orElse(null);

                if (slot == null) {
                    sender.sendMessage(color("&cCasino slot id " + id + " not found!"));
                    return false;
                }

                if (slot.inUse()) {
                    sender.sendMessage(color("&cCasino slot id " + slot.getId() + " is in use!"));
                    return false;
                }

                int credits;
                try {
                    credits = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    sender.sendMessage(color("&c" + getCurrencyName() + " must be a number!"));
                    return false;
                }

                if (vault) {
                    if (credits <= minimum) {
                        sender.sendMessage(color("&cMinimum " + getCurrencyName() + " bet is " + minimum + "!"));
                        return false;
                    }
                }

                Player user = sender instanceof Player ? ((Player) sender) : null;
                if (sender.hasPermission("sgc.casino.other") && args.length > 3) {
                    user = plugin.getServer().getPlayer(args[3]);
                }

                if (user == null) {
                    sender.sendMessage(color("&cPlayer " + args[3] + " not found!"));
                    return false;
                }

                if (vault) {
                    if (!Economy.has(user, credits)) {
                        user.sendMessage(color("&cYou do not have " + credits + " " + getCurrencyName() + "!"));
                        return false;
                    }
                    Economy.withdrawPlayer(user, credits);
                } else {
                    CasinoBank bank = new CasinoBank(this, user);
                    if (!bank.has(credits)) {
                        user.sendMessage(color("&cYou do not have " + credits + " " + getCurrencyName() + "!"));
                        return false;
                    }
                    bank.withdraw(credits);
                }


                slot.roll(credits, user);
                break;
            }

            default: {
                sender.sendMessage("&cUsage: /casino");
                break;
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Stream.of("create", "delete", "start").filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList())
                : args.length == 2 ?
                    (args[0].equals("delete") || args[0].equals("start") ? slots.stream().map(s -> String.valueOf(s.getId())).filter(i -> i.startsWith(args[1])).collect(Collectors.toList()) : null) : null;
    }

    public static class CasinoBank {
        protected final Casino casino;
        protected final Player player;

        public CasinoBank(Casino casino, Player player) {
            this.casino = casino;
            this.player = player;
        }

        public int getBalance() {
            int total = 0;
            if (!player.isOnline())
                return total;

            PlayerInventory inventory = player.getInventory();

            for (ItemStack item : inventory) {
                if (item == null || item.getType() != casino.getCurrency())
                    continue;

                total += item.getAmount();
            }
            return total;
        }

        public boolean has(int amount) {
            return getBalance() >= amount;
        }

        public boolean withdraw(int amount) {
            if (!has(amount))
                return false;

            int left = amount;
            for (ItemStack item : player.getInventory()) {
                if (item == null || item.getType() != casino.getCurrency())
                    continue;

                while (left > 0 && item.getAmount() > 0) {
                    item.setAmount(item.getAmount() - 1);
                    left--;
                }
            }
            return true;
        }

        public void deposit(int amount) {
            ItemStack money = new ItemStack(casino.getCurrency());
            money.setAmount(amount);

            Map<Integer, ItemStack> drops = player.getInventory().addItem(money);
            for (ItemStack drop : drops.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }
    }
}
