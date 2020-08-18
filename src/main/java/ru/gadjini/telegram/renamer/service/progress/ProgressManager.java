package ru.gadjini.telegram.renamer.service.progress;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.service.message.TelegramMediaServiceProvider;

@Service
public class ProgressManager {

    public boolean isShowingProgress(long fileSize) {
        if (fileSize == 0) {
            return true;
        }
        return TelegramMediaServiceProvider.BOT_API_DOWNLOAD_FILE_LIMIT < fileSize;
    }
}
