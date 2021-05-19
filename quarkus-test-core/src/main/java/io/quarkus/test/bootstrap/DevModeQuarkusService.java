package io.quarkus.test.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;

import io.quarkus.test.services.DevModeQuarkusApplication;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class DevModeQuarkusService extends BaseService<DevModeQuarkusService> {

    public RequestSpecification given() {
        return RestAssured.given().baseUri(getHost()).basePath("/").port(getPort());
    }

    public void modifyFile(String file, Function<String, String> modifier) {
        try {
            File targetFile = servicePath().resolve(file).toFile();
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
            Path targetPath = servicePath().resolve(target);
            FileUtils.deleteQuietly(targetPath.toFile());

            FileUtils.copyFile(sourcePath.toFile(), targetPath.toFile());
        } catch (IOException e) {
            Assertions.fail("Error copying file. Caused by " + e.getMessage());
        }
    }

    @Override
    public void validate(Field field) {
        if (!field.isAnnotationPresent(DevModeQuarkusApplication.class)) {
            Assertions.fail("DevModeQuarkusService service is not annotated with DevModeQuarkusApplication");
        }
    }

    private Path servicePath() {
        return Paths.get("target/" + getName());
    }
}
