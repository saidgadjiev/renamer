package ru.gadjini.telegram.renamer.dao.command.keyboard;

import ru.gadjini.telegram.renamer.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;

public interface ReplyKeyboardDao {
    void store(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup);

    ReplyKeyboardMarkup get(long chatId);
}
