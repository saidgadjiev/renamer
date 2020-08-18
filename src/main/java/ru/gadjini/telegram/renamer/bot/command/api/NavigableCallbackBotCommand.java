package ru.gadjini.telegram.renamer.bot.command.api;

import ru.gadjini.telegram.renamer.model.TgMessage;
import ru.gadjini.telegram.renamer.model.bot.api.object.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.renamer.request.RequestParams;

public interface NavigableCallbackBotCommand extends MyBotCommand {

    String getName();

    default void restore(TgMessage tgMessage, ReplyKeyboard replyKeyboard, RequestParams requestParams) {

    }

    default void leave(long chatId) {

    }

    default boolean isAcquireKeyboard() {
        return false;
    }
}
