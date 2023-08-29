package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.chat.ClickEvent;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;

public class BungeeClickEvent extends ComponentClickProvider<ClickEvent> {
    static {
        ComponentClickProvider.setProvider(BungeeClickEvent::new);
    }

    public BungeeClickEvent(ClickEvent click) {
        super(click);
    }

    public BungeeClickEvent() {
        super(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ""));
    }

    @Override
    public void setAction(Action action) {
        ClickEvent.Action ac = ClickEvent.Action.valueOf(action.name());
        click = new ClickEvent(ac, click.getValue());
    }

    @Override
    public void setText(String text) {
        click = new ClickEvent(click.getAction(), text);
    }
}
