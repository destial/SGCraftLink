package xyz.destiall.sgcraftlink.providers;

import net.dv8tion.jda.api.entities.Role;

public class Playtime {
    private final String name;
    private final long days;
    private final long messages;
    private final Role role;

    public Playtime(String name, long days, long messages, Role role) {
        this.name = name;
        this.days = days;
        this.role = role;
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public long getDays() {
        return days;
    }

    public Role getRole() {
        return role;
    }

    public long getMessages() {
        return messages;
    }
}
