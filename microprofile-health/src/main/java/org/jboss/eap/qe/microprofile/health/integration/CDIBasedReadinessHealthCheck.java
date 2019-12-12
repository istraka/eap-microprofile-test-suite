package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
public class CDIBasedReadinessHealthCheck implements HealthCheck {

    @Inject
    FailSafeDummyService dummyService;

    @Override
    public HealthCheckResponse call() {
        try {
            return HealthCheckResponse.named("dummyReadiness")
                    .state(dummyService.isReady())
                    .build();
        } catch (IOException e) {
            throw new Error("dummy service is should be fail safe");
        }
    }
}
