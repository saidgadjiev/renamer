package ru.gadjini.telegram.renamer.service.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.dao.RenameQueueDao;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;

@Service
public class RenameQueueService {

    private RenameQueueDao renameQueueDao;

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public RenameQueueService(RenameQueueDao renameQueueDao, FileLimitProperties fileLimitProperties) {
        this.renameQueueDao = renameQueueDao;
        this.fileLimitProperties = fileLimitProperties;
    }

    @Transactional
    public RenameQueueItem createItem(long userId, RenameState renameState, MessageMedia thumbnail, String newFileName) {
        RenameQueueItem renameQueueItem = new RenameQueueItem();
        renameQueueItem.setUserId(userId);
        renameQueueItem.setNewFileName(newFileName);
        renameQueueItem.setReplyToMessageId(renameState.getReplyMessageId());

        renameQueueItem.setFile(renameState.getFile().toTgFile());

        if (thumbnail != null) {
            renameQueueItem.setThumb(thumbnail.toTgFile());
        }

        renameQueueItem.setStatus(RenameQueueItem.Status.WAITING);

        int id = renameQueueDao.create(renameQueueItem);

        renameQueueItem.setQueuePosition(renameQueueDao.getQueuePosition(id,
                renameQueueItem.getFile().getSize() > fileLimitProperties.getLightFileMaxWeight()
                        ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT));
        renameQueueItem.setId(id);

        return renameQueueItem;
    }

    public RenameQueueItem getById(int id) {
        return renameQueueDao.getById(id);
    }
}
