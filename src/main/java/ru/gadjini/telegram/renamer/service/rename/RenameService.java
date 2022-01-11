package ru.gadjini.telegram.renamer.service.rename;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.event.RenameItemCreatedEvent;
import ru.gadjini.telegram.renamer.service.keyboard.InlineKeyboardService;
import ru.gadjini.telegram.renamer.service.queue.RenameQueueService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Service
public class RenameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameService.class);

    private FormatService formatService;

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private RenameQueueService renameQueueService;

    private InlineKeyboardService inlineKeyboardService;

    private CommandStateService commandStateService;

    private UserService userService;

    private RenameMessageBuilder renameMessageBuilder;

    @Autowired
    public RenameService(FormatService formatService,
                         @TgMessageLimitsControl MessageService messageService,
                         @Qualifier("mediaLimits") MediaMessageService mediaMessageService,
                         RenameQueueService renameQueueService,
                         InlineKeyboardService inlineKeyboardService,
                         CommandStateService commandStateService, UserService userService,
                         RenameMessageBuilder renameMessageBuilder) {
        this.formatService = formatService;
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.renameQueueService = renameQueueService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.commandStateService = commandStateService;
        this.userService = userService;
        this.renameMessageBuilder = renameMessageBuilder;
    }

    @Transactional
    public void rename(long userId, RenameState renameState, String newFileName) {
        MessageMedia thumb = commandStateService.getState(userId, RenameCommandNames.SET_THUMBNAIL_COMMAND, false, MessageMedia.class);
        if (isTheSameFileName(renameState.getFile().getFileName(), renameState.getFile().getMimeType(), newFileName)
                && thumb == null) {
            mediaMessageService.sendFile(userId, renameState.getFile().getFileId());
            LOGGER.debug("Same file name({}, {}, {})", userId, renameState.getFile().getFileId(), renameState.getFile().getFileName());
            return;
        }
        RenameQueueItem item = renameQueueService.createItem(userId, renameState, thumb, newFileName);
        sendStartRenamingMessage(item);
    }

    private void sendStartRenamingMessage(RenameQueueItem queueItem) {
        Locale locale = userService.getLocaleOrDefault(queueItem.getUserId());

        String message = renameMessageBuilder.buildMessage(queueItem, RenameStep.WAITING, locale);
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(queueItem.getUserId())).text(message)
                .parseMode(ParseMode.HTML)
                .replyMarkup(inlineKeyboardService.getRenameWaitingKeyboard(queueItem.getId(), locale)).build(),
                new RenameItemCreatedEvent(queueItem.getId()));
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
