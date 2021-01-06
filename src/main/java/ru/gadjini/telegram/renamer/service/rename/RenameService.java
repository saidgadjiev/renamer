package ru.gadjini.telegram.renamer.service.rename;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.progress.ProgressBuilder;
import ru.gadjini.telegram.renamer.service.queue.RenameQueueService;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.WorkQueueService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
public class RenameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private FormatService formatService;

    private MessageService messageService;

    private FileDownloadService fileDownloadService;

    private MediaMessageService mediaMessageService;

    private WorkQueueService queueService;

    private RenameQueueService renameQueueService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private RenameMessageBuilder renameMessageBuilder;

    private ProgressBuilder progressBuilder;

    @Autowired
    public RenameService(FormatService formatService,
                         @Qualifier("messageLimits") MessageService messageService,
                         FileDownloadService fileDownloadService, @Qualifier("mediaLimits") MediaMessageService mediaMessageService,
                         WorkQueueService queueService, RenameQueueService renameQueueService,
                         InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService,
                         RenameMessageBuilder renameMessageBuilder,
                         ProgressBuilder progressBuilder) {
        this.formatService = formatService;
        this.messageService = messageService;
        this.fileDownloadService = fileDownloadService;
        this.mediaMessageService = mediaMessageService;
        this.queueService = queueService;
        this.renameQueueService = renameQueueService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.renameMessageBuilder = renameMessageBuilder;
        this.progressBuilder = progressBuilder;
    }

    public void rename(int userId, RenameState renameState, String newFileName) {
        MessageMedia thumb = commandStateService.getState(userId, RenameCommandNames.SET_THUMBNAIL_COMMAND, false, MessageMedia.class);
        if (isTheSameFileName(renameState.getFile().getFileName(), renameState.getFile().getMimeType(), newFileName)
                && thumb == null) {
            mediaMessageService.sendFile(userId, renameState.getFile().getFileId());
            LOGGER.debug("Same file name({}, {}, {})", userId, renameState.getFile().getFileId(), renameState.getFile().getFileName());
            return;
        }
        RenameQueueItem item = renameQueueService.createItem(userId, renameState, thumb, newFileName);
        sendStartRenamingMessage(item, message -> {
            item.setProgressMessageId(message.getMessageId());
            queueService.setProgressMessageId(item.getId(), message.getMessageId());
            createDownload(item);
        });
    }

    private void createDownload(RenameQueueItem queueItem) {
        queueItem.getFile().setProgress(progressBuilder.progress(queueItem.getUserId(), queueItem, RenameStep.DOWNLOADING, RenameStep.RENAMING));
        fileDownloadService.createDownload(queueItem.getFile(), queueItem.getId(), queueItem.getUserId());
    }

    private void sendStartRenamingMessage(RenameQueueItem queueItem, Consumer<Message> callback) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        String message = renameMessageBuilder.buildMessage(queueItem, RenameStep.WAITING, locale);
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(queueItem.getUserId())).text(message)
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getRenameWaitingKeyboard(queueItem.getId(), locale)).build(), callback);
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
}
