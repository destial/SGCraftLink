package xyz.destiall.sgcraftlink.providers;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class PlaytimeProvider {
    protected final PluginProvider plugin;
    protected final File databaseFile;
    protected ConfigSectionProvider<?> database;

    public PlaytimeProvider(PluginProvider plugin) {
        this.plugin = plugin;

        databaseFile = new File(plugin.getDataFolder(), "playtime.yml");
        if (!databaseFile.exists()) {
            try {
                databaseFile.createNewFile();
            } catch (IOException e) {
                plugin.getLoggerProvider().error("Unable to create playtime database file!");
            }
        }

        plugin.getScheduler().schedule(() -> {
            long now = System.currentTimeMillis();
            for (String name : plugin.getLinkProvider().getDatabase()) {
                long firstJoin = getFirstJoin(name);
                int daysSince = (int) ((now - firstJoin) / 1000 / 60 / 60 / 24);
                for (Playtime playtime : plugin.getDiscordProvider().getPlaytimeMap().values()) {
                    if (daysSince >= playtime.getDays()) {
                        User user = plugin.getLinkProvider().getUser(name);
                        if (user == null) continue;
                        Member member = plugin.getDiscordProvider().getMember(user.getId());
                        if (member == null || member.getRoles().contains(playtime.getRole())) continue;
                        member.getGuild().addRoleToMember(member, playtime.getRole()).queue();
                    }
                }
            }
        }, 1, 10, TimeUnit.MINUTES);
    }

    public abstract void save();

    public boolean setFirstJoin(String player, long time) {
        long previous = getFirstJoin(player);
        if (previous > time) {
            database.set(player, time);
            return true;
        }
        return false;
    }

    public long getFirstJoin(String player) {
        return database.getLong(player, Long.MAX_VALUE);
    }

    public void unlink(String id) {
        Member member = plugin.getDiscordProvider().getMember(id);
        if (member == null) return;
        List<Role> rolesToRemove = new ArrayList<>();
        for (Playtime playtime : plugin.getDiscordProvider().getPlaytimeMap().values()) {
            rolesToRemove.add(playtime.getRole());
        }
        try {
            member.getGuild().modifyMemberRoles(member, null, rolesToRemove).queue(q -> {
            }, err -> {
            });
        } catch (Exception ignored) {}
    }

    public void link(String name) {
        long now = System.currentTimeMillis();
        long firstJoin = getFirstJoin(name);
        int daysSince = (int) ((now - firstJoin) / 1000f / 60f / 60f / 24f);
        String id = plugin.getLinkProvider().getId(name);
        if (id == null)
            return;

        Member member = plugin.getDiscordProvider().getMember(id);
        if (member == null)
            return;

        for (Playtime playtime : plugin.getDiscordProvider().getPlaytimeMap().values()) {
            if (daysSince >= playtime.getDays()) {
                if (member.getRoles().contains(playtime.getRole()))
                    continue;
                int messages = plugin.getLinkProvider().getMessages(name);
                if (messages < playtime.getMessages())
                    continue;

                member.getGuild().addRoleToMember(member, playtime.getRole()).queue();
            }
        }
    }

    public int getLongestMessages() {
        long longest = 0;
        for (Playtime playtime : plugin.getDiscordProvider().getPlaytimeMap().values()) {
            if (playtime.getMessages() > longest) {
                longest = playtime.getMessages();
            }
        }
        return (int) longest;
    }

}
