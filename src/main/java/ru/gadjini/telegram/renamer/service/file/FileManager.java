package ru.gadjini.telegram.renamer.service.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.exception.UserException;
import ru.gadjini.telegram.renamer.io.SmartTempFile;
import ru.gadjini.telegram.renamer.model.bot.api.object.Progress;
import ru.gadjini.telegram.renamer.service.LocalisationService;
import ru.gadjini.telegram.renamer.service.message.TelegramMediaServiceProvider;
import ru.gadjini.telegram.renamer.service.telegram.TelegramMTProtoService;
import ru.gadjini.telegram.renamer.service.UserService;

import java.util.Locale;

@Service
public class FileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileManager.class);

    private TelegramMTProtoService telegramService;

    private TelegramMediaServiceProvider mediaServiceProvider;

    private FileLimitsDao fileLimitsDao;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public FileManager(TelegramMTProtoService telegramService, TelegramMediaServiceProvider mediaServiceProvider,
                       FileLimitsDao fileLimitsDao, LocalisationService localisationService, UserService userService) {
        this.telegramService = telegramService;
        this.mediaServiceProvider = mediaServiceProvider;
        this.fileLimitsDao = fileLimitsDao;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void setInputFilePending(long chatId, Integer replyToMessageId, String fileId, long fileSize, String command) {
        if (fileSize > 0 && TelegramMediaServiceProvider.BOT_API_DOWNLOAD_FILE_LIMIT > fileSize) {
            return;
        }
        fileLimitsDao.setInputFile(chatId, new InputFileState(replyToMessageId, fileId, command));
    }

    public void resetLimits(long chatId) {
        fileLimitsDao.deleteInputFile(chatId);
    }

    public void inputFile(long chatId, String fileId, long fileSize) {
        if (fileSize == 0) {
            LOGGER.debug("File size 0({}, {}, {})", chatId, fileId, fileId);
        }
        if (fileSize > 0 && TelegramMediaServiceProvider.BOT_API_DOWNLOAD_FILE_LIMIT > fileSize) {
            return;
        }
        InputFileState inputFileState = fileLimitsDao.getInputFile(chatId);
        if (inputFileState != null) {
            Long ttl = fileLimitsDao.getInputFileTtl(chatId);

            if (ttl == null || ttl == -1) {
                Integer replyToMessageId = inputFileState.getReplyToMessageId();
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INPUT_FILE_WAIT, locale)).setReplyToMessageId(replyToMessageId);
            } else {
                Locale locale = userService.getLocaleOrDefault((int) chatId);
                throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_INPUT_FILE_WAIT_TTL, new Object[]{ttl}, locale));
            }
        }
    }

    public void downloadFileByFileId(String fileId, SmartTempFile outputFile) {
        telegramService.downloadFileByFileId(fileId, outputFile);
    }

    public void downloadFileByFileId(String fileId, long fileSize, Progress progress, SmartTempFile outputFile) {
        mediaServiceProvider.getDownloadMediaService(fileSize).downloadFileByFileId(fileId, fileSize, progress, outputFile);
    }

    public FileWorkObject fileWorkObject(long chatId, long fileSize) {
        return new FileWorkObject(chatId, fileSize, fileLimitsDao);
    }

    public boolean cancelDownloading(String fileId) {
        return telegramService.cancelDownloading(fileId);
    }

    public boolean cancelUploading(String filePath) {
        return telegramService.cancelUploading(filePath);
    }

    public void cancelDownloads() {
        telegramService.cancelDownloads();
    }

    public void restoreFileIfNeed(String filePath, String fileId) {
        telegramService.restoreFileIfNeed(filePath, fileId);
    }
}
