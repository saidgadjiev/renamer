package ru.gadjini.telegram.renamer.job;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.progress.Lang;
import ru.gadjini.telegram.renamer.service.queue.RenameQueueService;
import ru.gadjini.telegram.renamer.service.rename.RenameMessageBuilder;
import ru.gadjini.telegram.renamer.service.rename.RenameStep;
import ru.gadjini.telegram.renamer.service.thumb.ThumbService;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.exception.DownloadCanceledException;
import ru.gadjini.telegram.smart.bot.commons.exception.DownloadingException;
import ru.gadjini.telegram.smart.bot.commons.exception.FloodWaitException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileWorkObject;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

@Component
public class RenamerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenamerJob.class);

    private SmartExecutorService executor;

    private FileManager fileManager;

    private TempFileService tempFileService;

    private FormatService formatService;

    private MediaMessageService mediaMessageService;

    private RenameQueueService queueService;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private ThumbService thumbService;

    private MessageService messageService;

    private RenameMessageBuilder renameMessageBuilder;

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public RenamerJob(FileManager fileManager, TempFileService tempFileService, FormatService formatService,
                      @Qualifier("forceMedia") MediaMessageService mediaMessageService, RenameQueueService renameQueueService,
                      LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                      CommandStateService commandStateService, UserService userService, ThumbService thumbService,
                      @Qualifier("messageLimits") MessageService messageService, RenameMessageBuilder renameMessageBuilder, FileLimitProperties fileLimitProperties) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.mediaMessageService = mediaMessageService;
        this.queueService = renameQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.messageService = messageService;
        this.renameMessageBuilder = renameMessageBuilder;
        this.fileLimitProperties = fileLimitProperties;
    }

    @Autowired
    public void setExecutor(SmartExecutorService executor) {
        this.executor = executor;
    }

    @PostConstruct
    public void init() {
        queueService.resetProcessing();
    }

    @Scheduled(fixedDelay = 1000)
    public void pushTasks() {
        ThreadPoolExecutor heavyExecutor = executor.getExecutor(SmartExecutorService.JobWeight.HEAVY);
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<RenameQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.HEAVY, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push heavy jobs({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new RenameTask(queueItem)));
        }
        ThreadPoolExecutor lightExecutor = executor.getExecutor(SmartExecutorService.JobWeight.LIGHT);
        if (lightExecutor.getActiveCount() < lightExecutor.getCorePoolSize()) {
            Collection<RenameQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, lightExecutor.getCorePoolSize() - lightExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push light jobs({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new RenameTask(queueItem)));
        }
        if (heavyExecutor.getActiveCount() < heavyExecutor.getCorePoolSize()) {
            Collection<RenameQueueItem> items = queueService.poll(SmartExecutorService.JobWeight.LIGHT, heavyExecutor.getCorePoolSize() - heavyExecutor.getActiveCount());

            if (items.size() > 0) {
                LOGGER.debug("Push light jobs to heavy threads({})", items.size());
            }
            items.forEach(queueItem -> executor.execute(new RenameTask(queueItem), SmartExecutorService.JobWeight.HEAVY));
        }
    }

    public void cancel(long chatId, int messageId, String queryId, int jobId) {
        if (!queueService.exists(jobId)) {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_ITEM_NOT_FOUND, userService.getLocaleOrDefault((int) chatId)),
                    true
            ));
        } else {
            messageService.sendAnswerCallbackQuery(new AnswerCallbackQuery(
                    queryId,
                    localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))
            ));
            if (!executor.cancelAndComplete(jobId, true)) {
                RenameQueueItem renameQueueItem = queueService.deleteWithReturning(jobId);

                if (renameQueueItem != null) {
                    fileManager.fileWorkObject(renameQueueItem.getId(), renameQueueItem.getFile().getSize()).stop();
                }
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    public void removeAndCancelCurrentTask(long chatId) {
        RenameQueueItem renameQueueItem = queueService.deleteByUserId((int) chatId);
        if (renameQueueItem != null && !executor.cancelAndComplete(renameQueueItem.getId(), true)) {
            fileManager.fileWorkObject(chatId, renameQueueItem.getFile().getSize()).stop();
        }
    }

    public void rejectRenameTask(SmartExecutorService.Job job) {
        queueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({}, {})", job.getId(), job.getWeight());
    }

    public void shutdown() {
        executor.shutdown();
    }

    private Progress progress(long chatId, int jobId, int processMessageId, RenameStep renameStep, RenameStep nextStep) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        Progress progress = new Progress();
        progress.setLocale(locale.getLanguage());
        progress.setChatId(chatId);
        progress.setProgressMessageId(processMessageId);
        progress.setProgressMessage(renameMessageBuilder.buildRenamingMessage(renameStep, locale, Lang.PYTHON));
        if (nextStep != null) {
            String calculated = localisationService.getMessage(MessagesProperties.MESSAGE_CALCULATED, locale);
            String completionMessage = renameMessageBuilder.buildRenamingMessage(nextStep, locale, Lang.JAVA);
            String seconds = localisationService.getMessage(MessagesProperties.SECOND_PART, locale);
            if (nextStep == RenameStep.RENAMING) {
                progress.setAfterProgressCompletionMessage(String.format(completionMessage, 50, "7 " + seconds));
            } else {
                progress.setAfterProgressCompletionMessage(String.format(completionMessage, 0, calculated, calculated));
            }
            if (nextStep != RenameStep.COMPLETED) {
                progress.setAfterProgressCompletionReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale));
            }
        }
        progress.setProgressReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale));

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

    public final class RenameTask implements SmartExecutorService.Job {

        private final Logger LOGGER = LoggerFactory.getLogger(RenameTask.class);

        private static final String TAG = "rename";

        private int jobId;
        private final int userId;
        private final String fileName;
        private final String newFileName;
        private final String mimeType;
        private final String fileId;
        private long fileSize;
        private final int replyToMessageId;
        private final int progressMessageId;
        private volatile Supplier<Boolean> checker;
        private volatile boolean canceledByUser;
        private volatile SmartTempFile file;
        private volatile SmartTempFile thumbFile;
        private TgFile userThumb;
        private String thumb;
        private FileWorkObject fileWorkObject;

        private RenameTask(RenameQueueItem queueItem) {
            this.jobId = queueItem.getId();
            this.userId = queueItem.getUserId();
            this.fileName = queueItem.getFile().getFileName();
            this.newFileName = queueItem.getNewFileName();
            this.mimeType = queueItem.getFile().getMimeType();
            this.fileId = queueItem.getFile().getFileId();
            this.fileSize = queueItem.getFile().getSize();
            this.replyToMessageId = queueItem.getReplyToMessageId();
            this.thumb = queueItem.getFile().getThumb();
            this.userThumb = queueItem.getThumb();
            this.fileWorkObject = fileManager.fileWorkObject(userId, fileSize);
            this.progressMessageId = queueItem.getProgressMessageId();
        }

        @Override
        public void execute() {
            fileWorkObject.start();
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {})", userId, size, fileId);

            boolean success = false;
            try {
                String ext = formatService.getExt(fileName, mimeType);
                String finalFileName = createNewFileName(newFileName, ext);

                file = tempFileService.createTempFile(userId, fileId, TAG, ext);
                fileManager.downloadFileByFileId(fileId, fileSize, progress(userId, jobId, progressMessageId, RenameStep.DOWNLOADING, RenameStep.RENAMING), file);

                if (userThumb != null) {
                    thumbFile = thumbService.convertToThumb(userId, userThumb.getFileId(), userThumb.getSize(), userThumb.getFileName(), userThumb.getMimeType());
                    commandStateService.deleteState(userId, CommandNames.SET_THUMBNAIL_COMMAND);
                } else if (StringUtils.isNotBlank(thumb)) {
                    thumbFile = tempFileService.createTempFile(userId, fileId, TAG, Format.JPG.getExt());
                    fileManager.downloadFileByFileId(thumb, 1, thumbFile);
                }
                mediaMessageService.sendDocument(new SendDocument((long) userId, finalFileName, file.getFile())
                        .setProgress(progress(userId, jobId, progressMessageId, RenameStep.UPLOADING, RenameStep.COMPLETED))
                        .setThumb(thumbFile != null ? thumbFile.getAbsolutePath() : null)
                        .setReplyToMessageId(replyToMessageId));

                success = true;
                LOGGER.debug("Finish({}, {}, {})", userId, size, newFileName);
            } catch (Throwable e) {
                if (checker == null || !checker.get() || ExceptionUtils.indexOfThrowable(e, DownloadCanceledException.class) == -1) {
                    int downloadingExceptionIndexOf = ExceptionUtils.indexOfThrowable(e, DownloadingException.class);
                    int floodWaitExceptionIndexOf = ExceptionUtils.indexOfThrowable(e, FloodWaitException.class);

                    if (downloadingExceptionIndexOf != -1 || floodWaitExceptionIndexOf != -1) {
                        LOGGER.error(e.getMessage());
                        queueService.setWaiting(jobId);
                        updateProgressMessageAfterFloodWait(userId, getProgressMessageId(), jobId);
                    } else {
                        throw e;
                    }
                }
            } finally {
                if (checker == null || !checker.get()) {
                    executor.complete(jobId);
                    if (success) {
                        fileWorkObject.stop();
                        queueService.delete(jobId);
                    }
                    if (file != null) {
                        file.smartDelete();
                    }
                    if (thumbFile != null) {
                        thumbFile.smartDelete();
                    }
                }
            }
        }

        @Override
        public int getId() {
            return jobId;
        }

        @Override
        public void cancel() {
            if (!fileManager.cancelDownloading(fileId) && file != null) {
                file.smartDelete();
            }
            if (file != null && !fileManager.cancelUploading(file.getAbsolutePath())) {
                file.smartDelete();
            }
            if (!fileManager.cancelDownloading(thumb) && thumbFile != null) {
                thumbFile.smartDelete();
            }
            if (canceledByUser) {
                queueService.delete(jobId);
                fileWorkObject.stop();
                LOGGER.debug("Canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public Supplier<Boolean> getCancelChecker() {
            return null;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > fileLimitProperties.getLightFileMaxWeight() ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }

        @Override
        public long getChatId() {
            return userId;
        }

        @Override
        public int getProgressMessageId() {
            return progressMessageId;
        }

        private void updateProgressMessageAfterFloodWait(long chatId, int progressMessageId, int id) {
            Locale locale = userService.getLocaleOrDefault(id);
            String message = localisationService.getMessage(MessagesProperties.MESSAGE_AWAITING_PROCESSING, locale);

            messageService.editMessage(new EditMessageText(chatId, progressMessageId, message)
                    .setNoLogging(true)
                    .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(id, locale)));
        }
    }
}
