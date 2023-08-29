package xyz.destiall.sgcraftlink.velocityproviders;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

public class VelocityTextComponent extends ComponentProvider<TextComponent> {
    static {
        ComponentProvider.setProvider(VelocityTextComponent::new);
    }

    public VelocityTextComponent(TextComponent component) {
        super(component);
    }

    public VelocityTextComponent() {
        super(Component.text(""));
    }

    @Override
    public void setMessage(String message) {
        component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    @Override
    public void setColor(ColorProvider.ChatColor color) {
        component = component.color(TextColor.color(color.getColor().getRGB()));
    }

    @Override
    public void setHover(ComponentHoverProvider<?> hover) {
        component = component.hoverEvent((HoverEvent<?>) hover.getHover());
    }

    @Override
    public void setClick(ComponentClickProvider<?> click) {
        component = component.clickEvent((ClickEvent) click.getClick());
    }
}
