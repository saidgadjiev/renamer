package ru.gadjini.telegram.renamer.service.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.update.UpdateQueryStatusCommandMessageProvider;

import java.util.Locale;

@Service
public class RenameMessageBuilder implements UpdateQueryStatusCommandMessageProvider {

    private LocalisationService localisationService;

    @Autowired
    public RenameMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String buildMessage(RenameQueueItem queueItem, RenameStep renameStep, Locale locale) {
        StringBuilder message = new StringBuilder();

        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale)).append("\n\n");
        message.append(buildRenamingMessage(renameStep, locale)).append("\n\n");
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale));

        return message.toString();
    }

    private String buildRenamingMessage(RenameStep renameStep, Locale locale) {
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);

        switch (renameStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(MessagesProperties.WAITING_STEP, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case RENAMING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "...</b>";
            default:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }

    @Override
    public String getWaitingMessage(QueueItem queueItem, Locale locale) {
        return buildMessage((RenameQueueItem) queueItem, RenameStep.WAITING, locale);
    }
}
