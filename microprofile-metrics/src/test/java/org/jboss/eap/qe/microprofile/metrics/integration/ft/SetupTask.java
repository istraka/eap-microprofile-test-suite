package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

public class SetupTask implements ServerSetupTask {
    private static final String TEST_MODULE_NAME = "test.ft-custom-config-source";
    private static final String PROPERTY_FILENAME = "ft-custom-metric.properties";
    public static final File propertyFile = new File(SetupTask.class.getResource(PROPERTY_FILENAME).getFile());
    private static final PathAddress SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress().append(SYSTEM_PROPERTY,
            FTCustomConfigSource.FILENAME_PROPERTY);
    private static final PathAddress FT_EXTENSION_ADDRESS = PathAddress.pathAddress().append(EXTENSION,
            "org.wildfly.extension.microprofile.fault-tolerance-smallrye");
    private static final PathAddress FT_SUBSYSTEM_ADDRESS = PathAddress.pathAddress().append(SUBSYSTEM,
            "microprofile-fault-tolerance-smallrye");
    private TestModule testModule;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        setupSystemProperty(managementClient);
        setupModule(managementClient);
        addConfigSource(managementClient.getControllerClient());
        addFTSubsystem(managementClient.getControllerClient());
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        removeSystemProperty(managementClient);
        removeConfigSource(managementClient.getControllerClient());
        removeFTSubsystem(managementClient.getControllerClient());
        testModule.remove();
    }

    private void addFTSubsystem(ModelControllerClient client) throws IOException {
        client.execute(Util.createAddOperation(FT_EXTENSION_ADDRESS));
        client.execute(Util.createAddOperation(FT_SUBSYSTEM_ADDRESS));
    }

    private void removeFTSubsystem(ModelControllerClient client) throws IOException {
        client.execute(Util.createRemoveOperation(FT_EXTENSION_ADDRESS));
        client.execute(Util.createRemoveOperation(FT_SUBSYSTEM_ADDRESS));
    }

    private void setupSystemProperty(ManagementClient managementClient) throws IOException {
        ModelNode addFilenameProperty = Util.createAddOperation(SYSTEM_PROPERTY_ADDRESS);
        addFilenameProperty.get("value").set(SetupTask.class.getResource(PROPERTY_FILENAME).getFile());
        Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                .getControllerClient()
                .execute(addFilenameProperty)
                .get(ClientConstants.OUTCOME)
                .asString());
    }

    private void setupModule(ManagementClient managementClient) throws Exception {
        URL url = SetupTask.class.getResource("module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule(TEST_MODULE_NAME, moduleXmlFile);
        testModule.addResource("ft-config-source.jar")
                .addClass(FTCustomConfigSource.class);
        testModule.create();
    }

    private void addConfigSource(ModelControllerClient client) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
        op.get(OP_ADDR).add("config-source", "cs-from-class");
        op.get(OP).set(ADD);
        op.get("class").get("module").set(TEST_MODULE_NAME);
        op.get("class").get("name").set(FTCustomConfigSource.class.getName());
        client.execute(op);
    }

    private void removeConfigSource(ModelControllerClient client) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
        op.get(OP_ADDR).add("config-source", "cs-from-class");
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
