package org.jboss.eap.qe.microprofile.health.integration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDISystemPropertyHealthTest.SetupTask.class })
public class FailSafeCDISystemPropertyHealthTest extends FailSafeCDIHealthBaseTest {

    private static final PathAddress LIVE_SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress()
            .append(SYSTEM_PROPERTY, FailSafeDummyService.LIVE_CONFIG_PROPERTY);

    private static final PathAddress READY_SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress()
            .append(SYSTEM_PROPERTY, FailSafeDummyService.READY_CONFIG_PROPERTY);

    private static final PathAddress IN_MAINENANCE_SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress()
            .append(SYSTEM_PROPERTY, FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY);

    private static final PathAddress READY_IN_MAINENANCE_SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress()
            .append(SYSTEM_PROPERTY, FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY);

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, FailSafeCDISystemPropertyHealthTest.class.getSimpleName() + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    void setConfigProperty(PathAddress property, boolean value) throws IOException, ConfigurationException, CliException {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
        client.execute(String.format("/system-property=%s:write-attribute", property, value));
    }

    @Override
    void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance) throws Exception {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
        client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                FailSafeDummyService.LIVE_CONFIG_PROPERTY, live));
        client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                FailSafeDummyService.READY_CONFIG_PROPERTY, ready));
        client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintanance));
        client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMainenance));
        new Administration(ManagementClientProvider.onlineStandalone()).reload();
    }

    /**
     * Add system properties for MP Config
     */
    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
            client.execute(String.format("/system-property=%s:add", FailSafeDummyService.LIVE_CONFIG_PROPERTY));
            client.execute(String.format("/system-property=%s:add", FailSafeDummyService.READY_CONFIG_PROPERTY));
            client.execute(String.format("/system-property=%s:add", FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY));
            client.execute(String.format("/system-property=%s:add", FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
            client.execute(String.format("/system-property=%s:remove", FailSafeDummyService.LIVE_CONFIG_PROPERTY));
            client.execute(String.format("/system-property=%s:remove", FailSafeDummyService.READY_CONFIG_PROPERTY));
            client.execute(String.format("/system-property=%s:remove", FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY));
            client.execute(
                    String.format("/system-property=%s:remove", FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY));
        }
    }
}
