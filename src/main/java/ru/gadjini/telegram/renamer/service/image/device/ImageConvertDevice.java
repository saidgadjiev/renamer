package ru.gadjini.telegram.renamer.service.image.device;

public interface ImageConvertDevice {

    void convert(String in, String out, String ... options);

    void convertToThumb(String in, String out);
}
