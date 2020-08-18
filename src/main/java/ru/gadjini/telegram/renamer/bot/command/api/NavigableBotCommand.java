package ru.gadjini.telegram.renamer.bot.command.api;

import ru.gadjini.telegram.renamer.model.TgMessage;
import ru.gadjini.telegram.renamer.model.bot.api.object.replykeyboard.ReplyKeyboard;

public interface NavigableBotCommand extends MyBotCommand {

    String getParentCommandName(long chatId);

    String getHistoryName();

    default void restore(TgMessage message) {

    }

    default ReplyKeyboard getKeyboard(long chatId) {
        throw new UnsupportedOperationException();
    }

    default String getMessage(long chatId) {
        throw new UnsupportedOperationException();
    }

    default void leave(long chatId) {

    }

    default boolean setPrevCommand(long chatId, String prevCommand) {
        return false;
    }
}
