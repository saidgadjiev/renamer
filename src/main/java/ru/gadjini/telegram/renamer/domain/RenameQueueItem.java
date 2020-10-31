package ru.gadjini.telegram.renamer.domain;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;

public class RenameQueueItem {

    public static final String TYPE = "rename_queue";

    public static final String ID = "id";

    public static final String FILE = "file";

    public static final String NEW_FILE_NAME = "new_file_name";

    public static final String USER_ID = "user_id";

    public static final String REPLY_TO_MESSAGE_ID = "reply_to_message_id";

    public static final String PROGRESS_MESSAGE_ID = "progress_message_id";

    public static final String PLACE_IN_QUEUE = "place_in_queue";

    public static final String SUPPRESS_USER_EXCEPTIONS = "suppress_user_exceptions";

    private int id;

    private TgFile file;

    private TgFile thumb;

    private String newFileName;

    private int userId;

    private int replyToMessageId;

    private int progressMessageId;

    private Status status;

    private int queuePosition;

    private boolean suppressUserExceptions;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public TgFile getThumb() {
        return thumb;
    }

    public void setThumb(TgFile thumb) {
        this.thumb = thumb;
    }

    public int getProgressMessageId() {
        return progressMessageId;
    }

    public void setProgressMessageId(int progressMessageId) {
        this.progressMessageId = progressMessageId;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public boolean isSuppressUserExceptions() {
        return suppressUserExceptions;
    }

    public void setSuppressUserExceptions(boolean suppressUserExceptions) {
        this.suppressUserExceptions = suppressUserExceptions;
    }

    public enum Status {


        WAITING(0),

        PROCESSING(1),

        EXCEPTION(2);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
