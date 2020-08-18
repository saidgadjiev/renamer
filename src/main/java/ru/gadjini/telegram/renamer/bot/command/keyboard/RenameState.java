package ru.gadjini.telegram.renamer.bot.command.keyboard;

import ru.gadjini.telegram.renamer.model.Any2AnyFile;

public class RenameState {

    private Any2AnyFile file;

    private int replyMessageId;

    public void setFile(Any2AnyFile file) {
        this.file = file;
    }

    public Any2AnyFile getFile() {
        return file;
    }

    public int getReplyMessageId() {
        return replyMessageId;
    }

    public void setReplyMessageId(int replyMessageId) {
        this.replyMessageId = replyMessageId;
    }
}
