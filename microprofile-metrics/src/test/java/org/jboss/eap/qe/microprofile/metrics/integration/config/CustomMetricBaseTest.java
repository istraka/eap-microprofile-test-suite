package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;

public abstract class CustomMetricBaseTest {

    public static final String INCREMENT_CONFIG_PROPERTY = "dummy.increment";

    public static final int DEFAULT_VALUE = 2;

    private static final PathAddress CONFIG_SOURCE_PROPS_ADDRESS = PathAddress.pathAddress()
            .append(SUBSYSTEM, "microprofile-config-smallrye")
            .append("config-source", "props");

    @ArquillianResource
    URL deploymentUrl;

    @ContainerResource
    ManagementClient managementClient;

    String metricsURL;

    @Before
    public void before() {
        metricsURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    abstract void setConfigProperties(int number) throws Exception;

    /**
     * @tpTestDetails Multi-component customer scenario to verify correct behaviour when custom counter metric is used.
     *                The metric increment depends on CDI bean and MP Config property value.
     * @tpPassCrit Counter metric has correct value
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCustomMetricDefault() throws Exception {
        setConfigProperties(DEFAULT_VALUE);
        performRequest();
        testMetricForValue(DEFAULT_VALUE);
    }

    /**
     * @tpTestDetails High level customer scenario to verify correct behaviour when custom counter metric is used.
     *                The metric increment depends on CDI bean and MP Config property value.
     * @tpPassCrit Counter metric has correct value
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCustomMetricWichChange() throws Exception {
        setConfigProperties(5);
        performRequest();
        testMetricForValue(5);
        performRequest();
        testMetricForValue(10);
    }

    private void testMetricForValue(int value) {
        given()
                .baseUri("http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics")
                .accept(ContentType.JSON)
                .get()
                .then()
                .body("application.custom-metric", equalTo(value));
    }

    private void performRequest() {
        get(deploymentUrl.toString()).then()
                .statusCode(200)
                .body(equalTo("Hello from custom metric service!"));
    }

}
