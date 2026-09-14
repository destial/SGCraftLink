package xyz.destiall.sgcraftlink.velocityproviders;

import net.kyori.adventure.text.event.ClickEvent;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;

public class VelocityClickEvent extends ComponentClickProvider<ClickEvent> {
    public VelocityClickEvent() {
        this(ClickEvent.clickEvent(ClickEvent.Action.RunCommand.RUN_COMMAND, ClickEvent.Payload.string("")));
    }

    public VelocityClickEvent(ClickEvent<?> click) {
        super(click);
    }

    @Override
    public void setEvent(Action action, String text) {
        switch (action) {
            case OPEN_URL: {
                if (ClickEvent.Action.OPEN_URL.supports(click.payload())) {
                    click = ClickEvent.openUrl(text);
                }
                break;
            }
            case RUN_COMMAND: {
                if (ClickEvent.Action.RUN_COMMAND.supports(click.payload())) {
                    click = ClickEvent.runCommand(text);
                }
                break;
            }
            case SUGGEST_COMMAND: {
                if (ClickEvent.Action.SUGGEST_COMMAND.supports(click.payload())) {
                    click = ClickEvent.suggestCommand(text);
                }
                break;
            }
            case COPY_TO_CLIPBOARD: {
                if (ClickEvent.Action.COPY_TO_CLIPBOARD.supports(click.payload())) {
                    click = ClickEvent.copyToClipboard(text);
                }
                break;
            }
            case CHANGE_PAGE: {
                if (ClickEvent.Action.CHANGE_PAGE.supports(click.payload())) {
                    click = ClickEvent.changePage(Integer.parseInt(text));
                }
                break;
            }
            case OPEN_FILE: {
                if (ClickEvent.Action.OPEN_FILE.supports(click.payload())) {
                    click = ClickEvent.openFile(text);
                }
                break;
            }
            default: break;
        }
    }

    @Override
    public void setAction(Action action) {
        setEvent(action, ((ClickEvent.Payload.Text)click.payload()).value());
    }

    @Override
    public void setText(String text) {
        click = ClickEvent.clickEvent(click.action(), ClickEvent.Payload.string(text));
    }
}
