package ru.gadjini.telegram.renamer.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bot")
public class BotApiProperties {

    private String api;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }
}
