package ru.gadjini.telegram.renamer.service.keyboard;

import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.buttons.KeyboardRow;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public interface RenamerReplyKeyboardService extends ReplyKeyboardService {

    ReplyKeyboardMarkup goBack(long chatId, Locale locale);

    ReplyKeyboardMarkup cancel(long chatId, Locale locale);

    ReplyKeyboardRemove removeKeyboard(long chatId);

    default KeyboardRow keyboardRow(String... buttons) {
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.addAll(Arrays.asList(buttons));

        return keyboardRow;
    }

    default ReplyKeyboardMarkup replyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setKeyboard(new ArrayList<>());
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }
}
