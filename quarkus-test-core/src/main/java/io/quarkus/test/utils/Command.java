package io.quarkus.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;

public class Command {

    private static final PropertyLookup LOCK_FOLDER_PROPERTY = new PropertyLookup("ts.command.lock.folder", "target/");
    private static final String LOCK_SUFFIX = ".lock";
    private static final long LOCK_TIMEOUT_MINUTES = 5;
    private static final long LOCK_LOOP_INTERVAL_SECONDS = 5;

    private final String command;

    private List<String> arguments;
    private BiConsumer<String, InputStream> outputConsumer = consoleOutput();
    private String directory = ".";
    private boolean lockingAccess = false;
    private FileLock fileLock;

    public Command(String command, List<String> arguments) {
        this(command);
        this.arguments = arguments;
    }

    public Command(String command) {
        this.command = command;
    }

    public Command arguments(String... arguments) {
        return arguments(Arrays.asList(arguments));
    }

    public Command arguments(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Command outputToConsole() {
        outputConsumer = consoleOutput();
        return this;
    }

    public Command outputToList(List<String> list) {
        outputConsumer = listOutput(list);
        return this;
    }

    public Command onDirectory(Path path) {
        directory = path.toString();
        return this;
    }

    /**
     * Locking the access to the command, the framework will ensure that no other external access will use the same
     * program at the same time.
     */
    public Command lockAccess() {
        lockingAccess = true;
        return this;
    }

    /**
     * Unlocking the access to this command, the framework will not check whether other external executions are using
     * the same program at the same time.
     */
    public Command unlockAccess() {
        lockingAccess = false;
        return this;
    }

    public void runAndWait() throws IOException, InterruptedException {
        waitUntilProgramIsUnlockedIfEnabled();

        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command);
        fullCommand.addAll(arguments);

        Log.info("Running command: %s", String.join(" ", fullCommand));
        Process process = new ProcessBuilder().redirectErrorStream(true).command(fullCommand)
                .directory(new File(directory).getAbsoluteFile()).start();

        new Thread(() -> outputConsumer.accept(command, process.getInputStream()),
                "stdout consumer for command " + command).start();

        int result = process.waitFor();
        unlockProgramAccessIfEnabled();

        if (result != 0) {
            throw new RuntimeException(command + " failed (executed " + command + ", return code " + result + ")");
        }
    }

    private void unlockProgramAccessIfEnabled() {
        if (lockingAccess && fileLock != null) {
            try {
                fileLock.release();
                fileLock = null;
            } catch (IOException ignored) {
            }
        }
    }

    private void waitUntilProgramIsUnlockedIfEnabled() {
        if (lockingAccess) {
            String lockFileName = StringUtils.appendIfMissing(LOCK_FOLDER_PROPERTY.get(), "/") + command + LOCK_SUFFIX;
            fileLock = Awaitility.await()
                    .ignoreExceptions()
                    .pollInterval(LOCK_TIMEOUT_MINUTES, TimeUnit.SECONDS)
                    .atMost(LOCK_LOOP_INTERVAL_SECONDS, TimeUnit.MINUTES)
                    .until(() -> {
                        RandomAccessFile accessFile = new RandomAccessFile(lockFileName, "rw");
                        FileChannel fileChannel = accessFile.getChannel();
                        return fileChannel.tryLock();
                    }, Objects::nonNull);
        }
    }

    private static BiConsumer<String, InputStream> consoleOutput() {
        return (description, is) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.info("%s: %s", description, line);
                }
            } catch (IOException ignored) {
            }
        };
    }

    private static BiConsumer<String, InputStream> listOutput(List<String> list) {
        return (description, is) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
            } catch (IOException ignored) {
            }
        };
    }
}
