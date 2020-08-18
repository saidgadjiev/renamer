package ru.gadjini.telegram.renamer.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.model.bot.api.object.Update;
import ru.gadjini.telegram.renamer.service.RenamerBotService;

@Component
public class RenamerBotFilter extends BaseBotFilter {

    private RenamerBotService renamerBotService;

    @Autowired
    public RenamerBotFilter(RenamerBotService renamerBotService) {
        this.renamerBotService = renamerBotService;
    }

    @Override
    public void doFilter(Update update) {
        renamerBotService.onUpdateReceived(update);
    }
}
