package xyz.destiall.sgcraftlink.providers;

import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.awt.*;

public abstract class ColorProvider {
    private static ColorProvider provider;
    public static void setProvider(ColorProvider provider) {
        ColorProvider.provider = provider;
    }

    public static String of(String s) {
        return provider.color(s);
    }

    public static ComponentProvider<?> ofComponent(String s) {
        return provider.colorComponent(s);
    }

    public abstract String color(String s);

    public ComponentProvider<?> colorComponent(String s) {
        return ComponentProvider.of(color(s));
    }

    public enum ChatColor {
        BLACK(new Color(0x000000)),
        DARK_BLUE(new Color(0x0000AA)),
        DARK_GREEN(new Color( 0x00AA00 )),
        DARK_AQUA(new Color( 0x00AAAA )),
        DARK_RED(new Color( 0xAA0000 )),
        DARK_PURPLE(new Color( 0xAA00AA )),
        GOLD(new Color( 0xFFAA00 )),
        GRAY(new Color( 0xAAAAAA )),
        DARK_GRAY(new Color( 0x555555 )),
        BLUE(new Color( 0x5555FF )),
        GREEN(new Color( 0x55FF55 )),
        AQUA(new Color( 0x55FFFF )),
        RED(new Color( 0xFF5555 )),
        LIGHT_PURPLE(new Color( 0xFF55FF )),
        YELLOW(new Color( 0xFFFF55 )),
        WHITE(new Color( 0xFFFFFF )),

        ;

        private final Color color;
        ChatColor(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }
}
