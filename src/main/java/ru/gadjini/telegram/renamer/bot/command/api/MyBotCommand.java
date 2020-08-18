package ru.gadjini.telegram.renamer.bot.command.api;

import ru.gadjini.telegram.renamer.model.bot.api.object.Message;

public interface MyBotCommand {

    default void processNonCommandUpdate(Message message, String text) {
    }

    default boolean accept(Message message) {
        return message.hasText();
    }
}
