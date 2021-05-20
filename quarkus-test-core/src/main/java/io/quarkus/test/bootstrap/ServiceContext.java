package io.quarkus.test.bootstrap;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ServiceContext {
    private final Service owner;
    private final ExtensionContext testContext;
    private final Path serviceFolder;
    private final ExecutorService executorService;
    private final Map<String, Object> store = new HashMap<>();

    protected ServiceContext(Service owner, ExtensionContext testContext, ExecutorService executorService) {
        this.owner = owner;
        this.testContext = testContext;
        this.executorService = executorService;
        this.serviceFolder = new File("target", getName()).toPath();
    }

    public Service getOwner() {
        return owner;
    }

    public String getName() {
        return owner.getName();
    }

    public ExtensionContext getTestContext() {
        return testContext;
    }

    public Path getServiceFolder() {
        return serviceFolder;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void put(String key, Object value) {
        store.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) store.get(key);
    }

}
