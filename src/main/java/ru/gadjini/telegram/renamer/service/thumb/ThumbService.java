package ru.gadjini.telegram.renamer.service.thumb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.service.image.ImageConvertDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloader;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;

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
        String filePath = fileDownloader.downloadFileByFileId(fileId, fileSize, false);
        try {
            SmartTempFile out = tempFileService.createTempFile(FileTarget.TEMP, chatId, fileId, TAG, Format.JPG.getExt());
            try {
                convertDevice.convertToThumb(filePath, out.getAbsolutePath());

                return out;
            } catch (Exception e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            new SmartTempFile(new File(filePath), false);
        }
    }
}
