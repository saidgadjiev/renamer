package ru.gadjini.telegram.renamer.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.Redis;
import ru.gadjini.telegram.smart.bot.commons.dao.command.keyboard.ReplyKeyboardDao;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardHolderService;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;

import java.util.Locale;

@Service
@KeyboardHolder
public class CurrReplyKeyboard implements RenamerReplyKeyboardService, ReplyKeyboardHolderService {

    private ReplyKeyboardDao replyKeyboardDao;

    private RenamerReplyKeyboardService keyboardService;

    @Autowired
    public CurrReplyKeyboard(@Redis ReplyKeyboardDao replyKeyboardDao,
                             @Qualifier("keyboard") RenamerReplyKeyboardService keyboardService) {
        this.replyKeyboardDao = replyKeyboardDao;
        this.keyboardService = keyboardService;
    }

    @Override
    public ReplyKeyboard mainMenuKeyboard(long chatId, Locale locale) {
        return keyboardService.mainMenuKeyboard(chatId, locale);
    }

    @Override
    public ReplyKeyboardMarkup goBackKeyboard(long l, Locale locale) {
        return setCurrentKeyboard(l, keyboardService.goBackKeyboard(l, locale));
    }

    @Override
    public ReplyKeyboardMarkup smartFileFeatureKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.smartFileFeatureKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.languageKeyboard(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup goBack(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.goBack(chatId, locale));
    }

    @Override
    public ReplyKeyboardMarkup cancel(long chatId, Locale locale) {
        return setCurrentKeyboard(chatId, keyboardService.cancel(chatId, locale));
    }

    @Override
    public ReplyKeyboardRemove removeKeyboard(long chatId) {
        ReplyKeyboardRemove replyKeyboardRemove = keyboardService.removeKeyboard(chatId);
        setCurrentKeyboard(chatId, ReplyKeyboardService.replyKeyboardMarkup());

        return replyKeyboardRemove;
    }

    public ReplyKeyboardMarkup getCurrentReplyKeyboard(long chatId) {
        return replyKeyboardDao.get(chatId);
    }

    private ReplyKeyboardMarkup setCurrentKeyboard(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        replyKeyboardDao.store(chatId, replyKeyboardMarkup);

        return replyKeyboardMarkup;
    }
}
