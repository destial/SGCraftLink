package xyz.destiall.sgcraftlink.items;

public class Tag {
    private final String name;
    private String display;

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

    public void setDisplay(String display) {
        this.display = display;
    }
}
