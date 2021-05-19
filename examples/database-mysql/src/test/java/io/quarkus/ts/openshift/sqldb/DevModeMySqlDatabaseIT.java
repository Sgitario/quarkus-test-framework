package io.quarkus.ts.openshift.sqldb;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

/**
 * Running Quarkus on DEV mode will spin up a Database instance automatically.
 */
@QuarkusScenario
public class DevModeMySqlDatabaseIT extends AbstractSqlDatabaseIT {

    @DevModeQuarkusApplication
    static RestService app = new RestService();

    @Override
    protected RestService getApp() {
        return app;
    }

    @Test
    public void verifyLogsToAssertDevMode() {
        app.logs().assertContains("Profile dev activated. Live Coding activated");
    }
}
