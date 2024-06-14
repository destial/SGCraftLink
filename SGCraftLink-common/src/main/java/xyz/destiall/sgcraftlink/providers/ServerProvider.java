package xyz.destiall.sgcraftlink.providers;

import java.util.Collection;

public interface ServerProvider {
    String getName();
    Collection<PlayerProvider> getPlayers();
}
