package ru.gadjini.telegram.renamer.bot.command.keyboard.rename;

import ru.gadjini.telegram.renamer.domain.HasThumb;
import ru.gadjini.telegram.renamer.model.Any2AnyFile;

public class RenameState implements HasThumb {

    private Any2AnyFile file;

    private int replyMessageId;

    private Any2AnyFile thumb;

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

    @Override
    public void setThumb(Any2AnyFile thumb) {
        this.thumb = thumb;
    }

    @Override
    public void delThumb() {
        thumb = null;
    }

    @Override
    public Any2AnyFile getThumb() {
        return thumb;
    }
}
