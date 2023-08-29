package xyz.destiall.sgcraftlink.providers;

import java.util.List;

public interface TabProvider {
    List<String> tab(PlayerProvider sender, String[] args);
}
