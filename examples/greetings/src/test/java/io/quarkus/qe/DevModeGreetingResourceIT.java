package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.DevModeQuarkusService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@QuarkusScenario
public class DevModeGreetingResourceIT {

    static final String DEFAULT_NAME = "victor";
    static final String JOSE_NAME = "jose";
    static final String MANUEL_NAME = "manuel";

    @DevModeQuarkusApplication
    static DevModeQuarkusService app = new DevModeQuarkusService();

    @Test
    public void shouldUpdateResourcesAndSources() {
        // Should say first Victor (the default name)
        app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + DEFAULT_NAME));

        // Modify default name to manuel
        app.modifyFile("src/main/java/io/quarkus/qe/GreetingResource.java",
                content -> content.replace(DEFAULT_NAME, MANUEL_NAME));

        // Now, the app should say Manuel
        AwaitilityUtils.untilAsserted(
                () -> app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + MANUEL_NAME)));

        // When copying the new application properties, live code should detect it
        app.copyFile("src/test/resources/jose.properties", "src/main/resources/application.properties");

        // Now, the app should say Jose
        AwaitilityUtils.untilAsserted(
                () -> app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + JOSE_NAME)));
    }
}
