package xyz.destiall.sgcraftlink.velocityproviders;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

public class VelocityHoverEvent extends ComponentHoverProvider<HoverEvent<?>> {

    public VelocityHoverEvent() {
        this(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("")));
    }

    public VelocityHoverEvent(HoverEvent<?> hover) {
        super(hover);
    }

    @Override
    public void setAction(Action action) {
        switch (action) {
            case SHOW_TEXT: hover = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, (TextComponent) hover.value()); break;
            default: break;
        }
    }

    @Override
    public void setText(String text) {
        hover = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text(text));
    }

    @Override
    public void setText(ComponentProvider<?> component) {
        hover = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, (TextComponent) component.getComponent());
    }
}
