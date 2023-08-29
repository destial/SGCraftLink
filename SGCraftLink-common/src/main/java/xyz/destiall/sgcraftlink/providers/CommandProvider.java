package xyz.destiall.sgcraftlink.providers;

public interface CommandProvider {
    void execute(PlayerProvider player, String[] args);
}
