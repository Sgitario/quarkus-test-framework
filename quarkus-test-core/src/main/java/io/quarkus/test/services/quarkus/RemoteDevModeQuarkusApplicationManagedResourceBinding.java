package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;

public interface RemoteDevModeQuarkusApplicationManagedResourceBinding {
    /**
     * @param context
     * @return if the current managed resource applies for the current context.
     */
    boolean appliesFor(ServiceContext context);

    /**
     * Init and return the managed resource for the current context.
     *
     * @param builder
     * @return
     */
    QuarkusManagedResource init(RemoteDevModeQuarkusApplicationManagedResourceBuilder builder);
}
