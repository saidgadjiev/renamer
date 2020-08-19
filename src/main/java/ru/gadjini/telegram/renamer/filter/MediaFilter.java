package ru.gadjini.telegram.renamer.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.exception.UserException;
import ru.gadjini.telegram.renamer.model.Any2AnyFile;
import ru.gadjini.telegram.renamer.model.bot.api.object.Message;
import ru.gadjini.telegram.renamer.model.bot.api.object.Update;
import ru.gadjini.telegram.renamer.service.FileService;
import ru.gadjini.telegram.renamer.service.LocalisationService;
import ru.gadjini.telegram.renamer.service.UserService;
import ru.gadjini.telegram.renamer.service.file.FileManager;
import ru.gadjini.telegram.renamer.utils.MemoryUtils;

import java.util.Locale;

import static ru.gadjini.telegram.renamer.common.TgConstants.LARGE_FILE_SIZE;

@Component
@Qualifier("messagelimits")
public class MediaFilter extends BaseBotFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaFilter.class);

    private UserService userService;

    private LocalisationService localisationService;

    private FileService fileService;

    private FileManager fileManager;

    @Autowired
    public MediaFilter(UserService userService, LocalisationService localisationService, FileService fileService, FileManager fileManager) {
        this.userService = userService;
        this.localisationService = localisationService;
        this.fileService = fileService;
        this.fileManager = fileManager;
    }

    @Override
    public void doFilter(Update update) {
        if (update.hasMessage()) {
            Any2AnyFile file = fileService.getFile(update.getMessage(), Locale.getDefault());
            if (file != null) {
                checkInMediaSize(update.getMessage(), file);
                fileManager.inputFile(update.getMessage().getChatId(), file.getFileId(), file.getFileSize());
            }
        }

        super.doFilter(update);
    }

    private void checkInMediaSize(Message message, Any2AnyFile file) {
        if (file.getFileSize() > LARGE_FILE_SIZE) {
            LOGGER.warn("Large in file({}, {})", message.getFrom().getId(), MemoryUtils.humanReadableByteCount(file.getFileSize()));
            throw new UserException(localisationService.getMessage(
                    MessagesProperties.MESSAGE_TOO_LARGE_IN_FILE,
                    new Object[]{MemoryUtils.humanReadableByteCount(message.getDocument().getFileSize())},
                    userService.getLocaleOrDefault(message.getFrom().getId())));
        } else if (file.getFileSize() > MemoryUtils.MB_100) {
            LOGGER.warn("Heavy file({}, {}, {}, {})", message.getFrom().getId(), MemoryUtils.humanReadableByteCount(file.getFileSize()), file.getMimeType(), file.getFileName());
        }
    }
}
