package xyz.destiall.sgcraftlink.modules.survival;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.playmode.MonoStereoMode;
import com.xxmicloxx.NoteBlockAPI.model.playmode.StereoMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import cz.studiodamage.NoteBlockMusicPlayer.NoteBlockMusicPlayer;
import cz.studiodamage.NoteBlockMusicPlayer.objects.SongWithMetadata;
import cz.studiodamage.NoteBlockMusicPlayer.player.Radio;
import cz.studiodamage.NoteBlockMusicPlayer.player.RadioConfig;
import cz.studiodamage.NoteBlockMusicPlayer.player.WorldRadio;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.hooks.WorldGuardHook;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MusicHook extends Module implements Listener {
    private final Set<RegionRadio> radios;
    private WorldGuardHook wg;

    public MusicHook(SGCraftLinkSpigot plugin) {
        super(plugin, "region-radio.yml");
        radios = new HashSet<>();
    }

    private Song getCurrentSong(Player player) {
        WorldRadio radio = NoteBlockMusicPlayer.getInstance().worldRadio.stream().filter(wr -> wr.getWorld().equals(player.getWorld())).findFirst().orElse(null);
        if (radio == null) {
            return null;
        }

        return radio.getSongPlayer().getSong();
    }

    public String getListeningTitle(Player player) {
        Song song = getCurrentSong(player);
        if (song == null)
            return "N/A";

        if (song instanceof SongWithMetadata) {
            return ((SongWithMetadata) song).getCustomTitle();
        }

        return song.getTitle();
    }

    public String getListeningAuthor(Player player) {
        Song song = getCurrentSong(player);
        if (song == null)
            return "N/A";

        if (song instanceof SongWithMetadata) {
            return ((SongWithMetadata) song).getCustomArtist();
        }

        return song.getAuthor();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        Location from = e.getFrom();
        WorldGuardHook wg = getWg();
        if (wg == null)
            return;

        for (RegionRadio radio : radios) {
            if (!radio.isLoaded())
                continue;

            if (to.getWorld() != radio.getWorld())
                continue;

            if (wg.isInRegion(to, radio.getWorld(), radio.config.getRegion()) &&
                !wg.isInRegion(from, radio.getWorld(), radio.config.getRegion())) {
                radio.addPlayer(e.getPlayer());
            } else if (!wg.isInRegion(to, radio.getWorld(), radio.config.getRegion()) &&
                    wg.isInRegion(from, radio.getWorld(), radio.config.getRegion())) {
                radio.removePlayer(e.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        onMove(e);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        for (RegionRadio radio : radios) {
            if (!radio.isLoaded())
                continue;

            if (radio.getPlayers().contains(e.getPlayer().getUniqueId())) {
                radio.removePlayer(e.getPlayer());
            }
        }
        plugin.getScheduler().runTaskLater(() -> {
            Location loc = e.getPlayer().getLocation();
            WorldGuardHook wg = getWg();
            if (wg == null)
                return;

            for (RegionRadio radio : radios) {
                if (!radio.isLoaded())
                    continue;

                if (loc.getWorld() != radio.getWorld())
                    continue;

                if (wg.isInRegion(loc, radio.getWorld(), radio.config.getRegion())) {
                    radio.addPlayer(e.getPlayer());
                }
            }
        }, e.getPlayer(), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Location loc = e.getPlayer().getLocation();
        WorldGuardHook wg = getWg();
        if (wg == null)
            return;

        for (RegionRadio radio : radios) {
            if (!radio.isLoaded())
                continue;

            if (loc.getWorld() != radio.getWorld())
                continue;

            if (wg.isInRegion(loc, radio.getWorld(), radio.config.getRegion())) {
                radio.addPlayer(e.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent e) {
        for (RegionRadio radio : radios) {
            if (!radio.isLoaded())
                continue;

            if (radio.getPlayers().contains(e.getPlayer().getUniqueId())) {
                radio.removePlayer(e.getPlayer());
            }
        }
    }

    @Override
    public void loadDefaultConfig() {
        config.set("regions.glasacht.enabled", false);
        config.set("regions.glasacht.world", "catacumbas");
        config.set("regions.glasacht.region", "all_glasacht");
        config.set("regions.glasacht.playlist", "glasacht_playlist");
        config.set("regions.glasacht.volume", 50);
        config.set("regions.glasacht.delay", 0);
    }

    public static class RegionRadioConfig extends RadioConfig {
        protected String region;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    public class RegionRadio extends Radio {
        private final RegionRadioConfig config;
        private final World world;
        private RegionSongPlayer player;

        protected RegionRadio(RegionRadioConfig radioConfig) {
            super(radioConfig);
            this.config = radioConfig;
            this.world = plugin.getServer().getWorld(config.getWorldName());
        }

        @Override
        public void applyPermissions(Set<UUID> set) {}

        @Override
        protected void createPlayer(Playlist playlist) {
            this.player = new RegionSongPlayer(playlist);
            this.player.setVolume((byte)this.volume);
            this.player.setRandom(this.random);
            this.player.setRepeatMode(RepeatMode.ALL);
            StereoMode mode = new StereoMode();
            mode.setFallbackChannelMode(new MonoStereoMode());
            this.player.setChannelMode(mode);
            if (this.repeat) {
                this.player.setRepeatMode(RepeatMode.ALL);
            }
            this.player.setEnable10Octave(NoteBlockMusicPlayer.getInstance().use10OctaveRange);
            this.player.setPlaying(this.enable);
        }

        public void addPlayer(Player p) {
            if (this.loaded) {
                this.players.add(p.getUniqueId());
                this.player.addPlayer(p);
            }
        }

        public void removePlayer(Player p) {
            if (this.loaded) {
                this.players.remove(p.getUniqueId());
                this.player.removePlayer(p);
            }
        }

        public World getWorld() {
            return world;
        }

        @Override
        public SongPlayer getSongPlayer() {
            return player;
        }

        public Set<UUID> getPlayers() {
            return this.players;
        }
    }

    public WorldGuardHook getWg() {
        if (wg == null)
            wg = plugin.getModule(WorldGuardHook.class);
        return wg;
    }

    public static class RegionSongPlayer extends RadioSongPlayer {
        public RegionSongPlayer(Playlist playlist) {
            super(playlist);
        }
    }

    @Override
    public void disable() {
        for (RegionRadio radio : radios) {
            radio.setEnable(false);
            radio.player.destroy();
        }
    }

    @Override
    public void enable() {
        ConfigurationSection section = config.getConfigurationSection("regions");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            RegionRadioConfig radioConfig = new RegionRadioConfig();
            radioConfig.setName(key);
            radioConfig.setDelay(section.getDouble("delay"));
            radioConfig.setEnable(section.getBoolean("enabled"));
            radioConfig.setPlaylistName(section.getString("playlist"));
            radioConfig.setVolume(section.getInt("volume"));
            radioConfig.setWorldName(section.getString("world"));
            radioConfig.setRegion(section.getString("region"));
            radioConfig.setShowActionBar(false);
            radioConfig.setHologramName("example_hologram");
            radioConfig.setHologramOPName("example_hologram");
            radioConfig.setRandom(false);
            radioConfig.setRepeat(true);

            RegionRadio radio = new RegionRadio(radioConfig);
            radios.add(radio);
        }
    }
}
