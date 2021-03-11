package ru.gadjini.telegram.renamer.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.DownloadQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.domain.WorkQueueItem;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.io.File;

public class RenameQueueItem extends WorkQueueItem {

    public static final String TYPE = "rename_queue";

    public static final String FILE = "file";

    public static final String NEW_FILE_NAME = "new_file_name";

    public static final String DOWNLOADS = "downloads";

    private TgFile file;

    private TgFile thumb;

    private String newFileName;

    private DownloadQueueItem download;

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

    public void setDownload(DownloadQueueItem download) {
        this.download = download;
    }

    public SmartTempFile getDownloadedFile() {
        return download == null ? null : new SmartTempFile(new File(download.getFilePath()), download.isDeleteParentDir());
    }

    @Override
    public long getSize() {
        return file.getSize();
    }
}
