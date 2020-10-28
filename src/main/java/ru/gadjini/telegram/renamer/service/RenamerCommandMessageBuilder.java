package ru.gadjini.telegram.renamer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.RenameCommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.service.CommandMessageBuilder;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;

import java.util.Locale;

@Service
public class RenamerCommandMessageBuilder implements CommandMessageBuilder {

    private LocalisationService localisationService;

    @Autowired
    public RenamerCommandMessageBuilder(LocalisationService localisationService) {
        this.localisationService = localisationService;
    }

    @Override
    public String getCommandsInfo(Locale locale) {
        StringBuilder info = new StringBuilder();

        info.append("/").append(RenameCommandNames.START_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.START_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(RenameCommandNames.SET_THUMBNAIL_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.SET_THUMB_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(RenameCommandNames.VIEW_THUMBNAIL_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.VIEW_THUMB_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(RenameCommandNames.DEL_THUMBNAIL_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.DEL_THUMB_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(RenameCommandNames.LANGUAGE_COMMAND_NAME).append(" - ").append(localisationService.getMessage(MessagesProperties.LANGUAGE_COMMAND_DESCRIPTION, locale)).append("\n");
        info.append("/").append(RenameCommandNames.HELP_COMMAND).append(" - ").append(localisationService.getMessage(MessagesProperties.HELP_COMMAND_DESCRIPTION, locale));

        return info.toString();
    }
}
