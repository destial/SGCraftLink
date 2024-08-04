package xyz.destiall.sgcraftlink.items.utils;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonInteraction;
import org.jetbrains.annotations.NotNull;

public class ButtonCollector extends Collector<ButtonCollector, Message, ButtonInteraction, ButtonClickEvent> {
    private final Message message;

    public ButtonCollector(Message message) {
        super(message.getChannel());
        this.message = message;
        this.listener = new ListenerAdapter() {
            int amount = 0;

            @Override
            public void onButtonClick(@NotNull ButtonClickEvent event) {
                if (limit != -1 && amount > limit)
                    return;

                if (!event.getMessageId().equalsIgnoreCase(ButtonCollector.this.message.getId()))
                    return;

                if (!event.getUser().isBot() && (filter == null || filter.test(event))) {
                    ButtonCollector.this.collected.add(event.getInteraction());
                    if (collect != null) {
                        collect.accept(event.getInteraction());
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
