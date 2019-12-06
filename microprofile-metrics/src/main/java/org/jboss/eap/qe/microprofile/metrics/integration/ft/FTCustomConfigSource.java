package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class FTCustomConfigSource implements ConfigSource {
    public static final String FILENAME_PROPERTY = "ft-config.source.properties.path";

    @Override
    public Map<String, String> getProperties() {
        System.out.println("==================> About to read properties from the disk...");
        String filename = System.getProperty(FILENAME_PROPERTY);
        Map<String, String> props = new HashMap<>();
        try (FileInputStream is = new FileInputStream(filename)) {
            if (filename == null) {
                throw new RuntimeException(FILENAME_PROPERTY + " property not defined");
            }
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                props.put(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("==================> Success!");
        return Collections.unmodifiableMap(props);
    }

    @Override
    public String getValue(String s) {
        return getProperties().get(s);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }
}
