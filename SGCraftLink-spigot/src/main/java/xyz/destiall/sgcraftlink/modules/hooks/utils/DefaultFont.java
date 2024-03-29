package xyz.destiall.sgcraftlink.modules.hooks.utils;

public class DefaultFont extends Font {
    public DefaultFont() {
        super(null, null);
    }

    @Override
    public String format(String text) {
        return text;
    }
}
