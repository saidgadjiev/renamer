package ru.gadjini.telegram.renamer.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.service.file.FileManager;
import ru.gadjini.telegram.renamer.service.rename.RenameService;

@Component
public class ContextCloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextCloseListener.class);

    private RenameService renameService;

    private FileManager fileManager;

    private ThreadPoolTaskExecutor commonThreadPool;

    public ContextCloseListener(RenameService renameService,
                                FileManager fileManager, @Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor commonThreadPool) {
        this.renameService = renameService;
        this.fileManager = fileManager;
        this.commonThreadPool = commonThreadPool;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            renameService.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown renameService. " + e.getMessage(), e);
        }
        try {
            commonThreadPool.shutdown();
        } catch (Throwable e) {
            LOGGER.error("Error shutdown commonThreadPool. " + e.getMessage(), e);
        }
        try {
            fileManager.cancelDownloads();
        } catch (Throwable e) {
            LOGGER.error("Error cancel downloading telegramService. " + e.getMessage(), e);
        }
        LOGGER.debug("Shutdown success");
    }
}
