package io.quarkus.test.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.test.utils.ReflectionUtils;

public class CompoundService extends BaseService<CompoundService> {

    private RestService restService = new RestService();
    private GrpcService grpcService = new GrpcService();
    private DevModeQuarkusService devModeService = new DevModeQuarkusService();

    private final Map<Class<?>, BaseService<?>> allServices;

    public CompoundService(Class<BaseService<?>> ... classServices) {
        if (classServices == null) {
            throw new IllegalArgumentException("You provided no services. Don't use CompoundService when no inner services");
        }

        this.allServices = new HashMap<>(classServices.length);
        Stream.of(classServices)
                .map(ReflectionUtils::createInstance)
                .forEach(service -> this.allServices.put(service.getClass(), service));
    }

    public BaseService<?> as(Class<BaseService<?>> clazz) {
        BaseService<?> service = allServices.get(clazz);
        if (service == null) {
            throw new IllegalArgumentException("Service " + clazz.getName() + " not found");
        }

        return service;
    }

    @Override
    public void start() {
        super.start();

        allServices.values().forEach(Service::start);
    }

    @Override
    public void init(ManagedResourceBuilder managedResourceBuilder) {
        super.init(managedResourceBuilder);

        allServices.values().forEach(s -> s.bind(this));
    }
}
