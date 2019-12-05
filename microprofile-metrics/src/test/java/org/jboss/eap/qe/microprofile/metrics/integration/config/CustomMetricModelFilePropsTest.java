package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricModelFilePropsTest.SetupTask.class)
public class CustomMetricModelFilePropsTest extends CustomMetricBaseTest {

    private static final PathAddress CONFIG_SOURCE_PROPS_ADDRESS = PathAddress.pathAddress()
            .append(SUBSYSTEM, "microprofile-config-smallrye")
            .append("config-source", "file-props");

    private byte[] bytes;

    File incrementFile = new File(
            CustomMetricModelFilePropsTest.class.getResource("file-props/" + INCREMENT_CONFIG_PROPERTY).getFile());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, CustomMetricModelFilePropsTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    @Before
    public void backup() throws IOException {
        bytes = FileUtils.readFileToByteArray(incrementFile);
    }

    @After
    public void restore() throws IOException {
        FileUtils.writeByteArrayToFile(incrementFile, bytes);
    }

    void setConfigProperties(int increment) throws IOException {
        FileUtils.writeStringToFile(incrementFile, Integer.toString(increment));
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode dir = new ModelNode();
            dir.get("path").set(SetupTask.class.getResource("file-props").getFile());
            ModelNode add = Util.createAddOperation(CONFIG_SOURCE_PROPS_ADDRESS);
            add.get("dir").set(dir);
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(add)
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(Util.createRemoveOperation(CONFIG_SOURCE_PROPS_ADDRESS))
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }
    }
}
