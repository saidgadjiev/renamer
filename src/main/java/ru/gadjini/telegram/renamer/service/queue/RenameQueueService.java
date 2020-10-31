package ru.gadjini.telegram.renamer.service.queue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.dao.RenameQueueDao;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;

import java.util.List;

@Service
public class RenameQueueService {

    private RenameQueueDao renameQueueDao;

    private FileLimitProperties fileLimitProperties;

    @Autowired
    public RenameQueueService(RenameQueueDao renameQueueDao, FileLimitProperties fileLimitProperties) {
        this.renameQueueDao = renameQueueDao;
        this.fileLimitProperties = fileLimitProperties;
    }

    public void resetProcessing() {
        renameQueueDao.resetProcessing();
    }

    public RenameQueueItem createItem(int userId, RenameState renameState, MessageMedia thumbnail, String newFileName) {
        RenameQueueItem renameQueueItem = new RenameQueueItem();
        renameQueueItem.setUserId(userId);
        renameQueueItem.setNewFileName(newFileName);
        renameQueueItem.setReplyToMessageId(renameState.getReplyMessageId());

        TgFile file = new TgFile();
        file.setFileName(renameState.getFile().getFileName());
        file.setFileId(renameState.getFile().getFileId());
        file.setMimeType(renameState.getFile().getMimeType());
        file.setSize(renameState.getFile().getFileSize());
        file.setThumb(renameState.getFile().getThumb());
        renameQueueItem.setFile(file);

        if (thumbnail != null) {
            TgFile thumb = new TgFile();
            thumb.setFileId(thumbnail.getFileId());
            thumb.setFileName(thumbnail.getFileName());
            thumb.setMimeType(thumbnail.getMimeType());
            thumb.setSize(thumbnail.getFileSize());
            renameQueueItem.setThumb(thumb);
        }

        renameQueueItem.setStatus(RenameQueueItem.Status.WAITING);

        int id = renameQueueDao.create(renameQueueItem);

        renameQueueItem.setQueuePosition(renameQueueDao.getPlaceInQueue(id,
                renameQueueItem.getFile().getSize() > fileLimitProperties.getLightFileMaxWeight()
                        ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT));
        renameQueueItem.setId(id);

        return renameQueueItem;
    }

    public void setWaiting(int id) {
        renameQueueDao.setWaiting(id);
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        renameQueueDao.setProgressMessageId(id, progressMessageId);
    }

    public RenameQueueItem poll(SmartExecutorService.JobWeight weight) {
        List<RenameQueueItem> poll = renameQueueDao.poll(weight, 1);

        return poll.isEmpty() ? null : poll.iterator().next();
    }

    public List<RenameQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return renameQueueDao.poll(weight, limit);
    }

    public void delete(int id) {
        renameQueueDao.delete(id);
    }

    public RenameQueueItem deleteWithReturning(int id) {
        return renameQueueDao.deleteWithReturning(id);
    }

    public RenameQueueItem deleteByUserId(int userId) {
        return renameQueueDao.deleteProcessingOrWaitingByUserId(userId);
    }

    public boolean exists(int jobId) {
        return renameQueueDao.exists(jobId);
    }

    public boolean existsByToReplyMessageId(int replyToMessageId) {
        return renameQueueDao.exists(replyToMessageId);
    }

    public RenameQueueItem getById(int id) {
        return renameQueueDao.getById(id);
    }

    public void setExceptionStatus(int id, Throwable ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        renameQueueDao.setExceptionStatus(id, exception);
    }

    public void setWaiting(int id, Throwable ex) {
        String exception = ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex);
        renameQueueDao.setWaiting(id, exception);
    }
}
