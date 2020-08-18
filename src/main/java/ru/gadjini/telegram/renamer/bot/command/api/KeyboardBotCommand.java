package ru.gadjini.telegram.renamer.bot.command.api;

import ru.gadjini.telegram.renamer.model.bot.api.object.Message;

public interface KeyboardBotCommand extends MyBotCommand {

    boolean canHandle(long chatId, String command);
    
    default boolean isTextCommand() {
        return false;
    }

    boolean processMessage(Message message, String text);
}
