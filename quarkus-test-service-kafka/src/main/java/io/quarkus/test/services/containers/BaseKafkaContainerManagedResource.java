package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.logging.TestContainersLoggingHandler;

public abstract class BaseKafkaContainerManagedResource extends DockerContainerManagedResource {

    private static final String SERVER_PROPERTIES = "server.properties";

    protected final KafkaContainerManagedResourceBuilder model;

    private GenericContainer<?> schemaRegistry;
    private TestContainersLoggingHandler schemaRegistryLoggingHandler;
    private Network network;

    protected BaseKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model.getContext());

        this.model = model;
    }

    protected abstract GenericContainer<?> initKafkaContainer();

    protected abstract GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka);

    @Override
    public void start() {
        super.start();

        startRegistryIfEnabled();
    }

    @Override
    public void stop() {
        super.stop();

        stopRegistryIfEnabled();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() && (!model.isWithRegistry() || isRegistryRunning());
    }

    protected String getKafkaVersion() {
        return StringUtils.defaultIfBlank(model.getVersion(), model.getVendor().getDefaultVersion());
    }

    protected String getKafkaRegistryImage() {
        return model.getRegistryImageVersion();
    }

    protected int getKafkaRegistryPort() {
        return model.getVendor().getRegistry().getPort();
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> kafkaContainer = initKafkaContainer();

        String kafkaConfigPath = model.getKafkaConfigPath();
        if (StringUtils.isNotEmpty(getServerProperties())) {
            kafkaContainer.withCopyFileToContainer(MountableFile.forClasspathResource(getServerProperties()),
                    kafkaConfigPath + SERVER_PROPERTIES);
        }

        for (String resource : getKafkaConfigResources()) {
            kafkaContainer.withCopyFileToContainer(MountableFile.forClasspathResource(resource), kafkaConfigPath + resource);
        }

        if (model.isWithRegistry()) {
            schemaRegistry = initRegistryContainer(kafkaContainer);
            schemaRegistryLoggingHandler = new TestContainersLoggingHandler(model.getContext().getOwner(), schemaRegistry);

            // Setup common network for kafka and the registry
            network = Network.newNetwork();
            kafkaContainer.withNetwork(network);
            schemaRegistry.withNetwork(network);
        }

        return kafkaContainer;
    }

    @Override
    protected int getTargetPort() {
        return model.getVendor().getPort();
    }

    protected String[] getKafkaConfigResources() {
        return model.getKafkaConfigResources();
    }

    protected String getServerProperties() {
        return model.getServerProperties();
    }

    private void startRegistryIfEnabled() {
        if (model.isWithRegistry()) {
            schemaRegistryLoggingHandler.startWatching();

            if (!isRegistryRunning()) {
                schemaRegistry.start();
            }

            model.getContext().put(KafkaService.KAFKA_REGISTRY_URL_PROPERTY, getSchemaRegistryUrl());
        }
    }

    private void stopRegistryIfEnabled() {
        if (model.isWithRegistry() && isRegistryRunning()) {
            schemaRegistryLoggingHandler.stopWatching();
            schemaRegistry.stop();
        }

        if (network != null) {
            network.close();
        }
    }

    private boolean isRegistryRunning() {
        return schemaRegistry != null && schemaRegistry.isRunning();
    }

    private String getSchemaRegistryUrl() {
        String path = StringUtils.defaultIfBlank(model.getRegistryPath(), model.getVendor().getRegistry().getPath());
        String containerIp = schemaRegistry.getContainerIpAddress();
        return String.format("http://%s:%s%s", containerIp, schemaRegistry.getMappedPort(getKafkaRegistryPort()), path);
    }

}
