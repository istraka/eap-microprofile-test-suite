package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.metrics.annotation.Counted;

public class FailSafeDummyService {

    public static final String LIVE_CONFIG_PROPERTY = "dummy.live";
    public static final String READY_CONFIG_PROPERTY = "dummy.ready";
    public static final String IN_MAINTENANCE_CONFIG_PROPERTY = "dummy.in_maintenance";
    public static final String READY_IN_MAINTENANCE_CONFIG_PROPERTY = "dummy.in_maintenance.ready";

    @Inject
    @ConfigProperty(name = READY_CONFIG_PROPERTY)
    private Provider<Boolean> ready;

    @Inject
    @ConfigProperty(name = LIVE_CONFIG_PROPERTY)
    private Provider<Boolean> live;

    @Inject
    @ConfigProperty(name = READY_IN_MAINTENANCE_CONFIG_PROPERTY)
    private Provider<Boolean> readyInMainenance;

    @Inject
    @ConfigProperty(name = IN_MAINTENANCE_CONFIG_PROPERTY)
    private Provider<Boolean> inMaintanance;

    public boolean isLive() {
        return live.get();
    }

    @Fallback(fallbackMethod = "isReadyFallback")
    @Retry(maxRetries = 5)
    public boolean isReady() throws IOException {
        simulateOpeningResources();
        return ready.get();
    }

    public boolean isReadyFallback() {
        return readyInMainenance.get();
    }

    @Counted(name = "opening-resources-counter")
    private void simulateOpeningResources() throws IOException {
        if (inMaintanance.get()) {
            throw new IOException("In maintanance");
        }
    }
}
