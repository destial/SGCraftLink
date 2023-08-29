package xyz.destiall.sgcraftlink.velocityproviders;

import net.kyori.adventure.text.event.ClickEvent;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;

public class VelocityClickEvent extends ComponentClickProvider<ClickEvent> {
    static {
        ComponentClickProvider.setProvider(VelocityClickEvent::new);
    }

    public VelocityClickEvent(ClickEvent click) {
        super(click);
    }

    public VelocityClickEvent() {
        super(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, ""));
    }

    @Override
    public void setAction(Action action) {
        click = ClickEvent.clickEvent(ClickEvent.Action.valueOf(action.name()), click.value());
    }

    @Override
    public void setText(String text) {
        click = ClickEvent.clickEvent(click.action(), text);
    }
}
