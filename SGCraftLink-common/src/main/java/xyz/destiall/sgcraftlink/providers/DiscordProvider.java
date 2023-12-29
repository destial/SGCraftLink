package xyz.destiall.sgcraftlink.providers;

import me.scarsz.jdaappender.ChannelLoggingHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView;
import net.luckperms.api.node.types.InheritanceNode;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.proxy.listeners.AppealListener;
import xyz.destiall.sgcraftlink.proxy.listeners.ConsoleCommandListener;
import xyz.destiall.sgcraftlink.proxy.listeners.DiscordCommandListener;
import xyz.destiall.sgcraftlink.proxy.listeners.LinkListener;
import xyz.destiall.sgcraftlink.proxy.listeners.ReportButtonListener;
import xyz.destiall.sgcraftlink.proxy.listeners.StaffAppListener;
import xyz.destiall.sgcraftlink.proxy.litebans.PunishmentListener;
import xyz.destiall.sgcraftlink.utils.DataResult;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;

public class DiscordProvider {
    protected final PluginProvider plugin;
    protected JDA jda;
    protected Map<String, Role> roleMap;
    protected Map<String, Playtime> playtimeMap;
    protected Role linkedRole;
    protected ChannelLoggingHandler consoleLogger;
    protected TextChannel consoleChannel;
    protected StaffAppListener staffApp;
    protected DiscordCommandListener dcl;
    protected LinkListener linkListener;
    protected PunishmentListener punishmentListener;

    public DiscordProvider(PluginProvider plugin) {
        this.plugin = plugin;
        ConfigProvider<?> configManager = plugin.getConfigProvider();
        try {
            dcl = new DiscordCommandListener(plugin);
            jda = JDABuilder.createDefault(configManager.getString("token"))
                    .enableIntents(EnumSet.allOf(GatewayIntent.class))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();
            jda.addEventListener(
                    dcl,
                    linkListener = new LinkListener(plugin),
                    new AppealListener(plugin),
                    staffApp = new StaffAppListener(plugin),
                    new ReportButtonListener(plugin),
                    new ConsoleCommandListener(plugin, this));
            jda.awaitReady();

            for (Guild guild : jda.getGuilds()) {
                guild.loadMembers().onSuccess(l ->
                    plugin.getLoggerProvider().info("Loaded all members in " + guild.getName())
                );
            }

            dcl.registerCommands(jda);
        } catch (LoginException | InterruptedException e) {
            jda = null;
            e.printStackTrace();
        }

        setup(configManager);

        hookConsole();
        hookPunishmentListener();
    }

    private void hookPunishmentListener() {
        if (jda == null)
            return;

        punishmentListener = new PunishmentListener(plugin);
    }

    public PunishmentListener getPunishmentListener() {
        return punishmentListener;
    }

    public void hookConsole() {
        if (jda == null)
            return;

        boolean serverIsLog4jCapable = false;
        try {
            Class.forName("org.apache.logging.log4j.core.Logger");
            serverIsLog4jCapable = true;
        } catch (ClassNotFoundException e) {
            plugin.getLoggerProvider().error("Log4j classes are NOT available, console channel will not be attached");
        }

        if (!serverIsLog4jCapable)
            return;

        ConfigProvider<?> configManager = plugin.getConfigProvider();
        String id = configManager.getConfig().getString("console-channel", "1099010306396655707");
        consoleChannel = jda.getTextChannelById(id);

        if (consoleChannel == null) {
            plugin.getLoggerProvider().error("Channel with id " + id + " does not exist!");
            return;
        }

        consoleLogger = new ChannelLoggingHandler(() -> consoleChannel, config -> config.setColored(true)).attach().schedule();
    }

    public ChannelLoggingHandler getConsoleLogger() {
        return consoleLogger;
    }

    public TextChannel getConsoleChannel() {
        return consoleChannel;
    }

    public void setup(ConfigProvider<?> configManager) {
        if (jda == null) return;

        roleMap = new HashMap<>();
        try {
            linkedRole = jda.getRoleById(configManager.getString("link-role-id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String key : configManager.getConfig().getSection("role-map").getKeys()) {
            try {
                String id = configManager.getString("role-map." + key);
                Role role = jda.getRoleById(id);
                if (role == null) {
                    for (Guild guild : jda.getGuilds()) {
                        role = guild.getRoleById(id);
                        if (role != null) break;
                    }
                    if (role == null) continue;
                }
                roleMap.put(key, role);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        playtimeMap = new HashMap<>();
        for (String key : configManager.getConfig().getSection("playtime-role-map").getKeys()) {
            int days = configManager.getConfig().getInt("playtime-role-map." + key + ".length");
            int messages = configManager.getConfig().getInt("playtime-role-map." + key + ".messages", 0);
            String id = configManager.getConfig().getString("playtime-role-map." + key + ".id");
            Role role = jda.getRoleById(id);
            if (role == null) {
                for (Guild guild : jda.getGuilds()) {
                    role = guild.getRoleById(id);
                    if (role != null) break;
                }
                if (role == null) continue;
            }
            Playtime playtime = new Playtime(key, days, messages, role);
            playtimeMap.put(key, playtime);
        }
    }

    public Map<String, Role> getRoleMap() {
        return roleMap;
    }

    public Map<String, Playtime> getPlaytimeMap() {
        return playtimeMap;
    }

    public Member getMember(String id) {
        try {
            return linkedRole.getGuild().retrieveMemberById(id).complete();
        } catch (Exception ignored) {
            return null;
        }
    }

    public void sendReport(PlayerProvider player, PlayerProvider target, String message) {
        TextChannel channel = jda.getTextChannelById("1097754814223487037");
        if (channel == null)
            return;

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.RED)
               .setTitle(target.getName())
               .setFooter("Server: " + target.getServer() + " | Reporter: " + player.getName())
               .setDescription(message);

        ActionRow row = ActionRow.of(
                Button.primary("sgc-report-accepted", "Look Into"),
                Button.danger("sgc-report-denied", "Delete"),
                Button.secondary("sgc-report-history", "History"),
                Button.secondary("sgc-report-chathistory", "Chat History"));

        channel.sendMessageEmbeds(builder.build()).setActionRows(row).queue();
    }

    public void shutdown() {
        if (jda == null)
            return;
        if (consoleLogger != null) {
            consoleLogger.shutdown();
            consoleLogger = null;
        }

        dcl.unregisterCommands();
        jda.shutdown();
        jda = null;
        if (punishmentListener != null) {
            punishmentListener.close();
            punishmentListener = null;
        }
        linkListener.getSub().close();
    }

    public JDA getJda() {
        return jda;
    }

    public StaffAppListener getStaffAppListener() {
        return staffApp;
    }

    public LinkListener getLinkListener() {
        return linkListener;
    }

    public void updateRankStatus(PlayerProvider player, Member member) {
        Map<String, Object> body = createDataMap("uuid", player.getUUID().toString());
        Set<String> ranks = ConcurrentHashMap.newKeySet();
        plugin.getDataManager().sendDataToAll(null, SGCraftLinkCommon.DATA_GET_RANKS, body, (mappings) -> {
            for (DataResult result : mappings.values()) {
                if (!result.isSuccess())
                    continue;
                JSONObject object = result.getJSON();
                JSONArray rankArray = object.getJSONArray("ranks");
                for (int i = 0; i < rankArray.length(); i++) {
                    String rankName = rankArray.getString(i);
                    ranks.add(rankName);
                }

                Map<String, Role> roleMap = plugin.getDiscordProvider().getRoleMap();
                List<Role> rolesToAdd = roleMap.entrySet().stream().filter(en -> ranks.stream().anyMatch(r -> r.equalsIgnoreCase(en.getKey()))).map(Map.Entry::getValue).collect(Collectors.toList());
                List<Role> rolesToRemove = roleMap.values().stream().filter(r -> !rolesToAdd.contains(r)).collect(Collectors.toList());

                try {
                    member.getGuild().modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue(q -> {}, err -> {});
                } catch (Exception ignored) {}
            }
        });
    }

    public void link(PlayerProvider player, User user) {
        linkedRole.getGuild().retrieveMember(user).queue(m -> {
            updateRankStatus(player, m);
            player.sendPluginMessage(SGCraftLinkCommon.GET_FIRSTJOIN);

            plugin.getLuckPermsManager().getApi().getUserManager().modifyUser(player.getUUID(), u -> {
                u.data().add(InheritanceNode.builder("dclinked").build());
                plugin.getLoggerProvider().info(player.getName() + " is a newly linked member! Adding rank...");
                if (m.isBoosting()) {
                    u.data().add(InheritanceNode.builder("booster").build());
                    plugin.getLoggerProvider().info(player.getName() + " is a newly linked Nitro Booster! Adding rank...");
                }
            });

            updateNickname(player, m);
        }, e -> {});
    }

    public void updateNickname(PlayerProvider player, Member member) {
        String nickname = player.getName();
        String suffix = player.getSuffix();
        if (suffix != null) {
            nickname += " " + suffix;
        }
        try {
            member.modifyNickname(nickname).queue(q -> {
            }, err -> {
            });
        } catch (Exception ignored) {}
    }

    public void unlink(Member m, boolean left) {
        if (left)
            return;

        List<Role> rolesToRemove = new ArrayList<>(getRoleMap().values());
        rolesToRemove.add(linkedRole);
        try {
            m.getGuild().modifyMemberRoles(m, null, rolesToRemove).queue(q -> {
            }, err -> {
            });
        } catch (Exception ignored) {}
    }

    public void getMessagesSize(User user, Consumer<Integer> amount) {
        final Guild guild = linkedRole.getGuild();
        AtomicInteger integer = new AtomicInteger(0);
        SortedSnowflakeCacheView<TextChannel> view = guild.getTextChannelCache();
        Set<TextChannel> set = new HashSet<>(view.asSet());
        Member member = getMember(user.getId());
        set.removeIf(t -> !t.canTalk(member));
        int size = set.size();
        final boolean[] complete = new boolean[size];
        AtomicBoolean needed = new AtomicBoolean(true);
        int n = 0;
        for (TextChannel channel : set) {
            final int finalN = n++;
            channel.getIterableHistory()
                    .takeWhileAsync(m -> {
                        System.gc();
                        System.gc();
                        return needed.get();
                    })
                    .thenApply(list -> list.stream().filter(m -> m.getAuthor().equals(user)).collect(Collectors.toCollection(ArrayList::new)))
                    .thenApply(list -> {
                        int count = list.size();
                        integer.addAndGet(count);
                        complete[finalN] = true;
                        list.clear();
                        list = null;

                        boolean c = true;
                        for (int j = 0; j < size; j++) {
                            if (!complete[j]) {
                                c = false;
                                break;
                            }
                        }
                        c = c || integer.get() >= plugin.getPlaytimeProvider().getLongestMessages();

                        plugin.getLoggerProvider().info("Retrieved " + count + " from " + channel.getName());

                        if (c) {
                            int a = integer.get();
                            amount.accept(a);
                            String username = plugin.getLinkProvider().getUsername(user);
                            if (username != null) {
                                ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(username);
                                data.set("messages", a);
                                plugin.getLinkProvider().updateUser(username, data);
                                plugin.getLoggerProvider().info("Updated " + username + " with " + integer.get() + " messages.");
                            }
                            needed.set(false);
                        }

                        System.gc();
                        System.gc();
                        return true;
                    });
        }
    }
}
