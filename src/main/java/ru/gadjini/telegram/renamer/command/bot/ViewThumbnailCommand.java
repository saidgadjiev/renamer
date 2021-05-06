package ru.gadjini.telegram.renamer.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.service.thumb.ThumbService;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.SendFileResult;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

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
    public ViewThumbnailCommand(@TgMessageLimitsControl MessageService messageService,
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
        MessageMedia thumbnail = commandStateService.getState(message.getChatId(), RenameCommandNames.SET_THUMBNAIL_COMMAND, false, MessageMedia.class);
        if (thumbnail != null) {
            if (StringUtils.isNotBlank(thumbnail.getCachedFileId())) {
                mediaMessageService.sendPhoto(new SendPhoto(String.valueOf(message.getChatId()), new InputFile(thumbnail.getCachedFileId())));
            } else {
                executor.execute(() -> {
                    SmartTempFile tempFile = thumbService.convertToThumb(message.getChatId(), thumbnail.getFileId(), thumbnail.getFileSize());
                    try {
                        SendFileResult sendFileResult = mediaMessageService.sendPhoto(new SendPhoto(String.valueOf(message.getChatId()), new InputFile(tempFile.getFile())));
                        thumbnail.setCachedFileId(sendFileResult.getFileId());
                        commandStateService.setState(message.getChatId(), RenameCommandNames.SET_THUMBNAIL_COMMAND, thumbnail);
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
        return RenameCommandNames.VIEW_THUMBNAIL_COMMAND;
    }

    private void thumbNotFound(Message message) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessage(String.valueOf(message.getChatId()), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
    }
}
