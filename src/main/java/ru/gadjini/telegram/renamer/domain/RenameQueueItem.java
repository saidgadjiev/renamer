package ru.gadjini.telegram.renamer.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;

public class RenameQueueItem extends QueueItem {

    public static final String TYPE = "rename_queue";

    public static final String FILE = "file";

    public static final String NEW_FILE_NAME = "new_file_name";

    private TgFile file;

    private TgFile thumb;

    private String newFileName;

    public TgFile getFile() {
        return file;
    }

    public void setFile(TgFile file) {
        this.file = file;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public TgFile getThumb() {
        return thumb;
    }

    public void setThumb(TgFile thumb) {
        this.thumb = thumb;
    }

    @Override
    public long getSize() {
        return file.getSize();
    }
}
