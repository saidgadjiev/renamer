package ru.gadjini.telegram.renamer.bot.command.api;

import ru.gadjini.telegram.renamer.model.bot.api.object.Message;

public interface BotCommand {
    String COMMAND_INIT_CHARACTER = "/";

    void processMessage(Message message);

    String getCommandIdentifier();
}
