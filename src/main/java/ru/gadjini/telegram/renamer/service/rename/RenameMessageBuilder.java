package ru.gadjini.telegram.renamer.service.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.progress.Lang;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.ProgressManager;

import java.util.Locale;

@Service
public class RenameMessageBuilder {

    private LocalisationService localisationService;

    private ProgressManager progressManager;

    @Autowired
    public RenameMessageBuilder(LocalisationService localisationService, ProgressManager progressManager) {
        this.localisationService = localisationService;
        this.progressManager = progressManager;
    }

    public String buildMessage(RenameQueueItem queueItem, RenameStep renameStep, Lang lang, Locale locale) {
        StringBuilder message = new StringBuilder();

        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_FILE_QUEUED, new Object[]{queueItem.getQueuePosition()}, locale)).append("\n\n");
        message.append(buildRenamingMessage(renameStep, queueItem.getSize(), lang, locale)).append("\n\n");
        message.append(localisationService.getMessage(MessagesProperties.MESSAGE_DONT_SEND_NEW_REQUEST, locale));

        return message.toString();
    }

    private String buildRenamingMessage(RenameStep renameStep, long fileSize, Lang lang, Locale locale) {
        String formatter = lang == Lang.JAVA ? "%s" : "{}";
        String percentage = lang == Lang.JAVA ? "%%" : "%";
        String iconCheck = localisationService.getMessage(MessagesProperties.ICON_CHECK, locale);
        boolean progress = isShowingProgress(fileSize, renameStep);
        String percentageFormatter = progress ? "(" + formatter + percentage + ")..." : "...";

        switch (renameStep) {
            case WAITING:
                return "<b>" + localisationService.getMessage(MessagesProperties.WAITING_STEP, locale) + "...</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case DOWNLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + " " + percentageFormatter + "...</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>\n" : "") +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b>\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case RENAMING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + " " + percentageFormatter + "...</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b>";
            case UPLOADING:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + " " + percentageFormatter + "...</b>\n" +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_ETA, locale) + " <b>" + formatter + "</b>\n" : "") +
                        (progress ? localisationService.getMessage(MessagesProperties.MESSAGE_SPEED, locale) + " <b>" + formatter + "</b>" : "");
            default:
                return "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_PROCESSING_STEP, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING_STEP_TWO, locale) + "</b> " + iconCheck + "\n" +
                        "<b>" + localisationService.getMessage(MessagesProperties.MESSAGE_UPLOADING_STEP, locale) + "</b> " + iconCheck;
        }
    }

    private boolean isShowingProgress(long fileSize, RenameStep conversionStep) {
        if (conversionStep == RenameStep.DOWNLOADING) {
            return progressManager.isShowingDownloadingProgress(fileSize);
        } else if (conversionStep == RenameStep.UPLOADING) {
            return progressManager.isShowingUploadingProgress(fileSize);
        }

        return false;
    }
}
