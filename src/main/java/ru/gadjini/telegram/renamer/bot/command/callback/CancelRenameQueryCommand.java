package ru.gadjini.telegram.renamer.bot.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.bot.command.api.CallbackBotCommand;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.renamer.request.Arg;
import ru.gadjini.telegram.renamer.request.RequestParams;
import ru.gadjini.telegram.renamer.service.rename.RenameService;

@Service
public class CancelRenameQueryCommand implements CallbackBotCommand {

    private RenameService renameService;

    @Autowired
    public CancelRenameQueryCommand(RenameService renameService) {
        this.renameService = renameService;
    }

    @Override
    public String getName() {
        return CommandNames.CANCEL_RENAME_QUERY;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int jobId = requestParams.getInt(Arg.JOB_ID.getKey());
        renameService.cancel(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), callbackQuery.getId(), jobId);
    }
}
