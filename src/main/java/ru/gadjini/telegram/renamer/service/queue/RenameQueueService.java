package ru.gadjini.telegram.renamer.service.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.command.keyboard.RenameState;
import ru.gadjini.telegram.renamer.dao.RenameQueueDao;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.renamer.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.model.Any2AnyFile;
import ru.gadjini.telegram.renamer.service.concurrent.SmartExecutorService;

import java.util.List;

@Service
public class RenameQueueService {

    private RenameQueueDao renameQueueDao;

    @Autowired
    public RenameQueueService(RenameQueueDao renameQueueDao) {
        this.renameQueueDao = renameQueueDao;
    }

    public void resetProcessing() {
        renameQueueDao.resetProcessing();
    }

    public RenameQueueItem createProcessingItem(int userId, RenameState renameState, Any2AnyFile thumbnail, String newFileName) {
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

        renameQueueItem.setStatus(RenameQueueItem.Status.PROCESSING);

        int id = renameQueueDao.create(renameQueueItem);

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

    public List<Integer> deleteByUserId(int userId) {
        return renameQueueDao.deleteByUserId(userId);
    }

    public boolean exists(int jobId) {
        return renameQueueDao.exists(jobId);
    }

    public boolean existsByToReplyMessageId(int replyToMessageId) {
        return renameQueueDao.exists(replyToMessageId);
    }
}
