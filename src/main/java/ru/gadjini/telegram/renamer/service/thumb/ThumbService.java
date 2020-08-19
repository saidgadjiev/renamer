package ru.gadjini.telegram.renamer.service.thumb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.io.SmartTempFile;
import ru.gadjini.telegram.renamer.service.TempFileService;
import ru.gadjini.telegram.renamer.service.format.Format;
import ru.gadjini.telegram.renamer.service.format.FormatService;
import ru.gadjini.telegram.renamer.service.image.device.ImageConvertDevice;
import ru.gadjini.telegram.renamer.service.file.FileManager;

@Service
public class ThumbService {

    public static final String TAG = "thumb";

    private FileManager fileManager;

    private TempFileService tempFileService;

    private FormatService formatService;

    private ImageConvertDevice convertDevice;

    @Autowired
    public ThumbService(FileManager fileManager, TempFileService tempFileService,
                        FormatService formatService, ImageConvertDevice convertDevice) {
        this.fileManager = fileManager;
        this.tempFileService = tempFileService;
        this.formatService = formatService;
        this.convertDevice = convertDevice;
    }

    public SmartTempFile convertToThumb(long chatId, String fileId, long fileSize, String fileName, String mimeType) {
        String ext = formatService.getExt(fileName, mimeType);
        SmartTempFile thumb = tempFileService.createTempFile(chatId, fileId, TAG, ext);
        try {
            fileManager.downloadFileByFileId(fileId, fileSize, thumb);
            SmartTempFile out = tempFileService.createTempFile(chatId, fileId, TAG, Format.JPG.getExt());
            try {
                convertDevice.convertToThumb(thumb.getAbsolutePath(), out.getAbsolutePath());

                return out;
            } catch (Exception e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            thumb.smartDelete();
        }
    }
}
