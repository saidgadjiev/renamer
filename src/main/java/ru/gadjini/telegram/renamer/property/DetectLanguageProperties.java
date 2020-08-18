package ru.gadjini.telegram.renamer.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("detect")
public class DetectLanguageProperties {

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
