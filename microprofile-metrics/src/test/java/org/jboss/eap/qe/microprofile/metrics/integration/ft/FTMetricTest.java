package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SetupTask.class)
public class FTMetricTest {
    public static final String INCREMENT_CONFIG_PROPERTY = "dummy.increment";
    public static final String FAULT_CTL_CONFIG_PROPERTY = "dummy.corrupted";
    public static final String FAILSAFE_INCREMENT_CONFIG_PROPERTY = "dummy.failsafe.increment";

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    URL deploymentUrl;

    private byte[] bytes;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, FTMetricTest.class.getSimpleName() + ".war")
                .addPackage(FTCustomMetricApplication.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void reload() {
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    @Before
    public void backup() throws IOException {
        bytes = FileUtils.readFileToByteArray(SetupTask.propertyFile);
    }

    @After
    public void restore() throws IOException {
        FileUtils.writeByteArrayToFile(SetupTask.propertyFile, bytes);
    }

    /**
     * @tpTestDetails Multi-component customer scenario to verify correct behaviour when custom counter metric is used.
     *                The metric increment depends on CDI bean and MP Config property values. Increment for the metric
     *                is provided by a fail-safe service. If one provider fails (controlled by MP Config property)
     *                the fail-safe service fallbacks to another provider.
     * @tpPassCrit Counter metric has correct value
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void incrementProviderTest() throws IOException {
        setMPConfig(false, 2, 3);
        performRequest();
        performRequest();
        testCustomMetricForValue(4);
        testInvocationsMetric(2);
        performRequest();
        performRequest();
        performRequest();
        testCustomMetricForValue(10);
        testInvocationsMetric(5);
    }

    /**
     * @tpTestDetails Multi-component customer scenario to verify correct behaviour when custom counter metric is used.
     *                The metric increment depends on CDI bean and MP Config property values. Increment for the metric
     *                is provided by a fail-safe service. If one provider fails (controlled by MP Config property)
     *                the fail-safe service fallbacks to another provider.
     * @tpPassCrit Counter metric has correct value
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void incrementFailSafeProviderTest() throws IOException {
        setMPConfig(false, 1, 3);
        performRequest();
        performRequest();
        testCustomMetricForValue(2);
        testInvocationsMetric(2);
        setMPConfig(true, 2, 4);
        performRequest();
        performRequest();
        performRequest();
        testCustomMetricForValue(14);
        testInvocationsMetric(5);
        testFallbackMetric(3);
        setMPConfig(false, 10, 6);
        performRequest();
        testCustomMetricForValue(24);
        testInvocationsMetric(6);
        testFallbackMetric(3);
    }

    private void setMPConfig(boolean providerCorrupted, int increment, int failSafeIncrement) throws IOException {
        String content = String.format("%s=%s\n%s=%s\n%s=%s",
                FAULT_CTL_CONFIG_PROPERTY, providerCorrupted,
                INCREMENT_CONFIG_PROPERTY, increment,
                FAILSAFE_INCREMENT_CONFIG_PROPERTY, failSafeIncrement);
        FileUtils.writeStringToFile(SetupTask.propertyFile, content);
    }

    private void testCustomMetricForValue(int value) {
        testAppCounterMetric("ft-custom-metric", value);
    }

    private void testInvocationsMetric(int value) {
        testAppCounterMetric(
                "ft." + FTCustomCounterIncrementProviderService.class.getName() + ".getIncrement.invocations.total", value);
    }

    private void testFallbackMetric(int value) {
        testAppCounterMetric(
                "ft." + FTCustomCounterIncrementProviderService.class.getName() + ".getIncrement.fallback.calls.total", value);
    }

    private void testAppCounterMetric(String name, int value) {
        given()
                .baseUri("http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics")
                .accept(ContentType.JSON)
                .get()
                .then()
                .body("application", hasKey(name),
                        "application.'" + name + "'", equalTo(value));

    }

    private void performRequest() {
        get(deploymentUrl.toString()).then()
                .statusCode(200)
                .body(equalTo("Hello from custom metric fault-tolerant service!"));
    }
}
