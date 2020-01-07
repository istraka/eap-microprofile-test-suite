package org.jboss.eap.qe.microprofile.health.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDIModelPropsHealthTest.SetupTask.class })
public class FailSafeCDIModelPropsHealthTest extends FailSafeCDIHealthBaseTest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, FailSafeCDIModelPropsHealthTest.class + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance)
            throws Exception {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
        client.execute(String.format(
                "/subsystem=microprofile-config-smallrye/config-source=props:write-attribute(name=properties, value={%s=%s,%s=%s,%s=%s,%s=%s}",
                FailSafeDummyService.LIVE_CONFIG_PROPERTY, live,
                FailSafeDummyService.READY_CONFIG_PROPERTY, ready,
                FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintanance,
                FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMainenance));
        new Administration(client).reload();
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from properties defined in the subsystem
     */
    public static class SetupTask implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ManagementClientProvider.onlineStandalone(managementClient)
                    .execute("/subsystem=microprofile-config-smallrye/config-source=props:add");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            ManagementClientProvider.onlineStandalone(managementClient)
                    .execute("/subsystem=microprofile-config-smallrye/config-source=props:remove");
        }
    }
}
