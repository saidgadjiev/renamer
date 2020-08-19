package ru.gadjini.telegram.renamer.request;

public enum Arg {

    JOB_ID("s");

    private final String key;

    Arg(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
