package ru.gadjini.telegram.renamer.service.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice implements ImageConvertDevice, ImageIdentifyDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public ImageMagickDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert(String in, String out, String... options) {
        try {
            processExecutor.execute(getCommand(in, out, options));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public void convertToThumb(String in, String out) {
        try {
            processExecutor.execute(getThumbCommand(in, out));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public String getSize(String in) {
        try {
            return processExecutor.executeWithResult(getSizeCommand(in));
        } catch (InterruptedException e) {
            throw new ProcessException(e);
        }
    }

    private String[] getThumbCommand(String in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add("-thumbnail");
        command.add("320x320");
        command.add(in);
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getSizeCommand(String in) {
        List<String> command = new ArrayList<>(identifyCommandName());
        command.add("-format");
        command.add("\"%wx%h\"");
        command.add(in);

        return command.toArray(new String[0]);
    }

    private String[] getCommand(String in, String out, String... options) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add("-background");
        command.add("none");
        command.addAll(Arrays.asList(options));
        command.add(in);
        command.add(out);

        return command.toArray(new String[0]);
    }

    private List<String> identifyCommandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "identify") : List.of("identify");
    }

    private List<String> convertCommandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "convert") : List.of("convert");
    }
}
