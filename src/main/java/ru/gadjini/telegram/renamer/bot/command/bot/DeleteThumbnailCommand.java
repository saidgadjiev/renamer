package ru.gadjini.telegram.renamer.bot.command.bot;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.bot.command.api.BotCommand;
import ru.gadjini.telegram.renamer.bot.command.api.NavigableBotCommand;
import ru.gadjini.telegram.renamer.bot.command.keyboard.rename.RenameState;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.domain.HasThumb;
import ru.gadjini.telegram.renamer.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.renamer.model.bot.api.object.Message;
import ru.gadjini.telegram.renamer.service.LocalisationService;
import ru.gadjini.telegram.renamer.service.UserService;
import ru.gadjini.telegram.renamer.service.command.CommandStateService;
import ru.gadjini.telegram.renamer.service.command.navigator.CommandNavigator;
import ru.gadjini.telegram.renamer.service.message.MessageService;

import java.util.Locale;

@Component
public class DeleteThumbnailCommand implements BotCommand {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private CommandNavigator commandNavigator;

    @Autowired
    public DeleteThumbnailCommand(CommandStateService commandStateService, @Qualifier("messagelimits") MessageService messageService,
                                  LocalisationService localisationService, UserService userService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Override
    public void processMessage(Message message) {
        String currentCommandName = getCurrentCommandName(message.getChatId());
        if (StringUtils.isNotBlank(currentCommandName)) {
            Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
            HasThumb state = getState(message.getChatId(), currentCommandName);

            if (state != null && state.getThumb() != null) {
                state.delThumb();
                commandStateService.setState(message.getChatId(), currentCommandName, state);
                messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_DELETED, locale)));
            } else {
                messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_NOT_FOUND, locale)));
            }
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.DEL_THUMBNAIL_COMMAND;
    }

    private HasThumb getState(long chatId, String commandName) {
        if (commandName.equals(CommandNames.RENAME_COMMAND_NAME)) {
            return commandStateService.getState(chatId, commandName, false, RenameState.class);
        }

        return null;
    }

    private String getCurrentCommandName(long chatId) {
        NavigableBotCommand currentCommand = commandNavigator.getCurrentCommand(chatId);

        if (currentCommand != null) {
            return currentCommand.getHistoryName();
        }

        return null;
    }
}
