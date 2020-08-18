package ru.gadjini.telegram.renamer.filter;

import ru.gadjini.telegram.renamer.model.bot.api.object.Update;

public interface BotFilter {

    BotFilter setNext(BotFilter next);

    void doFilter(Update update);
}
