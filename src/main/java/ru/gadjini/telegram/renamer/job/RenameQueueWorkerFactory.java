package ru.gadjini.telegram.renamer.job;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.progress.Lang;
import ru.gadjini.telegram.renamer.service.rename.RenameMessageBuilder;
import ru.gadjini.telegram.renamer.service.rename.RenameStep;
import ru.gadjini.telegram.renamer.service.thumb.ThumbService;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorker;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueWorkerFactory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.Locale;

@Component
public class RenameQueueWorkerFactory implements QueueWorkerFactory<RenameQueueItem> {

    private FileManager fileManager;

    private TempFileService tempFileService;

    private FormatService formatService;

    private MediaMessageService mediaMessageService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private ThumbService thumbService;

    private RenameMessageBuilder renameMessageBuilder;

    @Autowired
    public RenameQueueWorkerFactory(FileManager fileManager, TempFileService tempFileService, FormatService formatService,
                                    @Qualifier("forceMedia") MediaMessageService mediaMessageService,
                                    LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                                    CommandStateService commandStateService, UserService userService, ThumbService thumbService,
                                    RenameMessageBuilder renameMessageBuilder) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.mediaMessageService = mediaMessageService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.renameMessageBuilder = renameMessageBuilder;
    }

    @Override
    public QueueWorker createWorker(RenameQueueItem queueItem) {
        return new RenameQueueWorker(queueItem);
    }

    private Progress progress(long chatId, RenameQueueItem queueItem, RenameStep renameStep, RenameStep nextStep) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(chatId);
        progress.setProgressMessageId(queueItem.getProgressMessageId());
        progress.setProgressMessage(renameMessageBuilder.buildMessage(queueItem, renameStep, Lang.PYTHON, locale));
        if (nextStep != null) {
            String calculated = localisationService.getMessage(MessagesProperties.MESSAGE_CALCULATED, locale);
            String completionMessage = renameMessageBuilder.buildMessage(queueItem, nextStep, Lang.JAVA, locale);
            String seconds = localisationService.getMessage(MessagesProperties.SECOND_PART, locale);
            if (nextStep == RenameStep.RENAMING) {
                progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50, "7 " + seconds));
            } else {
                progress.setAfterProgressCompletionMessage(String.format(completionMessage, 0, calculated, calculated));
            }
            if (nextStep != RenameStep.COMPLETED) {
                progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(queueItem.getId(), locale));
            }
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(queueItem.getId(), locale));

        return progress;
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

        private static final String TAG = "rename";

        private final RenameQueueItem queueItem;

        private volatile SmartTempFile file;

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

            file = tempFileService.createTempFile(queueItem.getUserId(), queueItem.getFile().getFileId(), TAG, ext);
            fileManager.forceDownloadFileByFileId(queueItem.getFile().getFileId(), queueItem.getFile().getSize(),
                    progress(queueItem.getUserId(), queueItem, RenameStep.DOWNLOADING, RenameStep.RENAMING), file);

            if (queueItem.getThumb() != null) {
                thumbFile = thumbService.convertToThumb(queueItem.getUserId(), queueItem.getThumb().getFileId(), queueItem.getThumb().getSize(), queueItem.getThumb().getFileName(), queueItem.getThumb().getMimeType());
                commandStateService.deleteState(queueItem.getUserId(), RenameCommandNames.SET_THUMBNAIL_COMMAND);
            } else if (StringUtils.isNotBlank(queueItem.getFile().getThumb())) {
                thumbFile = tempFileService.createTempFile(queueItem.getUserId(), queueItem.getFile().getFileId(), TAG, Format.JPG.getExt());
                fileManager.forceDownloadFileByFileId(queueItem.getFile().getThumb(), 1, thumbFile);
            }
            mediaMessageService.sendDocument(new SendDocument((long) queueItem.getUserId(), finalFileName, file.getFile())
                    .setProgress(progress(queueItem.getUserId(), queueItem, RenameStep.UPLOADING, RenameStep.COMPLETED))
                    .setThumb(thumbFile != null ? thumbFile.getAbsolutePath() : null)
                    .setReplyToMessageId(queueItem.getReplyToMessageId()));

            LOGGER.debug("Finish({}, {}, {})", queueItem.getUserId(), size, queueItem.getNewFileName());
        }

        @Override
        public void cancel() {
            if (!fileManager.cancelDownloading(queueItem.getFile().getFileId()) && file != null) {
                file.smartDelete();
            }
            if (file != null && !fileManager.cancelUploading(file.getAbsolutePath())) {
                file.smartDelete();
            }
            if (!fileManager.cancelDownloading(queueItem.getThumb().getFileId()) && thumbFile != null) {
                thumbFile.smartDelete();
            }
        }

        @Override
        public void finish() {
            if (file != null) {
                file.smartDelete();
            }
            if (thumbFile != null) {
                thumbFile.smartDelete();
            }
        }
    }
}
