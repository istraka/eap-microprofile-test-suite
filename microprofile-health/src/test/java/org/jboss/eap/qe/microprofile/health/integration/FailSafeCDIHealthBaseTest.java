package org.jboss.eap.qe.microprofile.health.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.http.ContentType;
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
        healthRequest.get().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("dummyLiveness", "dummyReadiness"));
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
        healthRequest.basePath("health/live").get().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));
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
        checks.add(Map.of("name", "dummyLiveness", "status", "UP"));
        checks.add(Map.of("name", "dummyReadiness", "status", "DOWN"));
        healthRequest.basePath("health/ready").get().then()
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
        Map<String, String> liveCheck = Map.of("name", "dummyLiveness", "status", "DOWN");
        Map<String, String> readyCheck = Map.of("name", "dummyReadiness", "status", "DOWN");
        healthRequest.get().then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(2),
                        "checks", containsInAnyOrder(liveCheck, readyCheck));
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
        healthRequest.basePath("health/live").get().then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));
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
        healthRequest.basePath("health/ready").get().then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));
    }
}
