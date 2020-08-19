package ru.gadjini.telegram.renamer.service.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.service.message.TelegramMediaServiceProvider;

@Service
public class ProgressManager {

    private TelegramMediaServiceProvider mediaServiceProvider;

    @Autowired
    public ProgressManager(TelegramMediaServiceProvider mediaServiceProvider) {
        this.mediaServiceProvider = mediaServiceProvider;
    }

    public boolean isShowingDownloadProgress(long fileSize) {
        return !mediaServiceProvider.isBotApiDownloadFile(fileSize);
    }
}
