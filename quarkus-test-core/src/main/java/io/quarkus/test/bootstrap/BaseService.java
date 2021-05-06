package io.quarkus.test.bootstrap;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class BaseService<T extends Service> implements Service {

    private static final int SERVICE_WAITER_POLL_EVERY_SECONDS = 2;
    private static final int SERVICE_WAITER_TIMEOUT_MINUTES = 5;

    private final List<Action> onPreStartActions = new LinkedList<>();
    private final List<Action> onPostStartActions = new LinkedList<>();
    private final Map<String, String> properties = new HashMap<>();
    private final Map<String, String> resources = new HashMap<>();
    private final List<Runnable> futureProperties = new LinkedList<>();

    private ManagedResource managedResource;
    private String serviceName;
    private Configuration configuration;
    private ServiceContext context;

    public String getName() {
        return serviceName;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getHost() {
        return getHost(Protocol.HTTP);
    }

    public String getHost(Protocol protocol) {
        return managedResource.getHost(protocol);
    }

    public Integer getPort() {
        return getPort(Protocol.HTTP);
    }

    public Integer getPort(Protocol protocol) {
        return managedResource.getPort(protocol);
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Map<String, String> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    public T onPreStart(Action action) {
        onPreStartActions.add(action);
        return (T) this;
    }

    public T onPostStart(Action action) {
        onPostStartActions.add(action);
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperties(String... propertiesFiles) {
        properties.clear();
        Stream.of(propertiesFiles).map(PropertiesUtils::toMap).forEach(properties::putAll);
        return (T) this;
    }

    /**
     * Map the resource to the target service.
     */
    public T withResource(String resource, String to) {
        resources.put(resource, to);
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperty(String key, String value) {
        this.properties.put(key, value);
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperty(String key, Supplier<String> value) {
        futureProperties.add(() -> properties.put(key, value.get()));
        return (T) this;
    }

    /**
     * Start the managed resource. If the managed resource is running, it does
     * nothing.
     *
     * @throws RuntimeException when application errors at startup.
     */
    @Override
    public void start() {
        if (isManagedResourceRunning()) {
            return;
        }

        Log.debug(this, "Starting service");

        onPreStartActions.forEach(a -> a.handle(this));
        managedResource.start();
        waitUntilServiceIsStarted();
        onPostStartActions.forEach(a -> a.handle(this));
        Log.info(this, "Service started");
    }

    /**
     * Stop the Quarkus application.
     */
    @Override
    public void stop() {
        if (!isManagedResourceRunning()) {
            return;
        }

        Log.debug(this, "Stopping service");
        managedResource.stop();

        Log.info(this, "Service stopped");
    }

    @Override
    public void register(String serviceName) {
        this.serviceName = serviceName;
        this.configuration = Configuration.load(serviceName);
        onPreStart(s -> futureProperties.forEach(Runnable::run));
    }

    @Override
    public void init(ManagedResourceBuilder managedResourceBuilder, ServiceContext context) {
        this.context = context;
        Log.info(this, "Initialize service");
        FileUtils.recreateDirectory(context.getServiceFolder());
        managedResource = managedResourceBuilder.build(context);
    }

    public void restart() {
        managedResource.restart();
    }

    protected <U> U getPropertyFromContext(String key) {
        if (context == null) {
            fail("Service has not been initialized yet. Make sure you invoke this method in the right order.");
        }

        return context.get(key);
    }

    private void waitUntilServiceIsStarted() {
        await().pollInterval(SERVICE_WAITER_POLL_EVERY_SECONDS, TimeUnit.SECONDS)
                .atMost(SERVICE_WAITER_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .until(this::isManagedResourceRunning);
    }

    private boolean isManagedResourceRunning() {
        Log.debug(this, "Checking if resource is running");
        boolean isRunning = managedResource.isRunning();
        if (isRunning) {
            Log.debug(this, "Resource is running");
        } else {
            Log.debug(this, "Resource is not running");
        }

        return isRunning;
    }
}
