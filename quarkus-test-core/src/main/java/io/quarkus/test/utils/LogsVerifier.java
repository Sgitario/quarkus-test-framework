package io.quarkus.test.utils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.quarkus.test.bootstrap.ServiceContext;
import net.jodah.concurrentunit.Waiter;

public class LogsVerifier {

    private static final int POLL_SECONDS = 5;
    private static final int TIMEOUT_SECONDS = 30;
    private static final int ATTEMPTS = 10;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final ServiceContext service;

    public LogsVerifier(ServiceContext service) {
        this.service = service;
    }

    public void assertContains(String expectedLog) {
        Waiter waiter = new Waiter();

        executorService.schedule(() -> assertContains(waiter, expectedLog, 0), POLL_SECONDS, TimeUnit.SECONDS);

        try {
            waiter.await(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //        Awaitility.await()
        //                .ignoreExceptions()
        //                .pollInterval(POLL_SECONDS, TimeUnit.SECONDS)
        //                .pollThread(Thread::new)
        //                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        //                .untilAsserted(() -> {
        //                    List<String> actualLogs = service.getOwner().getLogs();
        //                    Assertions.assertTrue(actualLogs.stream().anyMatch(line -> line.contains(expectedLog)),
        //                            "Log does not contain " + expectedLog + ". Full logs: " + actualLogs);
        //                });

        //        AwaitilityUtils.untilAsserted(() -> {
        //            //            List<String> actualLogs = service.getLogs();
        //            //            Assertions.assertTrue(actualLogs.stream().anyMatch(line -> line.contains(expectedLog)),
        //            //                    "Log does not contain " + expectedLog + ". Full logs: " + actualLogs);
        //
        //            Assertions.assertTrue(false);
        //        });
    }

    private void assertContains(Waiter waiter, String expectedLog, int attempt) {
        System.out.println("Attempt: " + attempt);
        List<String> actualLogs = service.getOwner().getLogs();
        if (!actualLogs.stream().anyMatch(line -> line.contains(expectedLog))) {
            System.out.println("Not found expected log. Trying again. Attempt: " + attempt);
            int newAttempt = attempt + 1;
            executorService.schedule(() -> assertContains(waiter, expectedLog, newAttempt), POLL_SECONDS, TimeUnit.SECONDS);
        } else {
            System.out.println("Found expected log! Attempt: " + attempt);
            waiter.assertTrue(true);
        }
    }
}
