package ru.gadjini.telegram.renamer.command.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.service.keyboard.RenamerReplyKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.method.send.SendMessage;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.Message;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.command.navigator.CommandNavigator;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;
import java.util.Objects;

@Component
public class SetThumbnailCommand implements BotCommand, NavigableBotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetThumbnailCommand.class);

    private CommandStateService commandStateService;

    private CommandNavigator commandNavigator;

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private RenamerReplyKeyboardService replyKeyboardService;

    private MessageMediaService fileService;

    @Autowired
    public SetThumbnailCommand(CommandStateService commandStateService,
                               @Qualifier("messageLimits") MessageService messageService, LocalisationService localisationService,
                               UserService userService, @Qualifier("curr") RenamerReplyKeyboardService replyKeyboardService,
                               MessageMediaService fileService) {
        this.commandStateService = commandStateService;
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.replyKeyboardService = replyKeyboardService;
        this.fileService = fileService;
    }

    @Autowired
    public void setCommandNavigator(CommandNavigator commandNavigator) {
        this.commandNavigator = commandNavigator;
    }

    @Override
    public boolean acceptNonCommandMessage(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }

    @Override
    public void processMessage(Message message, String[] params) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        messageService.sendMessage(new SendMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_SEND_THUMB, locale))
                .setReplyMarkup(replyKeyboardService.cancel(message.getChatId(), locale)));
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        MessageMedia any2AnyFile = fileService.getMedia(message, locale);

        if (any2AnyFile != null) {
            validate(message.getFrom().getId(), any2AnyFile, locale);
            commandStateService.setState(message.getChatId(), CommandNames.SET_THUMBNAIL_COMMAND, any2AnyFile);
            CommandNavigator.SilentPop silentPop = commandNavigator.silentPop(message.getChatId());
            messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_ADDED, locale) +
                    "\n\n" + silentPop.getMessage())
                    .setReplyMarkup(silentPop.getReplyKeyboardMarkup()));
        }
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.SET_THUMBNAIL_COMMAND;
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.SET_THUMBNAIL_COMMAND;
    }

    @Override
    public boolean setPrevCommand(long chatId, String prevCommand) {
        return true;
    }

    private void validate(int userId, MessageMedia any2AnyFile, Locale locale) {
        if (any2AnyFile.getFormat() == null) {
            LOGGER.debug("Null format({}, {})", userId, any2AnyFile);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_INVALID_FILE, locale));
        }
        if (!Objects.equals(any2AnyFile.getFormat().getCategory(), FormatCategory.IMAGES)) {
            LOGGER.debug("Non image thumb({}, {})", userId, any2AnyFile.getFormat());
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_THUMB_INVALID_FILE, locale));
        }
    }
}
