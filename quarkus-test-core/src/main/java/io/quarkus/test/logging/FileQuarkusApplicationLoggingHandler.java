package io.quarkus.test.logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import io.quarkus.test.bootstrap.ServiceContext;

public class FileQuarkusApplicationLoggingHandler extends LoggingHandler {

    private final File from;

    private String oldContent;

    public FileQuarkusApplicationLoggingHandler(ServiceContext context, File from) {
        super(context);
        this.from = from;
    }

    @Override
    protected void handle() {

        try {
            String newContent = FileUtils.readFileToString(from, StandardCharsets.UTF_8);
            onStringDifference(newContent, oldContent);
            oldContent = newContent;

            //            byte[] buffer = new byte[FILE_BUFFER_SIZE];
            //            int bytesRead;
            //            while ((bytesRead = from.read(buffer)) != -1) {
            //                String line = new String(buffer, 0, bytesRead);
            //                onLines(line);
            //                outStream.write(line.getBytes());
            //            }
        } catch (IOException ignored) {
        }
    }

}
