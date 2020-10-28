package ru.gadjini.telegram.renamer.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.job.RenamerJob;
import ru.gadjini.telegram.renamer.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

@Service
public class CancelRenameQueryCommand implements CallbackBotCommand {

    private RenamerJob renamerJob;

    @Autowired
    public CancelRenameQueryCommand(RenamerJob renamerJob) {
        this.renamerJob = renamerJob;
    }

    @Override
    public String getName() {
        return RenameCommandNames.CANCEL_RENAME_QUERY;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int jobId = requestParams.getInt(Arg.JOB_ID.getKey());
        renamerJob.cancel(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(), callbackQuery.getId(), jobId);
    }
}
