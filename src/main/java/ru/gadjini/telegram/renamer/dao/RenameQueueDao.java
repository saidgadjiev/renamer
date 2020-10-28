package ru.gadjini.telegram.renamer.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;

import java.sql.*;
import java.util.List;

@Repository
public class RenameQueueDao {

    private FileLimitProperties fileLimitProperties;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public RenameQueueDao(FileLimitProperties fileLimitProperties, JdbcTemplate jdbcTemplate) {
        this.fileLimitProperties = fileLimitProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(RenameQueueItem renameQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO rename_queue(user_id, file, thumb, new_file_name, reply_to_message_id, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, renameQueueItem.getUserId());
                    ps.setObject(2, renameQueueItem.getFile().sqlObject());
                    if (renameQueueItem.getThumb() != null) {
                        ps.setObject(3, renameQueueItem.getThumb().sqlObject());
                    } else {
                        ps.setNull(3, Types.OTHER);
                    }
                    ps.setString(4, renameQueueItem.getNewFileName());
                    ps.setInt(5, renameQueueItem.getReplyToMessageId());
                    ps.setInt(6, renameQueueItem.getStatus().getCode());

                    return ps;
                },
                keyHolder
        );

        return ((Number) keyHolder.getKeys().get(RenameQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE rename_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        jdbcTemplate.update("UPDATE rename_queue SET progress_message_id = ? WHERE id = ?",
                ps -> {
                    ps.setInt(1, progressMessageId);
                    ps.setInt(2, id);
                });
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE rename_queue SET status = 0 WHERE status = 1");
    }

    public List<RenameQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE rename_queue SET status = 1 WHERE id IN (SELECT id FROM rename_queue WHERE status = 0 " +
                        "AND (file).size " + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ? ORDER BY created_at LIMIT ?) RETURNING *\n" +
                        ")\n" +
                        "SELECT *, (file).*, (thumb).file_id as th_file_id, (thumb).file_name as th_file_name, (thumb).mime_type as th_mime_type\n" +
                        "FROM r",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM rename_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public RenameQueueItem deleteWithReturning(int id) {
        return jdbcTemplate.query(
                "WITH del AS(DELETE FROM rename_queue WHERE id = ? RETURNING *) SELECT (file).size, id FROM del",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        RenameQueueItem queueItem = new RenameQueueItem();

                        queueItem.setId(rs.getInt(RenameQueueItem.ID));

                        TgFile tgFile = new TgFile();
                        tgFile.setSize(rs.getLong(TgFile.SIZE));

                        return queueItem;
                    }

                    return null;
                }
        );
    }

    public RenameQueueItem deleteByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM rename_queue WHERE user_id = ? RETURNING *) SELECT id, (file).size FROM r",
                ps -> ps.setInt(1, userId),
                rs -> {
                    if (rs.next()) {
                        RenameQueueItem queueItem = new RenameQueueItem();

                        queueItem.setId(rs.getInt(RenameQueueItem.ID));

                        TgFile tgFile = new TgFile();
                        tgFile.setSize(rs.getLong(TgFile.SIZE));

                        return queueItem;
                    }

                    return null;
                });
    }

    public Boolean exists(int jobId) {
        return jdbcTemplate.query("SELECT TRUE FROM rename_queue WHERE id = ?", ps -> ps.setInt(1, jobId), ResultSet::next);
    }

    public Boolean existsByReplyToMessageId(int messageId) {
        return jdbcTemplate.query("SELECT TRUE FROM rename_queue WHERE reply_to_message_id = ?", ps -> ps.setInt(1, messageId), ResultSet::next);
    }

    private RenameQueueItem map(ResultSet resultSet) throws SQLException {
        RenameQueueItem item = new RenameQueueItem();
        item.setId(resultSet.getInt(RenameQueueItem.ID));

        TgFile tgFile = new TgFile();
        tgFile.setFileId(resultSet.getString(TgFile.FILE_ID));
        tgFile.setFileName(resultSet.getString(TgFile.FILE_NAME));
        tgFile.setMimeType(resultSet.getString(TgFile.MIME_TYPE));
        tgFile.setSize(resultSet.getLong(TgFile.SIZE));
        tgFile.setThumb(resultSet.getString(TgFile.THUMB));
        item.setFile(tgFile);

        String thumbFileId = resultSet.getString("th_" + TgFile.FILE_ID);
        if (StringUtils.isNotBlank(thumbFileId)) {
            TgFile thumb = new TgFile();
            thumb.setFileId(thumbFileId);
            thumb.setFileName(resultSet.getString("th_" + TgFile.FILE_NAME));
            thumb.setMimeType(resultSet.getString("th_" + TgFile.MIME_TYPE));
            item.setThumb(thumb);
        }

        item.setNewFileName(resultSet.getString(RenameQueueItem.NEW_FILE_NAME));
        item.setReplyToMessageId(resultSet.getInt(RenameQueueItem.REPLY_TO_MESSAGE_ID));
        item.setProgressMessageId(resultSet.getInt(RenameQueueItem.PROGRESS_MESSAGE_ID));
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        return item;
    }
}
