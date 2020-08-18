package ru.gadjini.telegram.renamer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;

import java.util.Locale;

@Service
public class CommandMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public CommandMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append("/").append(CommandNames.START_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.SET_THUMBNAIL_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.SET_THUMB_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.VIEW_THUMBNAIL_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.VIEW_THUMB_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.DEL_THUMBNAIL_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.DELETE_THUMB_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(CommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        return info.toString();
    }
}
