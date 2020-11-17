package ru.gadjini.telegram.renamer.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
public class DeleteThumbnailCommand implements BotCommand {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public DeleteThumbnailCommand(CommandStateService commandStateService, @Qualifier("messageLimits") MessageService messageService,
                                  LocalisationService localisationService, UserService userService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        MessageMedia thumb = commandStateService.getState(message.getChatId(), RenameCommandNames.SET_THUMBNAIL_COMMAND, false, MessageMedia.class);
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());

        if (thumb != null) {
            commandStateService.deleteState(message.getChatId(), RenameCommandNames.SET_THUMBNAIL_COMMAND);
            messageService.sendMessage(new SendMessage(String.valueOf(message.getChatId()), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_DELETED, locale)));
        } else {
            messageService.sendMessage(new SendMessage(String.valueOf(message.getChatId()), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return RenameCommandNames.DEL_THUMBNAIL_COMMAND;
    }
}
