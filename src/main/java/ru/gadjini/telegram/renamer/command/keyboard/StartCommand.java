package ru.gadjini.telegram.renamer.command.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.service.keyboard.RenamerReplyKeyboardService;
import ru.gadjini.telegram.renamer.service.rename.RenameService;
import ru.gadjini.telegram.smart.bot.commons.annotation.CommandStart;
import ru.gadjini.telegram.smart.bot.commons.annotation.KeyboardHolder;
import ru.gadjini.telegram.smart.bot.commons.annotation.TgMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.command.api.NavigableBotCommand;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.job.WorkQueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;

import java.util.Locale;

@Component
@CommandStart
public class StartCommand implements NavigableBotCommand, BotCommand {

    private CommandStateService commandStateService;

    private MessageService messageService;

    private LocalisationService localisationService;

    private RenamerReplyKeyboardService replyKeyboardService;

    private UserService userService;

    private final RenameService renameService;

    private MessageMediaService fileService;

    private WorkQueueJob queueJob;

    @Autowired
    public StartCommand(LocalisationService localisationService, CommandStateService commandStateService,
                        @TgMessageLimitsControl MessageService messageService, @KeyboardHolder RenamerReplyKeyboardService replyKeyboardService,
                        UserService userService, RenameService renameService, MessageMediaService fileService, WorkQueueJob queueJob) {
        this.commandStateService = commandStateService;
        this.localisationService = localisationService;
        this.messageService = messageService;
        this.replyKeyboardService = replyKeyboardService;
        this.userService = userService;
        this.renameService = renameService;
        this.fileService = fileService;
        this.queueJob = queueJob;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        processMessage0(message.getChatId(), message.getFrom().getId());
    }

    @Override
    public String getCommandIdentifier() {
        return RenameCommandNames.START_COMMAND;
    }

    private void processMessage0(long chatId, int userId) {
        Locale locale = userService.getLocaleOrDefault(userId);
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(chatId))
                .text(localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale))
                .parseMode(ParseMode.HTML)
                .replyMarkup(replyKeyboardService.removeKeyboard(chatId)).build());
    }

    @Override
    public String getParentCommandName(long chatId) {
        return RenameCommandNames.START_COMMAND;
    }

    @Override
    public String getHistoryName() {
        return RenameCommandNames.START_COMMAND;
    }

    @Override
    public void processNonCommandUpdate(Message message, String text) {
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        MessageMedia any2AnyFile = fileService.getMedia(message, locale);

        if (any2AnyFile != null) {
            RenameState renameState = initState(message, any2AnyFile);

            messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                    .text(localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale))
                    .parseMode(ParseMode.HTML).build());
            queueJob.cancelCurrentTasks(message.getChatId());
            commandStateService.setState(message.getChatId(), getHistoryName(), renameState);
        } else if (message.hasText()) {
            RenameState renameState = commandStateService.getState(message.getChatId(), getHistoryName(), true, RenameState.class);
            if (renameState.getFile() == null) {
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale));
            }
            renameService.rename(message.getFrom().getId(), renameState, text);
            commandStateService.deleteState(message.getChatId(), RenameCommandNames.START_COMMAND);
        }
    }

    @Override
    public void restore(TgMessage message) {
        RenameState renameState = commandStateService.getState(message.getChatId(), RenameCommandNames.START_COMMAND, false, RenameState.class);
        Locale locale = userService.getLocaleOrDefault(message.getUser().getId());
        String msg = localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale);
        if (renameState != null) {
            msg = localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale);
        }
        messageService.sendMessage(SendMessage.builder().chatId(String.valueOf(message.getChatId()))
                .parseMode(ParseMode.HTML)
                .text(msg).replyMarkup(replyKeyboardService.removeKeyboard(message.getChatId())).build());
    }

    @Override
    public ReplyKeyboard getKeyboard(long chatId) {
        return replyKeyboardService.removeKeyboard(chatId);
    }

    @Override
    public String getMessage(long chatId) {
        RenameState renameState = commandStateService.getState(chatId, RenameCommandNames.START_COMMAND, false, RenameState.class);
        Locale locale = userService.getLocaleOrDefault((int) chatId);
        String msg = localisationService.getMessage(MessagesProperties.MESSAGE_RENAME_FILE, locale);
        if (renameState != null) {
            msg = localisationService.getMessage(MessagesProperties.MESSAGE_NEW_FILE_NAME, locale);
        }

        return msg;
    }

    private RenameState initState(Message message, MessageMedia any2AnyFile) {
        RenameState renameState = new RenameState();
        renameState.setReplyMessageId(message.getMessageId());
        renameState.setFile(any2AnyFile);

        commandStateService.setState(message.getChatId(), RenameCommandNames.START_COMMAND, renameState);

        return renameState;
    }
}
