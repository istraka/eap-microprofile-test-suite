package org.jboss.eap.qe.microprofile.health.integration;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

public abstract class FailSafeCDIHealthBaseTest {

    @ContainerResource
    ManagementClient managementClient;

    RequestSpecification healthRequest;

    @Before
    public void before() throws ConfigurationException, InterruptedException, TimeoutException, IOException {
        healthRequest = given().baseUri("http://" + managementClient.getMgmtAddress())
                .port(managementClient.getMgmtPort())
                .basePath("health");
        new Administration(ManagementClientProvider.onlineStandalone()).reload();
    }

    abstract void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance)
            throws Exception;

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testHealthEndpointUp() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("dummyLiveness", "dummyReadiness"));

        MetricsChecker.get()
                .validateSimulationCounter(1)
                .validateInvocationsTotal(1)
                .validateRetryCallsSucceededNotTriedTotal(1);

        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("dummyLiveness", "dummyReadiness"));

        MetricsChecker.get()
                .validateSimulationCounter(2)
                .validateInvocationsTotal(2)
                .validateRetryCallsSucceededNotTriedTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testLivenessEndpointUp() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));

        MetricsChecker.get()
                .validateSimulationCounter(1)
                .validateInvocationsTotal(1)
                .validateRetryCallsSucceededNotTriedTotal(1);

        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));

        MetricsChecker.get()
                .validateSimulationCounter(2)
                .validateInvocationsTotal(2)
                .validateRetryCallsSucceededNotTriedTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testReadinessEndpointUp() throws Exception {
        setConfigProperties(true, true, false, false);
        List<Map<String, String>> checks = new ArrayList<>(3);
        // TODO Java 11 Map<String, String> liveCheck = Map.of( "name", "dummyLiveness", "status", "UP");
        checks.add(Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyLiveness");
                put("status", "UP");
            }
        }));
        checks.add(Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyReadiness");
                put("status", "DOWN");
            }
        }));
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status is down and fallback method is invoked.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testHealthEndpointDownInMaintenace() throws Exception {
        setConfigProperties(false, true, true, false);
        // TODO Java 11 Map<String, String> liveCheck = Map.of( "name", "dummyLiveness", "status", "DOWN");
        Map<String, String> liveCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyLiveness");
                put("status", "DOWN");
            }
        });
        Map<String, String> readyCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyReadiness");
                put("status", "DOWN");
            }
        });
        get(HealthUrlProvider.healthEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(2),
                        "checks", containsInAnyOrder(liveCheck, readyCheck));

        MetricsChecker.get()
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
                .validateInvocationsTotal(1)
                .validateInvocationsFailedTotal(1)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES) // N retries
                .validateRetryCallsFailedTotal(1)
                .validateRetryCallsSucceededTotal(0)
                .validateFallbackCallsTotal(1);

        get(HealthUrlProvider.healthEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(2),
                        "checks", containsInAnyOrder(liveCheck, readyCheck));

        MetricsChecker.get()
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
                .validateInvocationsTotal(2)
                .validateInvocationsFailedTotal(2)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2) // 2N retries
                .validateRetryCallsFailedTotal(2)
                .validateRetryCallsSucceededTotal(0)
                .validateFallbackCallsTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status is down and fallback method is invoked.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testLivenessEndpointDownInMaintenace() throws Exception {
        setConfigProperties(false, true, true, false);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));

        MetricsChecker.get()
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
                .validateInvocationsTotal(1)
                .validateInvocationsFailedTotal(1)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES) // N retries
                .validateRetryCallsFailedTotal(1)
                .validateRetryCallsSucceededTotal(0)
                .validateFallbackCallsTotal(1);

        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));

        MetricsChecker.get()
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
                .validateInvocationsTotal(2)
                .validateInvocationsFailedTotal(2)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2) // 2N retries
                .validateRetryCallsFailedTotal(2)
                .validateRetryCallsSucceededTotal(0)
                .validateFallbackCallsTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status is down and fallback method is invoked.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testReadinessEndpointDownInMaintenace() throws Exception {
        setConfigProperties(false, true, true, false);
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        MetricsChecker.get()
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
                .validateInvocationsTotal(1)
                .validateInvocationsFailedTotal(1)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES) // N retries
                .validateRetryCallsFailedTotal(1)
                .validateRetryCallsSucceededTotal(0)
                .validateFallbackCallsTotal(1);

        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        MetricsChecker.get()
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
                .validateInvocationsTotal(2)
                .validateInvocationsFailedTotal(2)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2) // 2N retries
                .validateRetryCallsFailedTotal(2)
                .validateRetryCallsSucceededTotal(0)
                .validateFallbackCallsTotal(2);
    }

    public static class MetricsChecker {
        public static MetricsChecker get() throws ConfigurationException {
            return new MetricsChecker();
        }

        private final ValidatableResponse response;

        private MetricsChecker() throws ConfigurationException {
            ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                    ArquillianDescriptorWrapper.getArquillianDescriptor());
            String url = "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                    + "/metrics";

            response = given()
                    .baseUri(url)
                    .accept(ContentType.JSON)
                    .get()
                    .then();
        }

        public MetricsChecker validateSimulationCounter(int simulationCount) {
            response.body("application.simulation-count", equalTo(simulationCount));
            return this;
        }

        public MetricsChecker validateRetryRetriesTotal(int retriesTotal) {
            response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.retry.retries.total'",
                    equalTo(retriesTotal));
            return this;
        }

        public MetricsChecker validateRetryCallsFailedTotal(int callsFailedTotal) {
            response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.retry.callsFailed.total'",
                    equalTo(callsFailedTotal));
            return this;
        }

        public MetricsChecker validateRetryCallsSucceededTotal(int callsSucceededTotal) {
            response.body(
                    "application.'ft." + FailSafeDummyService.class.getName() + ".isReady.retry.callsSucceededRetried.total'",
                    equalTo(callsSucceededTotal));
            return this;
        }

        public MetricsChecker validateRetryCallsSucceededNotTriedTotal(int callsSucceededNotTriedTotal) {
            response.body(
                    "application.'ft." + FailSafeDummyService.class.getName()
                            + ".isReady.retry.callsSucceededNotRetried.total'",
                    equalTo(callsSucceededNotTriedTotal));
            return this;
        }

        public MetricsChecker validateInvocationsTotal(int invocationsTotal) {
            response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.invocations.total'",
                    equalTo(invocationsTotal));
            return this;
        }

        public MetricsChecker validateInvocationsFailedTotal(int invocationsFailedTotal) {
            response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.invocations.failed.total'",
                    equalTo(invocationsFailedTotal));
            return this;
        }

        public MetricsChecker validateFallbackCallsTotal(int fallbackCallsTotal) {
            response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.invocations.total'",
                    equalTo(fallbackCallsTotal));
            return this;
        }

    }
}
