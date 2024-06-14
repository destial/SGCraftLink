package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

public class BungeeHoverEvent extends ComponentHoverProvider<HoverEvent> {
    static {
        ComponentHoverProvider.setProvider(BungeeHoverEvent::new);
    }

    public BungeeHoverEvent(HoverEvent hover) {
        super(hover);
    }

    public BungeeHoverEvent() {
        super(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("")));
    }

    @Override
    public void setAction(Action action) {
        HoverEvent.Action ac = HoverEvent.Action.valueOf(action.name());
        hover = new HoverEvent(ac, hover.getContents());
    }

    @Override
    public void setText(String text) {
        hover = new HoverEvent(hover.getAction(), new Text(text));
    }

    @Override
    public void setText(ComponentProvider<?> component) {
        hover = new HoverEvent(hover.getAction(), new Text(new TextComponent[] {(TextComponent) component.getComponent()}));
    }
}
