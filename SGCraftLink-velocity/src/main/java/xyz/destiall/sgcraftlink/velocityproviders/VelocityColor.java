package xyz.destiall.sgcraftlink.velocityproviders;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.destiall.sgcraftlink.providers.ColorProvider;

public class VelocityColor extends ColorProvider {
    public VelocityColor() {
        ColorProvider.setProvider(this);
    }

    @Override
    public String color(String s) {
        return LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
    }
}
