package ru.gadjini.telegram.renamer.bot.command.keyboard.rename;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.bot.command.api.BotCommand;
import ru.gadjini.telegram.renamer.bot.command.api.KeyboardBotCommand;
import ru.gadjini.telegram.renamer.bot.command.api.NavigableBotCommand;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.exception.UserException;
import ru.gadjini.telegram.renamer.model.Any2AnyFile;
import ru.gadjini.telegram.renamer.model.TgMessage;
import ru.gadjini.telegram.renamer.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.renamer.model.bot.api.object.Message;
import ru.gadjini.telegram.renamer.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;
import ru.gadjini.telegram.renamer.service.FileService;
import ru.gadjini.telegram.renamer.service.LocalisationService;
import ru.gadjini.telegram.renamer.service.rename.RenameService;
import ru.gadjini.telegram.renamer.service.UserService;
import ru.gadjini.telegram.renamer.service.command.CommandStateService;
import ru.gadjini.telegram.renamer.service.keyboard.ReplyKeyboardService;
import ru.gadjini.telegram.renamer.service.message.MessageService;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class RenameCommand implements KeyboardBotCommand, NavigableBotCommand, BotCommand {

    private Set<String> names = new HashSet<>();

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private ReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final RenameService renameService;

    private FileService fileService;

    @Autowired
    public RenameCommand(LocalisationService localisationService, CommandStateService commandStateService,
                         @Qualifier("messagelimits") MessageService messageService, @Qualifier("curr") ReplyKeyboardService replyKeyboardService,
                         UserService userService, RenameService renameService, FileService fileService) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.renameService = renameService;
        this.fileService = fileService;
        for (Locale locale : localisationService.getSupportedLocales()) {
            this.names.add(localisationService.getMessage(MessagesProperties.RENAME_COMMAND_NAME, locale));
        }
    }

    @Override
    public boolean canHandle(long chatId, String command) {
        return names.contains(command);
    }

    @Override
    public boolean accept(Message message) {
        return true;
    }

    @Override
    public void processMessage(Message message) {
        processMessage(message, null);
    }

    @Override
    public String getCommandIdentifier() {
        return CommandNames.RENAME_COMMAND_NAME;
    }

    @Override
    public boolean processMessage(Message message, String text) {
        processMessage0(message.getChatId(), message.getFrom().getId());
        commandStateService.setState(message.getChatId(), CommandNames.RENAME_COMMAND_NAME, new RenameState());

        return true;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(new HtmlMessage(chatId, localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale))
                .setReplyMarkup(replyKeyboardService.goBack(chatId, locale)));
    }

    @Override
    public String getParentCommandName(long chatId) {
        return CommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return CommandNames.RENAME_COMMAND_NAME;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        Any2AnyFile any2AnyFile = fileService.getFile(message, locale);

        if (any2AnyFile != null) {
            RenameState renameState = initState(message, any2AnyFile);

            messageService.sendMessage(new HtmlMessage(message.getChatId(), localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale)));
            renameService.removeAndCancelCurrentTasks(message.getChatId());
            commandStateService.setState(message.getChatId(), getHistoryName(), renameState);
        } else if (message.hasText()) {
            RenameState renameState = commandStateService.getState(message.getChatId(), getHistoryName(), true, RenameState.class);
            if (renameState.getFile() == null) {
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale));
            }
            renameService.rename(message.getFrom().getId(), renameState, text);
        }
    }

    @Override
    public void leave(long chatId) {
        renameService.leave(chatId);
    }

    @Override
    public void restore(TgMessage message) {
        RenameState renameState = commandStateService.getState(message.getChatId(), CommandNames.RENAME_COMMAND_NAME, true, RenameState.class);
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        String msg = localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale);
        if (renameState.getFile() != null) {
            msg = localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale);
        }
        messageService.sendMessage(new HtmlMessage(message.getChatId(), msg)
                .setReplyMarkup(replyKeyboardService.goBack(message.getChatId(), locale)));
    }

    @Override
    public ReplyKeyboardMarkup getKeyboard(long chatId) {
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        return replyKeyboardService.goBack(chatId, locale);
    }

    @Override
    public String getMessage(long chatId) {
        RenameState renameState = commandStateService.getState(chatId, CommandNames.RENAME_COMMAND_NAME, true, RenameState.class);
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        String msg = localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale);
        if (renameState.getFile() != null) {
            msg = localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale);
        }

        return msg;
    }

    private RenameState initState(Message message, Any2AnyFile any2AnyFile) {
        RenameState renameState = commandStateService.getState(message.getChatId(), CommandNames.RENAME_COMMAND_NAME, true, RenameState.class);
        renameState.setReplyMessageId(message.getMessageId());
        renameState.setFile(any2AnyFile);

        commandStateService.setState(message.getChatId(), CommandNames.RENAME_COMMAND_NAME, renameState);

        return renameState;
    }
}
