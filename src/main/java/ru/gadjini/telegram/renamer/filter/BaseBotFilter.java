package ru.gadjini.telegram.renamer.filter;

import ru.gadjini.telegram.renamer.model.bot.api.object.Update;

public class BaseBotFilter implements BotFilter {

    private BotFilter next;

    @Override
    public final BotFilter setNext(BotFilter next) {
        this.next = next;

        return next;
    }

    @Override
    public void doFilter(Update update) {
        if (next != null) {
            next.doFilter(update);
        }
    }
}
