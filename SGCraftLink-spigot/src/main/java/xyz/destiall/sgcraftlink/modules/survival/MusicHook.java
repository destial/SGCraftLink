package xyz.destiall.sgcraftlink.modules.survival;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import cz.studiodamage.NoteBlockMusicPlayer.NoteBlockMusicPlayer;
import cz.studiodamage.NoteBlockMusicPlayer.objects.SongWithMetadata;
import cz.studiodamage.NoteBlockMusicPlayer.player.WorldRadio;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class MusicHook extends Module {
    public MusicHook(SGCraftLinkSpigot plugin) {
        super(plugin);
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

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}
}
