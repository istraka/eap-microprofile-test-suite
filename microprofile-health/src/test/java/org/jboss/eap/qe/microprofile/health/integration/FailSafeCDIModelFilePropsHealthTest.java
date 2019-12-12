package org.jboss.eap.qe.microprofile.health.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
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

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDIModelFilePropsHealthTest.SetupTask.class })
public class FailSafeCDIModelFilePropsHealthTest extends FailSafeCDIHealthBaseTest {

    private byte[] liveBytes;
    private byte[] readyBytes;
    private byte[] inMaintananceBytes;
    private byte[] readyInMaintananceBytes;

    File liveFile = new File(
            FailSafeCDIModelFilePropsHealthTest.class.getResource("file-props/" + FailSafeDummyService.LIVE_CONFIG_PROPERTY)
                    .getFile());

    File readyFile = new File(
            FailSafeCDIModelFilePropsHealthTest.class.getResource("file-props/" + FailSafeDummyService.READY_CONFIG_PROPERTY)
                    .getFile());

    File inMainenanceFile = new File(
            FailSafeCDIModelFilePropsHealthTest.class
                    .getResource("file-props/" + FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY).getFile());

    File readyInMainenanceFile = new File(
            FailSafeCDIModelFilePropsHealthTest.class
                    .getResource("file-props/" + FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY).getFile());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, FailSafeCDIModelPropsHealthTest.class + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Before
    public void backup() throws IOException {
        liveBytes = FileUtils.readFileToByteArray(liveFile);
        readyBytes = FileUtils.readFileToByteArray(readyFile);
        inMaintananceBytes = FileUtils.readFileToByteArray(inMainenanceFile);
        readyInMaintananceBytes = FileUtils.readFileToByteArray(inMainenanceFile);
    }

    @After
    public void restore() throws IOException {
        FileUtils.writeByteArrayToFile(liveFile, liveBytes);
        FileUtils.writeByteArrayToFile(readyFile, readyBytes);
        FileUtils.writeByteArrayToFile(inMainenanceFile, inMaintananceBytes);
        FileUtils.writeByteArrayToFile(readyInMainenanceFile, readyInMaintananceBytes);
    }

    @Override
    void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance) throws Exception {
        FileUtils.writeStringToFile(liveFile, Boolean.toString(live));
        FileUtils.writeStringToFile(readyFile, Boolean.toString(ready));
        FileUtils.writeStringToFile(inMainenanceFile, Boolean.toString(inMaintanance));
        FileUtils.writeStringToFile(readyInMainenanceFile, Boolean.toString(readyInMainenance));
        new Administration(ManagementClientProvider.onlineStandalone()).reload();

    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from a directory defined in the subsystem.
     * The directory contains files (filenames are mapped to MP Config properties name) which contains config values.
     */
    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            String dir = SetupTask.class.getResource("file-props").getFile();
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
            client.execute(
                    String.format("/subsystem=microprofile-config-smallrye/config-source=file-props:add(dir={path=%s})", dir));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
            client.execute("/subsystem=microprofile-config-smallrye/config-source=file-props:remove");
        }
    }
}
