package xyz.destiall.sgcraftlink.modules.survival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Pair;
import xyz.destiall.sgcraftlink.modules.Module;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class ResourcePackModule extends Module implements CommandExecutor {
    public ResourcePackModule(SGCraftLinkSpigot plugin) {
        super(plugin, "resource-pack.yml");
        setCommandExecutor("rp");
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
        config.set("use-forcepack-url", true);
    }

    @Override
    public void disable() {
        unsetCommandExecutor("rp");
    }

    @Override
    public void enable() {}

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static String toHexString(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        int var4 = data.length;
        for (int var5 = 0; var5 < var4; ++var5) {
            byte b = data[var5];
            r.append(hexCode[b >> 4 & 15]);
            r.append(hexCode[b & 15]);
        }

        return r.toString();
    }

    public static Pair<String, byte[]> getDataFromUrl(String url) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        final URLConnection urlConnection = new URL(url).openConnection();

        final InputStream fis = urlConnection.getInputStream();
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        fis.close();
        final byte[] urlBytes = digest.digest();
        return new Pair<>(toHexString(urlBytes), urlBytes);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (player.hasResourcePack()) {
            player.sendMessage(color("&cYou already have the resource pack applied!"));
            return false;
        }

        List<String> urls = new ArrayList<>();
        if (config.getBoolean("use-forcepack-url")) {
            Plugin forcePack = plugin.getServer().getPluginManager().getPlugin("ForcePack");
            if (forcePack != null) {
                urls.addAll(forcePack.getConfig().getStringList("Server.packs.all.urls"));
            }
        } else {
            urls.add(plugin.getServer().getResourcePack());
        }

        for (String link : urls) {
            try {
                Pair<String, byte[]> data = getDataFromUrl(link);
                byte[] hash = data.getValue();
                try {
                    player.addResourcePack(UUID.nameUUIDFromBytes(hash), link, null, null, false);
                } catch (NoSuchMethodError e) {
                    player.setResourcePack(link);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
