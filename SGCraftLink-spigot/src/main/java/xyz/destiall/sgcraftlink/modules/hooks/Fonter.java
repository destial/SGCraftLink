package xyz.destiall.sgcraftlink.modules.hooks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.hooks.utils.DefaultFont;
import xyz.destiall.sgcraftlink.modules.hooks.utils.Font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

// Hi-jack ProtocolLib packets to translate all chat and display packets
public class Fonter extends Module implements CommandExecutor, TabExecutor {
    private final List<Font> fonts = new ArrayList<>();
    private final Map<UUID, Font> users = new HashMap<>();

    private boolean dirty = false;

    public Fonter(SGCraftLinkSpigot plugin) {
        super(plugin, "fonts.yml");
        plugin.getCommand("font").setExecutor(this);
        ConfigurationSection fonts = config.getConfigurationSection("fonts");
        this.fonts.add(new DefaultFont());
        if (fonts != null) {
            for (String name : fonts.getKeys(false)) {
                try {
                    Font font = new Font(name, Objects.requireNonNull(fonts.getConfigurationSection(name)));
                    this.fonts.add(font);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ConfigurationSection saved = config.getConfigurationSection("users");
        if (saved != null) {
            for (String uuid : saved.getKeys(false)) {
                Font font = getFontFromName(saved.getString(uuid));
                if (font == null)
                    continue;
                users.put(UUID.fromString(uuid), font);
            }
        }
    }

    @Override
    public void loadDefaultConfig() {
        config.set("fonts.sleek.ignore-case", true);
        config.set("fonts.sleek.lower", "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ");

        config.set("fonts.fancy.ignore-case", false);
        config.set("fonts.fancy.two-char-per", true);
        config.set("fonts.fancy.lower", "\uD835\uDC1A\uD835\uDC1B\uD835\uDC1C\uD835\uDC1D\uD835\uDC1E\uD835\uDC1F\uD835\uDC20\uD835\uDC21\uD835\uDC22\uD835\uDC23\uD835\uDC24\uD835\uDC25\uD835\uDC26\uD835\uDC27\uD835\uDC28\uD835\uDC29\uD835\uDC2A\uD835\uDC2B\uD835\uDC2C\uD835\uDC2D\uD835\uDC2E\uD835\uDC2F\uD835\uDC30\uD835\uDC31\uD835\uDC32\uD835\uDC33");
        config.set("fonts.fancy.upper", "\uD835\uDC00\uD835\uDC01\uD835\uDC02\uD835\uDC03\uD835\uDC04\uD835\uDC05\uD835\uDC06\uD835\uDC07\uD835\uDC08\uD835\uDC09\uD835\uDC0A\uD835\uDC0B\uD835\uDC0C\uD835\uDC0D\uD835\uDC0E\uD835\uDC0F\uD835\uDC10\uD835\uDC11\uD835\uDC12\uD835\uDC13\uD835\uDC14\uD835\uDC15\uD835\uDC16\uD835\uDC17\uD835\uDC18\uD835\uDC19");

        config.set("fonts.boxes.ignore-case", true);
        config.set("fonts.boxes.two-char-per", true);
        config.set("fonts.boxes.lower", "\uD83C\uDD30\uD83C\uDD31\uD83C\uDD32\uD83C\uDD33\uD83C\uDD34\uD83C\uDD35\uD83C\uDD36\uD83C\uDD37\uD83C\uDD38\uD83C\uDD39\uD83C\uDD3A\uD83C\uDD3B\uD83C\uDD3C\uD83C\uDD3D\uD83C\uDD3E\uD83C\uDD3F\uD83C\uDD40\uD83C\uDD41\uD83C\uDD42\uD83C\uDD43\uD83C\uDD44\uD83C\uDD45\uD83C\uDD46\uD83C\uDD47\uD83C\uDD48\uD83C\uDD49");

        config.set("fonts.gothic_bold.ignore-case", false);
        config.set("fonts.gothic_bold.two-char-per", true);
        config.set("fonts.gothic_bold.lower", "\uD835\uDD86\uD835\uDD87\uD835\uDD88\uD835\uDD89\uD835\uDD8A\uD835\uDD8B\uD835\uDD8C\uD835\uDD8D\uD835\uDD8E\uD835\uDD8F\uD835\uDD90\uD835\uDD91\uD835\uDD92\uD835\uDD93\uD835\uDD94\uD835\uDD95\uD835\uDD96\uD835\uDD97\uD835\uDD98\uD835\uDD99\uD835\uDD9A\uD835\uDD9B\uD835\uDD9C\uD835\uDD9D\uD835\uDD9E\uD835\uDD9F");
        config.set("fonts.gothic_bold.upper", "\uD835\uDD6C\uD835\uDD6D\uD835\uDD6E\uD835\uDD6F\uD835\uDD70\uD835\uDD71\uD835\uDD72\uD835\uDD73\uD835\uDD74\uD835\uDD75\uD835\uDD76\uD835\uDD77\uD835\uDD78\uD835\uDD79\uD835\uDD7A\uD835\uDD7B\uD835\uDD7C\uD835\uDD7D\uD835\uDD7E\uD835\uDD7F\uD835\uDD80\uD835\uDD81\uD835\uDD82\uD835\uDD83\uD835\uDD84\uD835\uDD85");

        config.set("fonts.gothic.ignore-case", false);
        config.set("fonts.gothic.two-char-per", true);
        config.set("fonts.gothic.lower", "\uD835\uDCB6\uD835\uDCB7\uD835\uDCB8\uD835\uDCB9\uD835\uDC52\uD835\uDCBB\uD835\uDC54\uD835\uDCBD\uD835\uDCBE\uD835\uDCBF\uD835\uDCC0\uD835\uDCC1\uD835\uDCC2\uD835\uDCC3\uD835\uDC5C\uD835\uDCC5\uD835\uDCC6\uD835\uDCC7\uD835\uDCC8\uD835\uDCC9\uD835\uDCCA\uD835\uDCCB\uD835\uDCCC\uD835\uDCCD\uD835\uDCCE\uD835\uDCCF");
        config.set("fonts.gothic.upper", "\uD835\uDD6C\uD835\uDD6D\uD835\uDD6E\uD835\uDD6F\uD835\uDD70\uD835\uDD71\uD835\uDD72\uD835\uDD73\uD835\uDD74\uD835\uDD75\uD835\uDD76\uD835\uDD77\uD835\uDD78\uD835\uDD79\uD835\uDD7A\uD835\uDD7B\uD835\uDD7C\uD835\uDD7D\uD835\uDD7E\uD835\uDD7F\uD835\uDD80\uD835\uDD81\uD835\uDD82\uD835\uDD83\uD835\uDD84\uD835\uDD85");
    }

    public List<Font> getFonts() {
        return fonts;
    }

    public Font getFontFromName(String name) {
        for (Font font : fonts) {
            if (font.getName().equalsIgnoreCase(name))
                return font;
        }
        return null;
    }

    public Font getFontUser(UUID player) {
        return users.getOrDefault(player, fonts.get(0));
    }

    public Map<UUID, Font> getUsers() {
        return users;
    }

    @Override
    public void disable() {
        fonts.clear();

        if (dirty) {
            save();
            dirty = false;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return false;
        }
        Player player = (Player) sender;
        Font font = getFontUser(player.getUniqueId());
        if (args.length == 0) {
            player.sendMessage(color(font.format("&cUsage: /font [font]")));
            player.sendMessage(color(font.format("&cSet the font on your display!")));
            return false;
        }
        Font newFont = getFontFromName(args[0]);
        if (newFont == null) {
            player.sendMessage(color(font.format("&cFont not found!")));
            return false;
        }
        font = newFont;
        users.put(player.getUniqueId(), newFont);
        config.set("users." + player.getUniqueId(), newFont.getName());
        dirty = true;
        player.sendMessage(color(font.format("&cYou have set your font to: " + font.getName())));
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0)
            return Collections.emptyList();
        return getFonts().stream().map(Font::getName).filter(f -> f.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
    }
}
