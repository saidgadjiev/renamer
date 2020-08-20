package ru.gadjini.telegram.renamer.exception;

public class DownloadCanceledException extends RuntimeException {

    public DownloadCanceledException(String message) {
        super(message);
    }
}
