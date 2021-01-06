package ru.gadjini.telegram.renamer.service.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.rename.RenameMessageBuilder;
import ru.gadjini.telegram.renamer.service.rename.RenameStep;
import ru.gadjini.telegram.smart.bot.commons.model.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;

import java.util.Locale;

@Component
public class ProgressBuilder {

    private UserService userService;

    private RenameMessageBuilder renameMessageBuilder;

    private InlineKeyboardService inlineKeyboardService;

    @Autowired
    public ProgressBuilder(UserService userService, RenameMessageBuilder renameMessageBuilder, InlineKeyboardService inlineKeyboardService) {
        this.userService = userService;
        this.renameMessageBuilder = renameMessageBuilder;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    public Progress progress(long chatId, RenameQueueItem queueItem, RenameStep renameStep, RenameStep nextStep) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        Progress progress = new Progress();
        progress.setChatId(chatId);
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(renameMessageBuilder.buildMessage(queueItem, renameStep, locale));
        if (nextStep != null) {
            String completionMessage = renameMessageBuilder.buildMessage(queueItem, nextStep, locale);
            if (nextStep == RenameStep.RENAMING) {
                progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50));
            } else {
                progress.setAfterProgressCompletionMessage(String.format(completionMessage, 0));
            }
            if (nextStep != RenameStep.COMPLETED) {
                progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(queueItem.getId(), locale));
            }
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(queueItem.getId(), locale));

        return progress;
    }

}
