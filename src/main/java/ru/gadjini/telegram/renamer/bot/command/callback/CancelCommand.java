package ru.gadjini.telegram.renamer.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.bot.command.api.CallbackBotCommand;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.renamer.request.RequestParams;
import ru.gadjini.telegram.renamer.service.command.CommandExecutor;

@Component
public class CancelCommand implements CallbackBotCommand {

    private CommandExecutor commandExecutor;

    @Autowired
    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        commandExecutor.cancelCommand(callbackQuery.getMessage().getChatId(), callbackQuery.getId());
    }
}
