package ru.gadjini.telegram.renamer.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.renamer.domain.RenameQueueItem;
import ru.gadjini.telegram.smart.bot.commons.dao.QueueDao;
import ru.gadjini.telegram.smart.bot.commons.dao.WorkQueueDaoDelegate;
import ru.gadjini.telegram.smart.bot.commons.domain.DownloadQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.property.ServerProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.utils.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class RenameQueueDao implements WorkQueueDaoDelegate<RenameQueueItem> {

    private FileLimitProperties fileLimitProperties;

    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;

    private ServerProperties serverProperties;

    @Autowired
    public RenameQueueDao(FileLimitProperties fileLimitProperties,
                          JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, ServerProperties serverProperties) {
        this.fileLimitProperties = fileLimitProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.serverProperties = serverProperties;
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

    @Override
    public List<RenameQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE rename_queue SET " + QueueDao.getUpdateList(serverProperties.getNumber()) + " WHERE id IN " +
                        "(SELECT id FROM rename_queue qu WHERE status = 0 " +
                        "AND (file).size " + getSign(weight) + " ? " +
                        " AND NOT EXISTS(SELECT 1 FROM " + DownloadQueueItem.NAME + " dq WHERE dq.producer_id = qu.id AND dq.producer = 'rename_queue' AND dq.status != 3) "
                        + QueueDao.POLL_ORDER_BY + " LIMIT " + limit + ") RETURNING *\n" +
                        ")\n" +
                        "SELECT id,\n" +
                        "       created_at,\n" +
                        "       user_id,\n" +
                        "       new_file_name,\n" +
                        "       reply_to_message_id,\n" +
                        "       status,\n" +
                        "       progress_message_id,\n" +
                        "       started_at,\n" +
                        "       last_run_at,\n" +
                        "       completed_at,\n" +
                        "       exception,\n" +
                        " server,\n" +
                        "       suppress_user_exceptions,\n" +
                        "       attempts, 1 as queue_position, (file).*, (thumb).file_id as th_file_id, (thumb).file_name as th_file_name, (thumb).mime_type as th_mime_type,\n" +
                        "(SELECT json_agg(ds) FROM (SELECT * FROM " + DownloadQueueItem.NAME + " dq WHERE dq.producer = 'rename_queue' AND dq.producer_id = r.id) as ds) as downloads\n" +
                        "FROM r",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs, rowNum) -> map(rs)
        );
    }

    public Integer getQueuePosition(int id, SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COALESCE(queue_position, 1) as queue_position\n" +
                        "FROM (SELECT id, row_number() over (ORDER BY created_at) AS queue_position\n" +
                        "      FROM rename_queue \n" +
                        "      WHERE status = 0 AND (file).size" + getSign(weight) + " ?\n" +
                        ") as file_q\n" +
                        "WHERE id = ?",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                },
                rs -> {
                    if (rs.next()) {
                        return rs.getInt(RenameQueueItem.QUEUE_POSITION);
                    }

                    return 1;
                }
        );
    }

    public SmartExecutorService.JobWeight getWeight(int id) {
        Long size = jdbcTemplate.query(
                "SELECT (file).size FROM rename_queue WHERE id = ?",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getLong("size") : null
        );

        return size == null ? null : size > fileLimitProperties.getLightFileMaxWeight() ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
    }

    @Override
    public RenameQueueItem getById(int id) {
        SmartExecutorService.JobWeight weight = getWeight(id);

        if (weight == null) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT f.id,\n" +
                        "       f.created_at,\n" +
                        "       f.user_id,\n" +
                        "       f.new_file_name,\n" +
                        "       f.reply_to_message_id,\n" +
                        "       f.status,\n" +
                        "       f.progress_message_id,\n" +
                        "       f.started_at,\n" +
                        "       f.last_run_at,\n" +
                        "       f.server,\n" +
                        "       f.completed_at,\n" +
                        "       f.exception,\n" +
                        "       f.suppress_user_exceptions,\n" +
                        "       f.attempts, (f.file).*, (f.thumb).file_id as th_file_id, (thumb).file_name as th_file_name, (thumb).mime_type as th_mime_type, COALESCE(queue_place.queue_position, 1) as queue_position\n" +
                        "FROM rename_queue f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as queue_position\n" +
                        "                     FROM rename_queue\n" +
                        "      WHERE status = 0 AND (file).size" + getSign(weight) + " ?\n" +
                        ") queue_place ON f.id = queue_place.id\n" +
                        "WHERE f.id = ?\n",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                },
                rs -> {
                    if (rs.next()) {
                        return map(rs);
                    }

                    return null;
                }
        );
    }

    @Override
    public List<RenameQueueItem> deleteAndGetProcessingOrWaitingByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM rename_queue WHERE user_id = ? RETURNING *) SELECT id, (file).size, server FROM r",
                ps -> ps.setInt(1, userId),
                (rs, num) -> mapDeleteItem(rs));
    }

    @Override
    public RenameQueueItem deleteAndGetById(int id) {
        return jdbcTemplate.query(
                "WITH del AS(DELETE FROM rename_queue WHERE id = ? RETURNING *) SELECT (file).size, id FROM del",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        return mapDeleteItem(rs);
                    }

                    return null;
                }
        );
    }

    @Override
    public long countReadToComplete(SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM rename_queue qu WHERE qu.status = 0 AND (file).size " + getSign(weight) + " ?" +
                        " AND NOT EXISTS(select 1 FROM " + DownloadQueueItem.NAME + " dq where dq.producer = 'rename_queue' AND dq.producer_id = qu.id AND dq.status != 3) ",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    @Override
    public long countProcessing(SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM rename_queue qu WHERE qu.status = 1 AND (file).size " + getSign(weight) + " ?",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    @Override
    public String getProducerName() {
        return getQueueName();
    }

    @Override
    public String getQueueName() {
        return RenameQueueItem.TYPE;
    }

    private String getSign(SmartExecutorService.JobWeight weight) {
        return weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">";
    }

    private RenameQueueItem mapDeleteItem(ResultSet rs) throws SQLException {
        RenameQueueItem queueItem = new RenameQueueItem();

        queueItem.setId(rs.getInt(RenameQueueItem.ID));
        queueItem.setServer(rs.getInt(QueueItem.SERVER));

        TgFile tgFile = new TgFile();
        tgFile.setSize(rs.getLong(TgFile.SIZE));

        return queueItem;
    }

    private RenameQueueItem map(ResultSet resultSet) throws SQLException {
        Set<String> columns = JdbcUtils.getColumnNames(resultSet.getMetaData());

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
        item.setServer(resultSet.getInt(QueueItem.SERVER));
        item.setNewFileName(resultSet.getString(RenameQueueItem.NEW_FILE_NAME));
        item.setReplyToMessageId(resultSet.getInt(RenameQueueItem.REPLY_TO_MESSAGE_ID));
        item.setProgressMessageId(resultSet.getInt(RenameQueueItem.PROGRESS_MESSAGE_ID));
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        item.setQueuePosition(resultSet.getInt(RenameQueueItem.QUEUE_POSITION));
        if (columns.contains(RenameQueueItem.DOWNLOADS)) {
            PGobject downloadsArr = (PGobject) resultSet.getObject(RenameQueueItem.DOWNLOADS);
            if (downloadsArr != null) {
                try {
                    List<Map<String, Object>> values = objectMapper.readValue(downloadsArr.getValue(), new TypeReference<>() {
                    });
                    List<DownloadQueueItem> downloadingQueueItems = new ArrayList<>();
                    for (Map<String, Object> value : values) {
                        DownloadQueueItem downloadingQueueItem = new DownloadQueueItem();
                        downloadingQueueItem.setFilePath((String) value.get(DownloadQueueItem.FILE_PATH));
                        downloadingQueueItem.setFile(objectMapper.convertValue(value.get(DownloadQueueItem.FILE), TgFile.class));
                        downloadingQueueItem.setDeleteParentDir((Boolean) value.get(DownloadQueueItem.DELETE_PARENT_DIR));
                        downloadingQueueItems.add(downloadingQueueItem);
                    }
                    item.setDownload(downloadingQueueItems.isEmpty() ? null : downloadingQueueItems.get(0));
                } catch (JsonProcessingException e) {
                    throw new SQLException(e);
                }
            }
        }

        return item;
    }
}
