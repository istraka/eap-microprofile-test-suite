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
import org.wildfly.extras.creaper.commands.modules.RemoveModule;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDICustomConfigSourceHealthTest.SetupTask.class })
public class FailSafeCDICustomConfigSourceHealthTest extends FailSafeCDIHealthDynamicBaseTest {
    private static final String PROPERTY_FILENAME = "health.properties";
    File propertyFile = new File(FailSafeCDICustomConfigSourceHealthTest.class.getResource(PROPERTY_FILENAME).getFile());
    private byte[] bytes;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, FailSafeCDICustomConfigSourceHealthTest.class.getSimpleName() + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    @Before
    public void backup() throws IOException {
        bytes = FileUtils.readFileToByteArray(propertyFile);
    }

    @After
    public void restore() throws IOException {
        FileUtils.writeByteArrayToFile(propertyFile, bytes);
    }

    @Override
    void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance) throws Exception {
        String content = String.format("%s=%s\n%s=%s\n%s=%s\n%s=%s",
                FailSafeDummyService.LIVE_CONFIG_PROPERTY, live,
                FailSafeDummyService.READY_CONFIG_PROPERTY, ready,
                FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMainenance,
                FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintanance);
        FileUtils.writeStringToFile(propertyFile, content);
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from {@link CustomConfigSource}
     */
    static class SetupTask implements ServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.custom-config-source";

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
            client.execute(String.format("/system-property=%s:add(value=%s)", CustomConfigSource.FILEPATH_PROPERTY,
                    SetupTask.class.getResource(PROPERTY_FILENAME).getFile()));
            ModuleUtil.setupModule(client, new File(SetupTask.class.getResource("configSourceModule.xml").toURI()),
                    TEST_MODULE_NAME, "config-source", CustomConfigSource.class);
            client.execute(String.format(
                    "/subsystem=microprofile-config-smallrye/config-source=cs-from-class:add(class={module=%s, name=%s})",
                    TEST_MODULE_NAME, CustomConfigSource.class.getName()));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
            client.execute(String.format("/system-property=%s:remove", CustomConfigSource.FILEPATH_PROPERTY));
            client.execute("/subsystem=microprofile-config-smallrye/config-source=cs-from-class:remove");
            ManagementClientProvider.onlineStandalone().apply(new RemoveModule(TEST_MODULE_NAME));
        }
    }
}
