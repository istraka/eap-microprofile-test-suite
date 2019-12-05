package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.module.util.TestModule;
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
@ServerSetup(CustomMetricCustomConfigSourceProviderTest.SetupTask.class)
public class CustomMetricCustomConfigSourceProviderTest extends CustomMetricBaseTest {
    private static final PathAddress SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress()
            .append(SYSTEM_PROPERTY, CustomConfigSource.FILENAME_PROPERTY);
    private static final String PROPERTY_FILENAME = "custom-metric.properties";

    private byte[] bytes;

    File propertyFile = new File(CustomMetricCustomConfigSourceProviderTest.class.getResource(PROPERTY_FILENAME).getFile());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, CustomMetricCustomConfigSourceProviderTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
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

    void setConfigProperties(int increment) throws IOException {
        FileUtils.writeStringToFile(propertyFile, INCREMENT_CONFIG_PROPERTY + "=" + Integer.toString(increment));
    }

    public static class SetupTask implements ServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.custom-config-source-provider";

        private static TestModule testModule;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            setupSystemProperty(managementClient);
            setupModule(managementClient);
            addConfigSourceProvider(managementClient.getControllerClient());
        }

        public void setupSystemProperty(ManagementClient managementClient) throws IOException {
            ModelNode addFilenameProperty = Util.createAddOperation(SYSTEM_PROPERTY_ADDRESS);
            addFilenameProperty.get("value").set(SetupTask.class.getResource(PROPERTY_FILENAME).getFile());
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(addFilenameProperty)
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }

        public void setupModule(ManagementClient managementClient) throws Exception {
            URL url = SetupTask.class.getResource("configSourceProviderModule.xml");
            File moduleXmlFile = new File(url.toURI());
            testModule = new TestModule(TEST_MODULE_NAME, moduleXmlFile);
            testModule.addResource("config-source-provider.jar")
                    .addClass(CustomConfigSourceProvider.class)
                    .addClass(CustomConfigSource.class);
            testModule.create();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            removeSystemProperty(managementClient);
            removeConfigSourceProvider(managementClient.getControllerClient());
            testModule.remove();
        }

        private void addConfigSourceProvider(ModelControllerClient client) throws IOException {
            ModelNode op;
            op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source-provider", "my-config-source-config_source_provider");
            op.get(OP).set(ADD);
            op.get("class").get("module").set(TEST_MODULE_NAME);
            op.get("class").get("name").set(CustomConfigSourceProvider.class.getName());
            client.execute(op);
        }

        private void removeConfigSourceProvider(ModelControllerClient client) throws IOException {
            ModelNode op;
            op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source-provider", "my-config-source-config_source_provider");
            op.get(OP).set(REMOVE);
            client.execute(op);
        }

        private void removeSystemProperty(ManagementClient managementClient) throws IOException {
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(Util.createRemoveOperation(SYSTEM_PROPERTY_ADDRESS))
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }
    }
}
