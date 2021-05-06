package ru.gadjini.telegram.renamer.job;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.progress.ProgressBuilder;
import ru.gadjini.telegram.renamer.service.rename.RenameStep;
import ru.gadjini.telegram.renamer.service.thumb.ThumbService;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloader;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileUploadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorker;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorkerFactory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.io.File;

@Component
public class RenameQueueWorkerFactory implements QueueWorkerFactory<RenameQueueItem> {

    private FileDownloader fileManager;

    private FormatService formatService;

    private CommandStateService commandStateService;

    private ThumbService thumbService;

    private FileUploadService fileUploadService;

    private ProgressBuilder progressBuilder;

    @Autowired
    public RenameQueueWorkerFactory(FileDownloader fileManager, FormatService formatService,
                                    CommandStateService commandStateService, ThumbService thumbService,
                                    FileUploadService fileUploadService, ProgressBuilder progressBuilder) {
        this.fileManager = fileManager;
        this.formatService = formatService;
        this.commandStateService = commandStateService;
        this.thumbService = thumbService;
        this.fileUploadService = fileUploadService;
        this.progressBuilder = progressBuilder;
    }

    @Override
    public QueueWorker createWorker(RenameQueueItem queueItem) {
        return new RenameQueueWorker(queueItem);
    }

    private String createNewFileName(String fileName, String ext) {
        if (StringUtils.isNotBlank(ext)) {
            String withExt = FilenameUtils.getExtension(fileName);

            if (StringUtils.isBlank(withExt)) {
                return fileName + "." + ext;
            }
        }

        return fileName;
    }

    public final class RenameQueueWorker implements QueueWorker {

        private final Logger LOGGER = LoggerFactory.getLogger(RenameQueueWorker.class);

        private final RenameQueueItem queueItem;

        private volatile SmartTempFile thumbFile;

        private RenameQueueWorker(RenameQueueItem queueItem) {
            this.queueItem = queueItem;
        }

        @Override
        public void execute() {
            String size = MemoryUtils.humanReadableByteCount(queueItem.getFile().getSize());
            LOGGER.debug("Start({}, {}, {})", queueItem.getUserId(), size, queueItem.getFile().getFileId());

            String ext = formatService.getExt(queueItem.getFile().getFileName(), queueItem.getFile().getMimeType());
            String finalFileName = createNewFileName(queueItem.getNewFileName(), ext);

            SmartTempFile file = queueItem.getDownloadedFile();

            if (queueItem.getThumb() != null) {
                thumbFile = thumbService.convertToThumb(queueItem.getUserId(), queueItem.getThumb().getFileId(), queueItem.getThumb().getSize());

                commandStateService.deleteState(queueItem.getUserId(), RenameCommandNames.SET_THUMBNAIL_COMMAND);
            } else if (StringUtils.isNotBlank(queueItem.getFile().getThumb())) {
                thumbFile = new SmartTempFile(
                        new File(fileManager.downloadFileByFileId(queueItem.getFile().getThumb(),
                                queueItem.getFile().getThumbSize(), false)),
                        false
                );
            }
            SendDocument.SendDocumentBuilder documentBuilder = SendDocument.builder().chatId(String.valueOf(queueItem.getUserId()))
                    .document(new InputFile(file.getFile(), finalFileName));
            if (thumbFile != null) {
                documentBuilder.thumb(new InputFile(thumbFile.getFile()));
            }
            fileUploadService.createUpload(queueItem.getUserId(), SendDocument.PATH, documentBuilder.replyToMessageId(queueItem.getReplyToMessageId()).build(),
                    progressBuilder.progress(queueItem.getUserId(), queueItem, RenameStep.UPLOADING, RenameStep.COMPLETED), queueItem.getId());

            LOGGER.debug("Finish({}, {}, {})", queueItem.getUserId(), size, queueItem.getNewFileName());
        }

        @Override
        public void cancel(boolean canceledByUser) {
            if (canceledByUser) {
                SmartTempFile downloadedFile = queueItem.getDownloadedFile();
                if (downloadedFile != null) {
                    downloadedFile.smartDelete();
                }
            }
            if (thumbFile != null) {
                thumbFile.smartDelete();
            }
        }
    }
}
