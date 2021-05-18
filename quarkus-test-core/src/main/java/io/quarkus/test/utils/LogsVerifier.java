package io.quarkus.test.utils;

import java.util.List;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.Service;

public class LogsVerifier {

    private static final int POLL_SECONDS = 5;
    private static final int TIMEOUT_MINUTES = 5;

    private final Service service;

    public LogsVerifier(Service service) {
        this.service = service;
    }

    public void assertContains(String expectedLog) {
        AwaitilityUtils.untilAsserted(() -> {
            List<String> actualLogs = service.getLogs();
            Assertions.assertTrue(actualLogs.stream().anyMatch(line -> line.contains(expectedLog)),
                    "Log does not contain " + expectedLog + ". Full logs: " + actualLogs);
        });
    }
}
