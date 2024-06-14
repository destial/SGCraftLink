package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

public class BungeeTextComponent extends ComponentProvider<TextComponent> {
    static {
        ComponentProvider.setProvider(BungeeTextComponent::new);
    }

    public BungeeTextComponent(TextComponent component) {
        super(component);
    }

    public BungeeTextComponent() {
        super(new TextComponent());
    }

    @Override
    public void setMessage(String message) {
        component.setText(message);
    }

    @Override
    public void addExtra(ComponentProvider<?> other) {
        component.addExtra((TextComponent) other.getComponent());
    }

    @Override
    public void setColor(ColorProvider.ChatColor color) {
        component.setColor(ChatColor.of(color.name()));
    }

    @Override
    public void setHover(ComponentHoverProvider<?> hover) {
        component.setHoverEvent((HoverEvent) hover.getHover());
    }

    @Override
    public void setClick(ComponentClickProvider<?> click) {
        component.setClickEvent((ClickEvent) click.getClick());
    }
}
