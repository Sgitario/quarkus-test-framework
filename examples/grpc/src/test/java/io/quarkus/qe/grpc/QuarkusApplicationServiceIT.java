package io.quarkus.qe.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusApplicationService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class QuarkusApplicationServiceIT {

    static final String NAME = "Victor";

    @QuarkusApplication(grpc = true)
    static final QuarkusApplicationService app = new QuarkusApplicationService();

    @Test
    public void shouldHelloWorldServiceWork() {
        HelloRequest request = HelloRequest.newBuilder().setName(NAME).build();
        HelloReply response = GreeterGrpc.newBlockingStub(app.grpc().grpcChannel()).sayHello(request);

        assertEquals("Hello " + NAME, response.getMessage());
    }
}
