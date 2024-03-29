package xyz.destiall.sgcraftlink.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.jetbrains.annotations.NotNull;

public class ButtonCollector extends Collector<ButtonCollector, Message, ButtonInteraction, ButtonInteractionEvent> {
    private final Message message;

    public ButtonCollector(Message message) {
        super(message.getChannel());
        this.message = message;
        this.listener = new ListenerAdapter() {
            int amount = 0;
            @Override
            public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
                if (limit != -1 && amount > limit)
                    return;

                if (event.getMessageId().equalsIgnoreCase(ButtonCollector.this.message.getId()) && !event.getUser().isBot() && (filter == null || filter.test(event))) {
                    ButtonCollector.this.collected.add(event.getInteraction());
                    if (collect != null) {
                        try {
                            collect.accept(event.getInteraction());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (limit != -1 && ++amount >= limit) {
                        stop();
                    }
                    return;
                }

                event.reply("You do not have permission!").setEphemeral(true).queue();
            }
        };
    }
}
