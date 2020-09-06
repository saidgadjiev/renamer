package ru.gadjini.telegram.renamer.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.Any2AnyFile;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendPhoto;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.renamer.service.thumb.ThumbService;

import java.util.Locale;

@Component
public class ViewThumbnailCommand implements BotCommand {

    private MessageService messageService;

    private MediaMessageService mediaMessageService;

    private LocalisationService localisationService;

    private UserService userService;

    private ThumbService thumbService;

    private CommandStateService commandStateService;

    private ThreadPoolTaskExecutor executor;

    @Autowired
    public ViewThumbnailCommand(@Qualifier("messageLimits") MessageService messageService,
                                @Qualifier("mediaLimits") MediaMessageService mediaMessageService, LocalisationService localisationService,
                                UserService userService, ThumbService thumbService, CommandStateService commandStateService,
                                @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.messageService = messageService;
        this.mediaMessageService = mediaMessageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.thumbService = thumbService;
        this.commandStateService = commandStateService;
        this.executor = executor;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Any2AnyFile thumbnail = commandStateService.getState(message.getChatId(), CommandNames.SET_THUMBNAIL_COMMAND, false, Any2AnyFile.class);
        if (thumbnail != null) {
            if (StringUtils.isNotBlank(thumbnail.getCachedFileId())) {
                mediaMessageService.sendPhoto(new SendPhoto(message.getChatId(), thumbnail.getCachedFileId()));
            } else {
                executor.execute(() -> {
                    SmartTempFile tempFile = thumbService.convertToThumb(message.getChatId(), thumbnail.getFileId(), thumbnail.getFileSize(), thumbnail.getFileName(), thumbnail.getMimeType());
                    try {
                        SendFileResult sendFileResult = mediaMessageService.sendPhoto(new SendPhoto(message.getChatId(), tempFile.getFile()));
                        thumbnail.setCachedFileId(sendFileResult.getFileId());
                        commandStateService.setState(message.getChatId(), CommandNames.SET_THUMBNAIL_COMMAND, thumbnail);
                    } finally {
                        tempFile.smartDelete();
                    }
                });
            }
        } else {
            thumbNotFound(message);
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.VIEW_THUMBNAIL_COMMAND;
    }

    private void thumbNotFound(Message message) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
    }
}
