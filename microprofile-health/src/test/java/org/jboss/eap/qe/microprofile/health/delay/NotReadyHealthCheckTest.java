package org.jboss.eap.qe.microprofile.health.delay;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.DOWN_NO_CHECK;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.DOWN_WITH_CHECK;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.START;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.UNABLE_TO_CONNECT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.health.ManualTests;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(ManualTests.class)
@RunWith(Arquillian.class)
public class NotReadyHealthCheckTest {

    @ArquillianResource
    ContainerController controller;

    private ArquillianContainerProperties arquillianContainerProperties;
    private ExecutorService executorService;

    public static File deployment() {
        File deployment;
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            deployment = new File(tempDirectory.toFile(), NotReadyHealthCheckTest.class.getSimpleName() + ".war");
        } catch (IOException e) {
            e.printStackTrace();
            deployment = new File(NotReadyHealthCheckTest.class.getSimpleName() + ".war");
        }

        ShrinkWrap.create(WebArchive.class, NotReadyHealthCheckTest.class.getSimpleName() + ".war")
                .addClasses(DelayedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(ZipExporter.class)
                .exportTo(deployment, true);

        return deployment;
    }

    @Deployment(managed = false)
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void after() {
        executorService.shutdownNow();
    }

    @Test
    public void delayedConstructorTest()
            throws IOException, ConfigurationException, InterruptedException, TimeoutException, ExecutionException {
        File source = deployment();
        File dest = new File(System.getProperty("jboss.home") + "/standalone/deployments/" + source.getName());
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ReadinessChecker readinessChecker = new ReadinessChecker();
        Future<Boolean> readinessCheckerFuture = executorService.submit(readinessChecker);

        controller.start(ManualTests.ARQUILLIAN_CONTAINER);

        // ready check is installed
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(503)
                .body("status", is("DOWN"),
                        "checks.name", containsInAnyOrder(DelayedReadinessHealthCheck.NAME));

        controller.stop(ManualTests.ARQUILLIAN_CONTAINER);

        readinessChecker.stop();
        Assert.assertTrue(readinessCheckerFuture.get(5, TimeUnit.SECONDS));

        ReadinessStatesValidator.of(readinessChecker)
                .finishedPropertly()
                .containSequence(START(), UNABLE_TO_CONNECT(), DOWN_NO_CHECK(), DOWN_WITH_CHECK());

        Files.delete(dest.toPath());
    }

}
