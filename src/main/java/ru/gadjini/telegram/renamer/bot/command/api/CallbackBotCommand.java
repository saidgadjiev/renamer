package ru.gadjini.telegram.renamer.bot.command.api;

import ru.gadjini.telegram.renamer.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.renamer.request.RequestParams;

public interface CallbackBotCommand extends MyBotCommand {

    String getName();

    void processMessage(CallbackQuery callbackQuery, RequestParams requestParams);
}
