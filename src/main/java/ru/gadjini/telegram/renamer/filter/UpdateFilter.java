package ru.gadjini.telegram.renamer.filter;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.model.bot.api.object.Update;

@Component
public class UpdateFilter extends BaseBotFilter {

    @Override
    public void doFilter(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            super.doFilter(update);
        }
    }
}
