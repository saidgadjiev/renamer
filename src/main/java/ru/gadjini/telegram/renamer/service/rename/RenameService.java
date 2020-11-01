package ru.gadjini.telegram.renamer.service.rename;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.progress.Lang;
import ru.gadjini.telegram.renamer.service.queue.RenameQueueService;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileManager;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.QueueService;

import java.util.Locale;
import java.util.function.Consumer;

@Service
public class RenameService {

    private static final String TAG = "rn";

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private FileManager fileManager;

    private FormatService formatService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private QueueService queueService;

    private RenameQueueService renameQueueService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private RenameMessageBuilder messageBuilder;

    @Autowired
    public RenameService(FileManager fileManager, FormatService formatService,
                         @Qualifier("messageLimits") MessageService messageService,
                         @Qualifier("mediaLimits") MediaMessageService mediaMessageService, QueueService queueService,
                         RenameQueueService renameQueueService,
                         InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService,
                         RenameMessageBuilder messageBuilder) {
        this.fileManager = fileManager;
        this.formatService = formatService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.queueService = queueService;
        this.renameQueueService = renameQueueService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.messageBuilder = messageBuilder;
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
            fileManager.setInputFilePending(userId, renameState.getReplyMessageId(), renameState.getFile().getFileId(), renameState.getFile().getFileSize(), TAG);
        });
    }

    private void sendStartRenamingMessage(RenameQueueItem queueItem, Consumer<Message> callback) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        String message = messageBuilder.buildMessage(queueItem, RenameStep.WAITING, Lang.JAVA, locale);
        messageService.sendMessage(new HtmlMessage((long) queueItem.getUserId(), message)
                .setReplyMarkup(inlineKeyboardService.getRenameProcessingKeyboard(queueItem.getId(), locale)), callback);
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
