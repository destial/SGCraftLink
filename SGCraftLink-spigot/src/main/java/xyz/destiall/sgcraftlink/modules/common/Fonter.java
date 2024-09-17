package xyz.destiall.sgcraftlink.modules.common;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.FontUserSettings;
import xyz.destiall.sgcraftlink.items.font.DefaultFont;
import xyz.destiall.sgcraftlink.items.font.Font;
import xyz.destiall.sgcraftlink.modules.Module;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

// Hi-jack ProtocolLib packets to translate all chat and display packets
public class Fonter extends Module implements CommandExecutor, TabExecutor {
    private Map<UUID, FontUserSettings> users;
    private boolean dirty = false;
    private Font defaultFont;
    private List<Font> fonts;

    private boolean convertChat = true;
    private boolean convertEntityMetadata = true;
    private boolean convertBossBar = true;
    private boolean convertActionBar = true;
    private boolean convertTitle = true;

    public Fonter(SGCraftLinkSpigot plugin) {
        super(plugin, "fonts.yml");
        setCommandExecutor("font");
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("conversion.chat", convertChat = true);
        config.set("conversion.entity-name", convertEntityMetadata = true);
        config.set("conversion.boss-bar", convertBossBar = true);
        config.set("conversion.action-bar", convertActionBar = true);
        config.set("conversion.title-subtitle", convertTitle = true);
        config.set("default-font", "default");

        config.set("fonts.sleek.ignore-case", true);
        config.set("fonts.sleek.lower", "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ");

        config.set("fonts.mono.ignore-case", false);
        config.set("fonts.mono.two-char-per", true);
        config.set("fonts.mono.lower", "\uD835\uDC1A\uD835\uDC1B\uD835\uDC1C\uD835\uDC1D\uD835\uDC1E\uD835\uDC1F\uD835\uDC20\uD835\uDC21\uD835\uDC22\uD835\uDC23\uD835\uDC24\uD835\uDC25\uD835\uDC26\uD835\uDC27\uD835\uDC28\uD835\uDC29\uD835\uDC2A\uD835\uDC2B\uD835\uDC2C\uD835\uDC2D\uD835\uDC2E\uD835\uDC2F\uD835\uDC30\uD835\uDC31\uD835\uDC32\uD835\uDC33");
        config.set("fonts.mono.upper", "\uD835\uDC00\uD835\uDC01\uD835\uDC02\uD835\uDC03\uD835\uDC04\uD835\uDC05\uD835\uDC06\uD835\uDC07\uD835\uDC08\uD835\uDC09\uD835\uDC0A\uD835\uDC0B\uD835\uDC0C\uD835\uDC0D\uD835\uDC0E\uD835\uDC0F\uD835\uDC10\uD835\uDC11\uD835\uDC12\uD835\uDC13\uD835\uDC14\uD835\uDC15\uD835\uDC16\uD835\uDC17\uD835\uDC18\uD835\uDC19");
        config.set("fonts.mono.number", "\uD835\uDFEC\uD835\uDFED\uD835\uDFEE\uD835\uDFEF\uD835\uDFF0\uD835\uDFF1\uD835\uDFF2\uD835\uDFF3\uD835\uDFF4\uD835\uDFF5");

        config.set("fonts.roman.ignore-case", false);
        config.set("fonts.roman.two-char-per", true);
        config.set("fonts.roman.lower", "\uD835\uDE8A\uD835\uDE8B\uD835\uDE8C\uD835\uDE8D\uD835\uDE8E\uD835\uDE8F\uD835\uDE90\uD835\uDE91\uD835\uDE92\uD835\uDE93\uD835\uDE94\uD835\uDE95\uD835\uDE96\uD835\uDE97\uD835\uDE98\uD835\uDE99\uD835\uDE9A\uD835\uDE9B\uD835\uDE9C\uD835\uDE9D\uD835\uDE9E\uD835\uDE9F\uD835\uDEA0\uD835\uDEA1\uD835\uDEA2\uD835\uDEA3");
        config.set("fonts.roman.upper", "\uD835\uDE70\uD835\uDE71\uD835\uDE72\uD835\uDE73\uD835\uDE74\uD835\uDE75\uD835\uDE76\uD835\uDE77\uD835\uDE78\uD835\uDE79\uD835\uDE7A\uD835\uDE7B\uD835\uDE7C\uD835\uDE7D\uD835\uDE7E\uD835\uDE7F\uD835\uDE80\uD835\uDE81\uD835\uDE82\uD835\uDE83\uD835\uDE84\uD835\uDE85\uD835\uDE86\uD835\uDE87\uD835\uDE88\uD835\uDE89");
        config.set("fonts.roman.number", "\uD835\uDFF6\uD835\uDFF7\uD835\uDFF8\uD835\uDFF9\uD835\uDFFA\uD835\uDFFB\uD835\uDFFC\uD835\uDFFD\uD835\uDFFE\uD835\uDFFF");

        config.set("fonts.greek.ignore-case", true);
        config.set("fonts.greek.two-char-per", false);
        config.set("fonts.greek.lower", "αв¢∂єƒgнιנкℓмησρqяѕтυνωχуz");

        config.set("fonts.dollar.ignore-case", true);
        config.set("fonts.dollar.two-char-per", false);
        config.set("fonts.dollar.lower", "₳฿₵ĐɆ₣₲ⱧłJ₭Ⱡ₥₦Ø₱QⱤ₴₮ɄV₩ӾɎⱫ");

        config.set("fonts.boxes.ignore-case", true);
        config.set("fonts.boxes.two-char-per", true);
        config.set("fonts.boxes.lower", "\uD83C\uDD30\uD83C\uDD31\uD83C\uDD32\uD83C\uDD33\uD83C\uDD34\uD83C\uDD35\uD83C\uDD36\uD83C\uDD37\uD83C\uDD38\uD83C\uDD39\uD83C\uDD3A\uD83C\uDD3B\uD83C\uDD3C\uD83C\uDD3D\uD83C\uDD3E\uD83C\uDD3F\uD83C\uDD40\uD83C\uDD41\uD83C\uDD42\uD83C\uDD43\uD83C\uDD44\uD83C\uDD45\uD83C\uDD46\uD83C\uDD47\uD83C\uDD48\uD83C\uDD49");

        config.set("fonts.cursive.ignore-case", false);
        config.set("fonts.cursive.two-char-per", true);
        config.set("fonts.cursive.lower", "\uD835\uDCB6\uD835\uDCB7\uD835\uDCB8\uD835\uDCB9\uD835\uDC52\uD835\uDCBB\uD835\uDC54\uD835\uDCBD\uD835\uDCBE\uD835\uDCBF\uD835\uDCC0\uD835\uDCC1\uD835\uDCC2\uD835\uDCC3\uD835\uDC5C\uD835\uDCC5\uD835\uDCC6\uD835\uDCC7\uD835\uDCC8\uD835\uDCC9\uD835\uDCCA\uD835\uDCCB\uD835\uDCCC\uD835\uDCCD\uD835\uDCCE\uD835\uDCCF");
        config.set("fonts.cursive.upper", "\uD835\uDC9C\uD835\uDC35\uD835\uDC9E\uD835\uDC9F\uD835\uDC38\uD835\uDC39\uD835\uDCA2\uD835\uDC3B\uD835\uDC3C\uD835\uDCA5\uD835\uDCA6\uD835\uDC3F\uD835\uDC40\uD835\uDCA9\uD835\uDCAA\uD835\uDCAB\uD835\uDCAC\uD835\uDC45\uD835\uDCAE\uD835\uDCAF\uD835\uDCB0\uD835\uDCB1\uD835\uDCB2\uD835\uDCB3\uD835\uDCB4\uD835\uDCB5");
        config.set("fonts.cursive.number", "\uD835\uDFE2\uD835\uDFE3\uD835\uDFE4\uD835\uDFE5\uD835\uDFE6\uD835\uDFE7\uD835\uDFE8\uD835\uDFE9\uD835\uDFEA\uD835\uDFEB");

        config.set("fonts.gothic.ignore-case", false);
        config.set("fonts.gothic.two-char-per", true);
        config.set("fonts.gothic.lower", "\uD835\uDD1E\uD835\uDD1F\uD835\uDD20\uD835\uDD21\uD835\uDD22\uD835\uDD23\uD835\uDD24\uD835\uDD25\uD835\uDD26\uD835\uDD27\uD835\uDD28\uD835\uDD29\uD835\uDD2A\uD835\uDD2B\uD835\uDD2C\uD835\uDD2D\uD835\uDD2E\uD835\uDD2F\uD835\uDD30\uD835\uDD31\uD835\uDD32\uD835\uDD33\uD835\uDD34\uD835\uDD35\uD835\uDD36\uD835\uDD37");
        config.set("fonts.gothic.upper", "\uD835\uDD04\uD835\uDD05ℭ\uD835\uDD07\uD835\uDD08\uD835\uDD09\uD835\uDD0Aℌℑ\uD835\uDD0D\uD835\uDD0E\uD835\uDD0F\uD835\uDD10\uD835\uDD11\uD835\uDD12\uD835\uDD13\uD835\uDD14ℜ\uD835\uDD16\uD835\uDD17\uD835\uDD18\uD835\uDD19\uD835\uDD1A\uD835\uDD1B\uD835\uDD1Cℨ");

        config.set("fonts.hollow.ignore-case", false);
        config.set("fonts.hollow.two-char-per", true);
        config.set("fonts.hollow.lower", "\uD835\uDD52\uD835\uDD53\uD835\uDD54\uD835\uDD55\uD835\uDD56\uD835\uDD57\uD835\uDD58\uD835\uDD59\uD835\uDD5A\uD835\uDD5B\uD835\uDD5C\uD835\uDD5D\uD835\uDD5E\uD835\uDD5F\uD835\uDD60\uD835\uDD61\uD835\uDD62\uD835\uDD63\uD835\uDD64\uD835\uDD65\uD835\uDD66\uD835\uDD67\uD835\uDD68\uD835\uDD69\uD835\uDD6A\uD835\uDD6B");
        config.set("fonts.hollow.upper", "\uD835\uDD38\uD835\uDD39ℂ\uD835\uDD3B\uD835\uDD3C\uD835\uDD3D\uD835\uDD3Eℍ\uD835\uDD40\uD835\uDD41\uD835\uDD42\uD835\uDD43\uD835\uDD44ℕ\uD835\uDD46ℙℚℝ\uD835\uDD4A\uD835\uDD4B\uD835\uDD4C\uD835\uDD4D\uD835\uDD4E\uD835\uDD4F\uD835\uDD50ℤ");
        config.set("fonts.hollow.number", "\uD835\uDFD8\uD835\uDFD9\uD835\uDFDA\uD835\uDFDB\uD835\uDFDC\uD835\uDFDD\uD835\uDFDE\uD835\uDFDF\uD835\uDFE0\uD835\uDFE1");

        config.set("fonts.round.ignore-case", false);
        config.set("fonts.round.two-char-per", false);
        config.set("fonts.round.lower", "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ");
        config.set("fonts.round.upper", "ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ");
        config.set("fonts.round.number", "⓪①②③④⑤⑥⑦⑧⑨");
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

    public FontUserSettings getFontUser(UUID player) {
        return users.getOrDefault(player, new FontUserSettings(defaultFont));
    }

    @Override
    public void enable() {
        if (!config.getBoolean("enabled", true))
            return;

        ConfigurationSection fonts = config.getConfigurationSection("fonts");
        this.fonts = new ArrayList<>();
        this.fonts.add(new DefaultFont());
        if (fonts != null) {
            for (String name : fonts.getKeys(false)) {
                try {
                    Font font = new Font(name, Objects.requireNonNull(fonts.getConfigurationSection(name)));
                    this.fonts.add(font);
                } catch (Exception e) {
                    plugin.getLogger().warning("Unable to load font '" + name + "'");
                    e.printStackTrace();
                }
            }
        }

        String def = config.getString("default-font", "default");
        defaultFont = getFontFromName(def);
        if (defaultFont == null) {
            defaultFont = this.fonts.get(0);
            plugin.getLogger().warning("Unable to load font '" + def + "' as default. Using normal font...");
            dirty = true;
        }

        users = new HashMap<>();
        ConfigurationSection saved = config.getConfigurationSection("users");
        if (saved != null) {
            for (String uuid : saved.getKeys(false)) {
                Object o = saved.get(uuid);
                FontUserSettings settings;
                if (o instanceof String) {
                    Font font = getFontFromName((String) o);
                    if (font == null)
                        continue;

                    settings = new FontUserSettings(font);
                    config.set("users." + uuid + ".font", font.getName());
                    config.set("users." + uuid + ".action-bar", true);
                    config.set("users." + uuid + ".chat", true);
                    config.set("users." + uuid + ".boss-bar", true);
                    config.set("users." + uuid + ".entity-name", true);
                    config.set("users." + uuid + ".title", true);
                    dirty = true;
                } else {
                    ConfigurationSection section = saved.getConfigurationSection(uuid);
                    if (section == null) {
                        continue;
                    }

                    Font font = getFontFromName(section.getString("font"));
                    if (font == null) {
                        font = defaultFont;
                    }
                    settings = new FontUserSettings(font);
                    settings.setConvertActionBar(section.getBoolean("action-bar", true));
                    settings.setConvertChat(section.getBoolean("chat", true));
                    settings.setConvertBossBar(section.getBoolean("boss-bar", true));
                    settings.setConvertEntityMetadata(section.getBoolean("entity-name", true));
                    settings.setConvertTitle(section.getBoolean("title", true));
                }
                users.put(UUID.fromString(uuid), settings);
            }
        }

        plugin.getLogger().info("Registered " + this.fonts.size() + " fonts " + new JSONArray(this.fonts.stream().map(Font::getName).collect(Collectors.toList())).toString().replace("\"", ""));

        convertChat = config.getBoolean("conversion.chat", convertChat);
        convertEntityMetadata = config.getBoolean("conversion.entity-name", convertEntityMetadata);
        convertBossBar = config.getBoolean("conversion.boss-bar", convertBossBar);
        convertActionBar = config.getBoolean("conversion.action-bar", convertActionBar);
        convertTitle = config.getBoolean("conversion.title-subtitle", convertTitle);

        if (dirty) {
            saveConfig();
            dirty = false;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.DISGUISED_CHAT,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SET_ACTION_BAR_TEXT,
                PacketType.Play.Server.SET_TITLE_TEXT,
                PacketType.Play.Server.SET_SUBTITLE_TEXT,
                PacketType.Play.Server.BOSS,
                PacketType.Play.Server.ENTITY_METADATA
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                FontUserSettings fontUserSettings = getFontUser(event.getPlayer().getUniqueId());
                Font font = fontUserSettings.getFont();
                if (font instanceof DefaultFont)
                    return;

                PacketContainer container = event.getPacket();
                WrappedChatComponent component = null;
                if (container.getType() == PacketType.Play.Server.ENTITY_METADATA) {
                    if (!convertEntityMetadata || !fontUserSettings.isConvertEntityMetadata())
                        return;
                    try {
                        Class.forName("com.comphenix.protocol.wrappers.WrappedDataValue");
                        List<WrappedDataValue> wrappedDataValueList = container.getDataValueCollectionModifier().read(0);
                        for (WrappedDataValue dv : wrappedDataValueList) {
                            Object o = dv.getValue();
                            if (dv.getIndex() == 2) {
                                boolean optional = false;
                                try {
                                    component = (WrappedChatComponent) o;
                                } catch (Exception ex) {
                                    optional = true;
                                    component = ((Optional<WrappedChatComponent>) o).orElse(null);
                                }
                                if (component != null) {
                                    JSONObject object = new JSONObject(component.getJson());
                                    chatComponent(object, font, false);
                                    component.setJson(object.toString());
                                    dv.setValue(optional ? Optional.of(component) : component);
                                }
                            }
                        }
                        container.getDataValueCollectionModifier().write(0, wrappedDataValueList);
                    } catch (Exception e) {
                        try {
                            List<WrappedWatchableObject> wrappedWatchableObjects = container.getWatchableCollectionModifier().read(0);
                            for (WrappedWatchableObject wo : wrappedWatchableObjects) {
                                Object o = wo.getValue();
                                if (wo.getIndex() == 2) {
                                    boolean optional = false;
                                    try {
                                        component = (WrappedChatComponent) o;
                                    } catch (Exception ex) {
                                        optional = true;
                                        component = ((Optional<WrappedChatComponent>) o).orElse(null);
                                    }
                                    if (component != null) {
                                        JSONObject object = new JSONObject(component.getJson());
                                        chatComponent(object, font, false);
                                        component.setJson(object.toString());
                                        wo.setValue(optional ? Optional.of(component) : component);
                                    }
                                }
                            }
                            container.getWatchableCollectionModifier().write(0, wrappedWatchableObjects);
                        } catch (Exception ex) {
                            WrappedDataWatcher dataWatcher = container.getDataWatcherModifier().read(0);
                            for (WrappedWatchableObject wo : dataWatcher.getWatchableObjects()) {
                                Object o = wo.getValue();
                                if (wo.getIndex() == 2) {
                                    boolean optional = false;
                                    try {
                                        component = (WrappedChatComponent) o;
                                    } catch (Exception exx) {
                                        optional = true;
                                        component = ((Optional<WrappedChatComponent>) o).orElse(null);
                                    }
                                    if (component != null) {
                                        JSONObject object = new JSONObject(component.getJson());
                                        chatComponent(object, font, false);
                                        component.setJson(object.toString());
                                        wo.setValue(optional ? Optional.of(component) : component);
                                        dataWatcher.setObject(wo.getIndex(), wo);
                                    }
                                }
                            }
                            container.getDataWatcherModifier().write(0, dataWatcher);
                        }
                    }
                    return;
                }

                if (container.getType() == PacketType.Play.Server.BOSS) {
                    if (!convertBossBar || !fontUserSettings.isConvertBossBar())
                        return;

                    StructureModifier<?> m = container.getModifier();
                    for (int i = 0; i < m.size(); i++) {
                        Object bar = m.readSafely(i);
                        if (bar != null) {
                            if (bar instanceof UUID)
                                continue;

                            for (Field f : FuzzyReflection.fromObject(bar, true).getFields()) {
                                if (MinecraftReflection.getIChatBaseComponentClass().equals(f.getType())) {
                                    try {
                                        f.setAccessible(true);
                                        component = WrappedChatComponent.fromHandle(f.get(bar));
                                        JSONObject object = new JSONObject(component.getJson());
                                        chatComponent(object, font, false);
                                        component.setJson(object.toString());
                                        f.set(bar, BukkitConverters.getWrappedChatComponentConverter().getGeneric(component));
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    return;
                }

                if (container.getType() == PacketType.Play.Server.SET_ACTION_BAR_TEXT && (!convertActionBar || !fontUserSettings.isConvertActionBar()))
                    return;

                if ((container.getType() == PacketType.Play.Server.SET_TITLE_TEXT || container.getType() == PacketType.Play.Server.SET_SUBTITLE_TEXT) && (!convertTitle || !fontUserSettings.isConvertTitle()))
                    return;

                if ((container.getType() == PacketType.Play.Server.CHAT || container.getType() == PacketType.Play.Server.SYSTEM_CHAT || container.getType() == PacketType.Play.Server.DISGUISED_CHAT) && (!convertChat || !fontUserSettings.isConvertChat()))
                    return;

                try {
                    StructureModifier<Object> adventureModifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
                    if (!adventureModifier.getFields().isEmpty()) { // Only true on Paper
                        Component comp = (Component) adventureModifier.read(0);
                        if (comp != null) {
                            component = AdventureComponentConverter.fromComponent(comp);
                            adventureModifier.write(0, null); // Reset to null - the JSON message will be set instead
                        }
                    }
                } catch (Throwable ignored) {} // Ignore if paper is unavailable

                try {
                    if (component == null) {
                        component = WrappedChatComponent.fromJson(container.getStrings().read(0));
                    }

                    JSONObject object = new JSONObject(component.getJson());
                    chatComponent(object, font, container.getType() == PacketType.Play.Server.SET_ACTION_BAR_TEXT);
                    container.getStrings().write(0, object.toString());
                    return;
                } catch (Throwable ignored) {} // If using adventure API

                try {
                    if (component == null) {
                        component = container.getChatComponents().read(0);
                    }

                    JSONObject object = new JSONObject(component.getJson());
                    chatComponent(object, font, container.getType() == PacketType.Play.Server.SET_ACTION_BAR_TEXT);
                    component.setJson(object.toString());
                    container.getChatComponents().write(0, component);
                } catch (Throwable ignored) {} // If all else fails
            }
        });
    }

    public void chatComponent(JSONObject object, Font font, boolean checkChar) {
        if (object.has("text")) {
            object.put("text", font.format(object.getString("text"), checkChar));
        }
        if (object.has("hoverEvent")) {
            JSONObject hoverEvent = object.getJSONObject("hoverEvent");
            if (hoverEvent.has("contents")) {
                JSONObject contents = hoverEvent.getJSONObject("contents");
                chatComponent(contents, font, checkChar);
                hoverEvent.put("contents", contents);
            }
            object.put("hoverEvent", hoverEvent);
        }
        if (object.has("extra")) {
            JSONArray extra = object.getJSONArray("extra");
            JSONArray update = new JSONArray();
            for (int i = 0; i < extra.length(); i++) {
                try {
                    JSONObject obj = extra.getJSONObject(i);
                    chatComponent(obj, font, checkChar);
                    update.put(obj);
                } catch (Exception e) {
                    String string = extra.getString(i);
                    JSONObject obj = new JSONObject();
                    obj.put("text", string);
                    chatComponent(obj, font, checkChar);
                    update.put(obj);
                }

            }
            object.put("extra", update);
        }
    }

    @Override
    public void disable() {
        unsetCommandExecutor("font");
        if (!config.getBoolean("enabled", true))
            return;

        fonts.clear();

        if (dirty) {
            saveConfig();
            dirty = false;
        }

        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return false;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /font [font|toggle] [setting]"));
            player.sendMessage(color("&cSet the font on your display!"));
            return false;
        }

        if (args[0].equalsIgnoreCase("reload") && player.hasPermission("sgc.font.admin")) {
            player.sendMessage(color("&aReloading fonts..."));
            disable();
            loadConfig();
            enable();
            return false;
        }

        if (args[0].equalsIgnoreCase("setdefault") && player.hasPermission("sgc.font.admin") && args.length == 2) {
            Font newDefault = getFontFromName(args[1]);
            if (newDefault != null) {
                defaultFont = newDefault;
                config.set("default-font", defaultFont.getName());
                dirty = true;
                saveConfig();
                player.sendMessage(color("&aSet global default font to: " + defaultFont.getName()));
                return false;
            }
            player.sendMessage(color("&cFont not found!"));
            return false;
        }

        if (args[0].equalsIgnoreCase("toggle") && args.length == 2) {
            FontUserSettings settings = getFontUser(player.getUniqueId());
            switch (args[1].toLowerCase()) {
                case "chat": {
                    settings.setConvertChat(!settings.isConvertChat());
                    config.set("users." + player.getUniqueId() + ".chat", settings.isConvertChat());
                    users.put(player.getUniqueId(), settings);
                    player.sendMessage(color("&eToggled chat font conversion to: " + (settings.isConvertChat() ? "&a&lON" : "&c&lOFF")));
                    break;
                }
                case "actionbar": {
                    settings.setConvertActionBar(!settings.isConvertActionBar());
                    config.set("users." + player.getUniqueId() + ".action-bar", settings.isConvertActionBar());
                    users.put(player.getUniqueId(), settings);
                    player.sendMessage(color("&eToggled action bar font conversion to: " + (settings.isConvertActionBar() ? "&a&lON" : "&c&lOFF")));
                    break;
                }
                case "bossbar": {
                    settings.setConvertBossBar(!settings.isConvertBossBar());
                    config.set("users." + player.getUniqueId() + ".boss-bar", settings.isConvertBossBar());
                    users.put(player.getUniqueId(), settings);
                    player.sendMessage(color("&eToggled boss bar font conversion to: " + (settings.isConvertBossBar() ? "&a&lON" : "&c&lOFF")));
                    break;
                }
                case "title": {
                    settings.setConvertTitle(!settings.isConvertTitle());
                    config.set("users." + player.getUniqueId() + ".title", settings.isConvertTitle());
                    users.put(player.getUniqueId(), settings);
                    player.sendMessage(color("&eToggled title font conversion to: " + (settings.isConvertTitle() ? "&a&lON" : "&c&lOFF")));
                    break;
                }
                case "entity-name": {
                    settings.setConvertEntityMetadata(!settings.isConvertEntityMetadata());
                    config.set("users." + player.getUniqueId() + ".entity-name", settings.isConvertEntityMetadata());
                    users.put(player.getUniqueId(), settings);
                    player.sendMessage(color("&eToggled entity font conversion to: " + (settings.isConvertEntityMetadata() ? "&a&lON" : "&c&lOFF")));
                    break;
                }
                default: {
                    player.sendMessage(color("&cUnknown setting of: " + args[1]));
                    return false;
                }
            }

            dirty = true;
            saveConfig();
            return false;
        }

        Font newFont = getFontFromName(args[0]);
        if (newFont == null) {
            player.sendMessage(color("&cFont not found!"));
            return false;
        }
        FontUserSettings settings = getFontUser(player.getUniqueId());
        settings.setFont(newFont);
        users.put(player.getUniqueId(), settings);
        config.set("users." + player.getUniqueId() + ".font", newFont.getName());
        dirty = true;
        saveConfig();
        player.sendMessage(color("&aYou have set your font to: " + newFont.getName()));
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0)
            return Collections.emptyList();

        if (args.length == 1 || (args.length == 2 && args[0].equalsIgnoreCase("setdefault") && commandSender.hasPermission("sgc.font.admin"))) {
            List<String> a = getFonts().stream().map(Font::getName).collect(Collectors.toList());
            if (args.length == 1) {
                a.add("toggle");
                if (commandSender.hasPermission("sgc.font.admin")) {
                    a.add("setdefault");
                }
            }
            return a.stream().filter(f -> f.startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return Stream.of("chat", "actionbar", "bossbar", "title", "entity-name").filter(f -> f.startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
