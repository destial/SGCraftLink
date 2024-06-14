package xyz.destiall.sgcraftlink.modules.hooks.utils;

public final class DefaultFont extends Font {
    public DefaultFont() {
        super("default", null);
    }

    @Override
    public String format(String text, boolean checkChar) {
        return text;
    }
}
