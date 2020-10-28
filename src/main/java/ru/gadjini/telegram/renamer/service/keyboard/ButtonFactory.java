package ru.gadjini.telegram.renamer.service.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.renamer.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandParser;

import java.util.Locale;

@Service
public class ButtonFactory {

    private LocalisationService localisationService;

    @Autowired
    public ButtonFactory(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public InlineKeyboardButton updateQueryStatus(int queryItemId, Locale locale) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.UPDATE_QUERY_STATUS_COMMAND_DESCRIPTION, locale));
        inlineKeyboardButton.setCallbackData(RenameCommandNames.UPDATE_QUERY_STATUS + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams()
                        .add(Arg.JOB_ID.getKey(), queryItemId)
                        .serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return inlineKeyboardButton;
    }

    public InlineKeyboardButton cancelRenameQuery(int jobId, Locale locale) {
        InlineKeyboardButton button = new InlineKeyboardButton(localisationService.getMessage(MessagesProperties.CANCEL_COMMAND_DESCRIPTION, locale));
        button.setCallbackData(RenameCommandNames.CANCEL_RENAME_QUERY + CommandParser.COMMAND_NAME_SEPARATOR +
                new RequestParams().add(Arg.JOB_ID.getKey(), jobId).serialize(CommandParser.COMMAND_ARG_SEPARATOR));

        return button;
    }
}
