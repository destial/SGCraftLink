package xyz.destiall.sgcraftlink.modules.lobby;

import org.bukkit.ChatColor;

public final class NPCGlow {
    private final int id;
    private final ChatColor color;
    private boolean glowing;

    public NPCGlow(int id, ChatColor color, boolean glowing) {
        this.id = id;
        this.color = color;
        this.glowing = glowing;
    }

    public int getId() {
        return id;
    }

    public ChatColor getColor() {
        return color;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }
}
