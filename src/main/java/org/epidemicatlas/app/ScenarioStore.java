package org.epidemicatlas.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class ScenarioStore {
    private static final String VERSION = "1";

    private ScenarioStore() { }

    static void save(Path path, Properties properties) throws IOException {
        properties.setProperty("formatVersion", VERSION);
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "Epidemic Atlas scenario; model-generated educational experiment");
        }
    }

    static Properties load(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }
        if (!VERSION.equals(properties.getProperty("formatVersion")))
            throw new IOException("Unsupported scenario format version.");
        return properties;
    }
}
