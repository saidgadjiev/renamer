package ru.gadjini.telegram.renamer.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.ReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartReplyKeyboardService;

import java.util.Locale;

@Service
@Qualifier("keyboard")
public class ReplyKeyboardServiceImpl implements RenamerReplyKeyboardService {

    private LocalisationService localisationService;

    private SmartReplyKeyboardService smartReplyKeyboardService;

    @Autowired
    public ReplyKeyboardServiceImpl(LocalisationService localisationService, SmartReplyKeyboardService smartReplyKeyboardService) {
        this.localisationService = localisationService;
        this.smartReplyKeyboardService = smartReplyKeyboardService;
    }

    @Override
    public ReplyKeyboard mainMenuKeyboard(long chatId, Locale locale) {
        return removeKeyboard(chatId);
    }

    @Override
    public ReplyKeyboardMarkup goBackKeyboard(long l, Locale locale) {
        return smartReplyKeyboardService.goBackKeyboard(l, locale);
    }

    @Override
    public ReplyKeyboardMarkup smartFileFeatureKeyboard(long chatId, Locale locale) {
        return smartReplyKeyboardService.smartFileFeatureKeyboard(locale);
    }

    @Override
    public ReplyKeyboardMarkup languageKeyboard(long chatId, Locale locale) {
        return smartReplyKeyboardService.languageKeyboard(locale);
    }

    @Override
    public ReplyKeyboardMarkup goBack(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardService.replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(ReplyKeyboardService.keyboardRow(localisationService.getMessage(MessagesProperties.GO_BACK_COMMAND_NAME, locale)));

        return replyKeyboardMarkup;
    }

    @Override
    public ReplyKeyboardMarkup cancel(long chatId, Locale locale) {
        ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardService.replyKeyboardMarkup();

        replyKeyboardMarkup.getKeyboard().add(ReplyKeyboardService.keyboardRow(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale)));

        return replyKeyboardMarkup;
    }
}
