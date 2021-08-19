package io.quarkus.test.bootstrap;

import java.util.Arrays;
import java.util.List;

public class QuarkusApplicationService extends CompoundService {

    public QuarkusApplicationService()

    private RestService restService = new RestService();
    private GrpcService grpcService = new GrpcService();
    private DevModeQuarkusService devModeService = new DevModeQuarkusService();

    private List<BaseService<?>> allServices = Arrays.asList(restService, grpcService, devModeService);

    public RestService rest() {
        return restService;
    }

    public GrpcService grpc() {
        return grpcService;
    }

    public DevModeQuarkusService devMode() {
        return devModeService;
    }

    @Override
    public void start() {
        super.start();

        allServices.forEach(Service::start);
    }

    @Override
    public void init(ManagedResourceBuilder managedResourceBuilder) {
        super.init(managedResourceBuilder);

        allServices.forEach(s -> s.bind(this));
    }
}
