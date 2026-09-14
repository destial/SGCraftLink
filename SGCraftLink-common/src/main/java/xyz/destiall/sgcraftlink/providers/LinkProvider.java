package xyz.destiall.sgcraftlink.providers;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.util.Tristate;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LinkProvider {
    protected final PluginProvider plugin;
    protected final Map<Integer, String> awaitingLinking;
    protected ConfigSectionProvider<?> database;
    protected final File databaseFile;

    public LinkProvider(PluginProvider plugin) {
        this.plugin = plugin;
        this.awaitingLinking = new ConcurrentHashMap<>();
        databaseFile = new File(plugin.getDataFolder(), "database.yml");

        if (!databaseFile.exists()) {
            try {
                databaseFile.createNewFile();
            } catch (IOException e) {
                plugin.getLoggerProvider().error("Unable to create database file!");
            }
        }
    }

    public int generateCode(String name) {
        int code;
        while (awaitingLinking.containsKey(code = (int) (Math.random() * 1000000))) {}
        awaitingLinking.put(code, name);
        return code;
    }

    public String getName(int code) {
        return awaitingLinking.get(code);
    }

    public String getId(String name) {
        ConfigSectionProvider<?> data = getData(name);
        if (data == null)
            return null;
        if (!data.contains("id"))
            return null;
        return data.getString("id");
    }

    public User getUser(String name) {
        String id = getId(name);
        if (id == null)
            return null;
        return plugin.getDiscordProvider().getJda().retrieveUserById(id).complete();
    }

    public boolean link(String name, User user) {
        ConfigSectionProvider<?> map = plugin.getConfigProvider().createSection();
        map.set("id", user.getId());
        map.set("messages", 0);
        database.set(name, map);
        plugin.getConfigProvider().save();

        plugin.getPlaytimeProvider().link(name);
        PlayerProvider player = plugin.getPlayer(name);
        if (player.isOnline()) {
            plugin.getDiscordProvider().link(player, user);
            ComponentProvider<?> message = ComponentProvider.of("You have linked your account to " + user.getAsTag());
            message.setColor(ColorProvider.ChatColor.AQUA);
            player.sendMessageComponent(message);
        }
        return true;
    }

    public void unlinkAllId(String id) {
        List<String> keysToRemove = new ArrayList<>();
        for (String key : database.getKeys()) {
            String id2 = getId(key);
            if (id2.equals(id)) {
                keysToRemove.add(key);
                break;
            }
        }
        for (String key : keysToRemove) {
            database.remove(key);
        }
        save();
    }

    public boolean isLinked(String name) {
        return database.contains(name);
    }

    public List<String> getUsernames(User user) {
        List<String> usernames = new ArrayList<>();
        for (String key : database.getKeys()) {
            String id = getId(key);
            if (user.getId().equals(id)) {
                usernames.add(key);
            }
        }
        return usernames;
    }

    public int getMessages(String name) {
        ConfigSectionProvider<?> data = getData(name);
        if (data == null) return 0;
        return data.getInt("messages", 0);
    }

    public boolean invalidate(int code) {
        return awaitingLinking.remove(code) != null;
    }


    public void updateUser(String name, ConfigSectionProvider<?> data) {
        database.set(name, data.getBase());
        save();
    }

    public void unlink(String name) {
        Member member = plugin.getDiscordProvider().getMember(getId(name));
        if (member == null)
            return;

        database.remove(name);
        save();

        plugin.getDiscordProvider().unlink(member, false);
        plugin.getPlaytimeProvider().unlink(member.getId());

        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage("You have unlinked this account from " + name).queue());
    }

    public void unlink(PlayerProvider player) {
        String name = player.getName();
        unlink(name);

        if (player.isOnline()) {
            plugin.getLuckPermsManager().getApi().getUserManager().modifyUser(player.getUUID(), u -> {
                Node node = InheritanceNode.builder("booster").build();
                if (u.data().contains(node, NodeEqualityPredicate.ONLY_KEY) == Tristate.TRUE) {
                    u.data().remove(node);
                    plugin.getLoggerProvider().info(name + " is unlinking his Nitro! Removing rank...");
                }

                Node linked = InheritanceNode.builder("dclinked").build();
                if (u.data().contains(linked, NodeEqualityPredicate.ONLY_KEY) == Tristate.TRUE) {
                    u.data().remove(linked);
                    plugin.getLoggerProvider().info(name + " is unlinking! Removing rank...");
                }
            });
        }
    }

    public Collection<String> getDatabase() {
        return database.getKeys();
    }

    public abstract void save();
    public abstract ConfigSectionProvider<?> getData(String user);

}
