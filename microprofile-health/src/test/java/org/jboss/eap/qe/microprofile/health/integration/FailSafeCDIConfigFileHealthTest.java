package org.jboss.eap.qe.microprofile.health.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * MP Config properties are defined in microprofile-config.properties in META INF. Since properties are immutable,
 * only some of test cases are valid.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(MicroProfileFTSetupTask.class)
public class FailSafeCDIConfigFileHealthTest extends FailSafeCDIHealthBaseTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        String content = String.format("%s=%s\n%s=%s\n%s=%s\n%s=%s",
                FailSafeDummyService.LIVE_CONFIG_PROPERTY, false,
                FailSafeDummyService.READY_CONFIG_PROPERTY, true,
                FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, true,
                FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, false);

        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, FailSafeCDIConfigFileHealthTest.class.getSimpleName() + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsManifestResource(new StringAsset(content),
                        "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    @Override
    public void testHealthEndpointUp() {
    }

    @Override
    public void testLivenessEndpointUp() {
    }

    @Override
    public void testReadinessEndpointUp() {
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance) {
        if (!(!live && ready && inMaintanance && !readyInMainenance)) {
            throw new RuntimeException("Invalid scenario!");
        }
    }

}
