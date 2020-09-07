package ru.gadjini.telegram.renamer.command.keyboard;

import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;

public class RenameState {

    private MessageMedia file;

    private int replyMessageId;

    public void setFile(MessageMedia file) {
        this.file = file;
    }

    public MessageMedia getFile() {
        return file;
    }

    public int getReplyMessageId() {
        return replyMessageId;
    }

    public void setReplyMessageId(int replyMessageId) {
        this.replyMessageId = replyMessageId;
    }
}
