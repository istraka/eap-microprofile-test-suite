package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("/")
public class MetricsApp extends Application {

    @Path("/")
    public static class HelloResource {

        @Inject
        FailSafeDummyService service;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public boolean doGet() throws InterruptedException, IOException {
            return service.isReady();
        }
    }

    @Path("/2")
    public static class HelloResource2 {

        @Inject
        FailSafeDummyService service;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public boolean doGet() throws InterruptedException, IOException {
            service.simulateOpeningResources();
            return service.isReady();
        }
    }
}
