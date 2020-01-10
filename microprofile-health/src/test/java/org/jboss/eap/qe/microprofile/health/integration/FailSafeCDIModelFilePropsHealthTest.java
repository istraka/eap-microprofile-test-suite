package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property is provided by file-props model option - values are stored in files on FS.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDIModelFilePropsHealthTest.SetupTask.class })
public class FailSafeCDIModelFilePropsHealthTest extends FailSafeCDIHealthBaseTest {

    private byte[] liveBytes;
    private byte[] readyBytes;
    private byte[] inMaintananceBytes;
    private byte[] readyInMaintananceBytes;

    Path liveFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class.getResource("file-props/" + FailSafeDummyService.LIVE_CONFIG_PROPERTY)
                    .getPath());

    Path readyFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class.getResource("file-props/" + FailSafeDummyService.READY_CONFIG_PROPERTY)
                    .getFile());

    Path inMainenanceFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class
                    .getResource("file-props/" + FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY).getPath());

    Path readyInMainenanceFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class
                    .getResource("file-props/" + FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY).getPath());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, FailSafeCDIModelPropsHealthTest.class + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Before
    public void backup() throws IOException {
        liveBytes = Files.readAllBytes(liveFile);
        readyBytes = Files.readAllBytes(readyFile);
        inMaintananceBytes = Files.readAllBytes(inMainenanceFile);
        readyInMaintananceBytes = Files.readAllBytes(inMainenanceFile);
    }

    @After
    public void restore() throws IOException {
        Files.write(liveFile, liveBytes);
        Files.write(readyFile, readyBytes);
        Files.write(inMainenanceFile, inMaintananceBytes);
        Files.write(readyInMainenanceFile, readyInMaintananceBytes);
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance)
            throws Exception {
        Files.write(liveFile, Boolean.toString(live).getBytes(StandardCharsets.UTF_8));
        Files.write(readyFile, Boolean.toString(ready).getBytes(StandardCharsets.UTF_8));
        Files.write(inMainenanceFile, Boolean.toString(inMaintanance).getBytes(StandardCharsets.UTF_8));
        Files.write(readyInMainenanceFile, Boolean.toString(readyInMainenance).getBytes(StandardCharsets.UTF_8));

        new Administration(ManagementClientProvider.onlineStandalone(managementClient)).reload();
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from a directory defined in the subsystem.
     * The directory contains files (filenames are mapped to MP Config properties name) which contains config values.
     */
    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            String dir = SetupTask.class.getResource("file-props").getFile();
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
            client.execute(
                    String.format("/subsystem=microprofile-config-smallrye/config-source=file-props:add(dir={path=%s})",
                            dir))
                    .assertSuccess();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
            client.execute("/subsystem=microprofile-config-smallrye/config-source=file-props:remove").assertSuccess();
        }
    }
}
