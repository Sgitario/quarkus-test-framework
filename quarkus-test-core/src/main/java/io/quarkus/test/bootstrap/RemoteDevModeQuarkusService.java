package io.quarkus.test.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;

import io.quarkus.test.services.RemoteDevModeQuarkusApplication;

public class RemoteDevModeQuarkusService extends RestService {

    public void modifyFile(String file, Function<String, String> modifier) {
        try {
            File targetFile = getServiceFolder().resolve(file).toFile();
            String original = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
            String updated = modifier.apply(original);

            FileUtils.writeStringToFile(targetFile, updated, StandardCharsets.UTF_8, false);
        } catch (IOException e) {
            Assertions.fail("Error modifying file. Caused by " + e.getMessage());
        }
    }

    public void copyFile(String file, String target) {
        try {
            Path sourcePath = Path.of(file);
            File targetPath = getServiceFolder().resolve(target).toFile();
            FileUtils.deleteQuietly(targetPath);

            FileUtils.copyFile(sourcePath.toFile(), targetPath);
            targetPath.setLastModified(System.currentTimeMillis());
        } catch (IOException e) {
            Assertions.fail("Error copying file. Caused by " + e.getMessage());
        }
    }

    @Override
    public void validate(Field field) {
        if (!field.isAnnotationPresent(RemoteDevModeQuarkusApplication.class)) {
            Assertions.fail("RemoteDevModeQuarkusService service is not annotated with RemoteDevModeQuarkusApplication");
        }
    }
}
