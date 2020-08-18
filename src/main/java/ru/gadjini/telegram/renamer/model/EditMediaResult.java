package ru.gadjini.telegram.renamer.model;

public class EditMediaResult {

    private String fileId;

    public EditMediaResult(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}
