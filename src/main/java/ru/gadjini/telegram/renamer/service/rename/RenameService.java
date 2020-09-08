package ru.gadjini.telegram.renamer.service.rename;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.domain.TgFile;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.progress.Lang;
import ru.gadjini.telegram.renamer.service.queue.RenameQueueService;
import ru.gadjini.telegram.renamer.service.thumb.ThumbService;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendDocument;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.updatemessages.EditMessageText;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Progress;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.ProgressManager;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class RenameService {

    private static final String TAG = "rn";

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private FileManager fileManager;

    private TempFileService tempFileService;

    private FormatService formatService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private RenameQueueService renameQueueService;

    private SmartExecutorService executor;

    private LocalisationService localisationService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private ThumbService thumbService;

    private RenameMessageBuilder renameMessageBuilder;

    private ProgressManager progressManager;

    @Autowired
    public RenameService(FileManager fileManager, TempFileService tempFileService, FormatService formatService,
                         @Qualifier("messageLimits") MessageService messageService,
                         @Qualifier("mediaLimits") MediaMessageService mediaMessageService, RenameQueueService renameQueueService,
                         LocalisationService localisationService, InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService, ThumbService thumbService,
                         RenameMessageBuilder renameMessageBuilder, ProgressManager progressManager) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.renameQueueService = renameQueueService;
        this.localisationService = localisationService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.renameMessageBuilder = renameMessageBuilder;
        this.progressManager = progressManager;
    }

    @PostConstruct
    public void init() {
        renameQueueService.resetProcessing();
        pushTasks(SmartExecutorService.JobWeight.LIGHT);
        pushTasks(SmartExecutorService.JobWeight.HEAVY);
    }

    @Autowired
    public void setExecutor(@Qualifier("renameTaskExecutor") SmartExecutorService executor) {
        this.executor = executor;
    }

    public void rejectRenameTask(SmartExecutorService.Job job) {
        renameQueueService.setWaiting(job.getId());
        LOGGER.debug("Rejected({}, {})", job.getId(), job.getWeight());
    }

    public RenameTask getTask(SmartExecutorService.JobWeight weight) {
        synchronized (this) {
            RenameQueueItem peek = renameQueueService.poll(weight);

            if (peek != null) {
                return new RenameTask(peek);
            }
            return null;
        }
    }

    public void rename(int userId, RenameState renameState, String newFileName) {
        MessageMedia thumb = commandStateService.getState(userId, CommandNames.SET_THUMBNAIL_COMMAND, false, MessageMedia.class);
        if (isTheSameFileName(renameState.getFile().getFileName(), renameState.getFile().getMimeType(), newFileName)
                && thumb == null) {
            mediaMessageService.sendFile(userId, renameState.getFile().getFileId());
            LOGGER.debug("Same file name({}, {}, {})", userId, renameState.getFile().getFileId(), renameState.getFile().getFileName());
            return;
        }
        RenameQueueItem item = renameQueueService.createProcessingItem(userId, renameState, thumb, newFileName);
        sendStartRenamingMessage(item.getId(), userId, renameState.getFile().getFileSize(), message -> {
            item.setProgressMessageId(message.getMessageId());
            renameQueueService.setProgressMessageId(item.getId(), message.getMessageId());
            fileManager.setInputFilePending(userId, renameState.getReplyMessageId(), renameState.getFile().getFileId(), renameState.getFile().getFileSize(), TAG);
            executor.execute(new RenameTask(item));
        });
    }

    public void removeAndCancelCurrentTask(long chatId) {
        RenameQueueItem renameQueueItem = renameQueueService.deleteByUserId((int) chatId);
        if (renameQueueItem != null && !executor.cancelAndComplete(renameQueueItem.getId(), true)) {
            fileManager.fileWorkObject(chatId, renameQueueItem.getFile().getSize()).stop();
        }
    }

    public void cancel(long chatId, int messageId, String queryId, int jobId) {
        if (!renameQueueService.exists(jobId)) {
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
                RenameQueueItem renameQueueItem = renameQueueService.deleteWithReturning(jobId);

                if (renameQueueItem != null) {
                    fileManager.fileWorkObject(renameQueueItem.getId(), renameQueueItem.getFile().getSize()).stop();
                }
            }
        }
        messageService.editMessage(new EditMessageText(
                chatId, messageId, localisationService.getMessage(MessagesProperties.MESSAGE_QUERY_CANCELED, userService.getLocaleOrDefault((int) chatId))));
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void pushTasks(SmartExecutorService.JobWeight jobWeight) {
        List<RenameQueueItem> tasks = renameQueueService.poll(jobWeight, 1);
        for (RenameQueueItem item : tasks) {
            executor.execute(new RenameTask(item));
        }
    }

    private void sendStartRenamingMessage(int jobId, int userId, long fileSize, Consumer<Message> callback) {
        Locale locale = userService.getLocaleOrDefault(userId);
        if (progressManager.isShowingDownloadingProgress(fileSize)) {
            String message = localisationService.getMessage(MessagesProperties.MESSAGE_AWAITING_PROCESSING, locale);
            messageService.sendMessage(new SendMessage((long) userId, message)
                    .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale)), callback);
        } else {
            String message = localisationService.getMessage(MessagesProperties.MESSAGE_RENAMING, locale);
            messageService.sendMessage(new SendMessage((long) userId, message)
                    .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(jobId, locale)), callback);
        }
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

    private boolean isTheSameFileName(String fileName, String mimeType, String newFileName) {
        String ext = formatService.getExt(fileName, mimeType);

        String finalFileName = createNewFileName(newFileName, ext);

        return finalFileName.equals(fileName);
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
        public void run() {
            fileWorkObject.start();
            String size = MemoryUtils.humanReadableByteCount(fileSize);
            LOGGER.debug("Start({}, {}, {})", userId, size, fileId);

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

                LOGGER.debug("Finish({}, {}, {})", userId, size, newFileName);
            } catch (Exception ex) {
                if (checker == null || !checker.get()) {
                    LOGGER.error(ex.getMessage(), ex);
                    messageService.sendErrorMessage(userId, userService.getLocaleOrDefault(userId));
                }
            } finally {
                if (checker == null || !checker.get()) {
                    executor.complete(jobId);
                    renameQueueService.delete(jobId);
                    if (file != null) {
                        file.smartDelete();
                    }
                    if (thumbFile != null) {
                        thumbFile.smartDelete();
                    }
                    fileWorkObject.stop();
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
                renameQueueService.delete(jobId);
                fileWorkObject.stop();
                LOGGER.debug("Canceled by user({}, {})", userId, MemoryUtils.humanReadableByteCount(fileSize));
            }
        }

        @Override
        public void setCancelChecker(Supplier<Boolean> checker) {
            this.checker = checker;
        }

        @Override
        public void setCanceledByUser(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
        }

        @Override
        public SmartExecutorService.JobWeight getWeight() {
            return fileSize > MemoryUtils.MB_100 ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
        }
    }
}
