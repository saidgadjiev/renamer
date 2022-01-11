package ru.gadjini.telegram.renamer.service.thumb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.service.image.ImageConvertDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloader;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

@Service
public class ThumbService {

    private static final String TAG = "thumb";

    private FileDownloader fileDownloader;

    private TempFileService tempFileService;

    private ImageConvertDevice convertDevice;

    @Autowired
    public ThumbService(FileDownloader fileDownloader, TempFileService tempFileService,
                        ImageConvertDevice convertDevice) {
        this.fileDownloader = fileDownloader;
        this.tempFileService = tempFileService;
        this.convertDevice = convertDevice;
    }

    public SmartTempFile convertToThumb(long chatId, String fileId, long fileSize) {
        SmartTempFile out = tempFileService.createTempFile(FileTarget.TEMP, chatId, fileId, TAG, Format.JPG.getExt());
        try {
            SmartTempFile thumb = downloadThumb(chatId, fileId, fileSize);
            try {
                convertDevice.convertToThumb(thumb.getAbsolutePath(), out.getAbsolutePath());

                return out;
            } finally {
                tempFileService.delete(thumb);
            }
        } catch (Exception e) {
            out.smartDelete();
            throw e;
        }
    }

    public SmartTempFile downloadThumb(long userId, String fileId, long fileSize) {
        SmartTempFile result = tempFileService.createTempFile(FileTarget.DOWNLOAD, userId,
                fileId, TAG, Format.JPG.getExt());
        try {
            fileDownloader.downloadFileByFileId(fileId, fileSize, result, false);
        } catch (Throwable e) {
            tempFileService.delete(result);
            return null;
        }

        return result;
    }
}
