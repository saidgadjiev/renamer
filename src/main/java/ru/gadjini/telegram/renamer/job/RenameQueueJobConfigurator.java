package ru.gadjini.telegram.renamer.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.rename.RenameMessageBuilder;
import ru.gadjini.telegram.renamer.service.rename.RenameStep;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueJobConfigurator;

import java.util.Locale;

@Component
public class RenameQueueJobConfigurator implements QueueJobConfigurator<RenameQueueItem> {

    private RenameMessageBuilder renameMessageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public RenameQueueJobConfigurator(RenameMessageBuilder messageBuilder, InlineKeyboardService inlineKeyboardService) {
        this.renameMessageBuilder = messageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public String getWaitingMessage(RenameQueueItem queueItem, Locale locale) {
        return renameMessageBuilder.buildMessage(queueItem, RenameStep.WAITING, locale);
    }

    @Override
    public InlineKeyboardMarkup getWaitingKeyboard(RenameQueueItem queueItem, Locale locale) {
        return inlineKeyboardService.getRenameProcessingKeyboard(queueItem.getId(), locale);
    }
}
