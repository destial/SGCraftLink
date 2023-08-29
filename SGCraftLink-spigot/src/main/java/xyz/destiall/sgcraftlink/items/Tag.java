package xyz.destiall.sgcraftlink.items;

public class Tag {
    private final String name;
    private final String display;

    public Tag(String name, String display) {
        this.name = name;
        this.display = display;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return display;
    }
}
