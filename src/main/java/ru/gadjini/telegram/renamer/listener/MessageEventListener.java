package ru.gadjini.telegram.renamer.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.event.RenameItemCreatedEvent;
import ru.gadjini.telegram.renamer.service.progress.ProgressBuilder;
import ru.gadjini.telegram.renamer.service.rename.RenameStep;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageEvent;
import ru.gadjini.telegram.smart.bot.commons.service.queue.WorkQueueService;

@Component
public class MessageEventListener {

    private FileDownloadService fileDownloadService;

    private ProgressBuilder progressBuilder;

    private WorkQueueService queueService;

    @Autowired
    public MessageEventListener(FileDownloadService fileDownloadService, ProgressBuilder progressBuilder,
                                WorkQueueService queueService) {
        this.fileDownloadService = fileDownloadService;
        this.progressBuilder = progressBuilder;
        this.queueService = queueService;
    }

    @EventListener(MessageEvent.class)
    public void onEvent(MessageEvent messageEvent) {
        if (messageEvent.getEvent() instanceof RenameItemCreatedEvent) {
            RenameItemCreatedEvent createConversionEvent = (RenameItemCreatedEvent) messageEvent.getEvent();
            Message message = (Message) messageEvent.getSendResult();

            RenameQueueItem queueItem = (RenameQueueItem) queueService.getById(createConversionEvent.getItemId());

            queueItem.setProgressMessageId(message.getMessageId());
            queueService.setProgressMessageId(queueItem.getId(), message.getMessageId());
            createDownload(queueItem);
        }
    }

    private void createDownload(RenameQueueItem queueItem) {
        queueItem.getFile().setProgress(progressBuilder.progress(queueItem.getUserId(), queueItem, RenameStep.DOWNLOADING, RenameStep.RENAMING));
        fileDownloadService.createDownload(queueItem.getFile(), queueItem.getId(), queueItem.getUserId());
    }
}
