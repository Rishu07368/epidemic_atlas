package org.epidemicatlas.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioStoreTest {
    @TempDir Path temp;

    @Test
    void savesAndLoadsVersionedScenario() throws Exception {
        Properties input = new Properties();
        input.setProperty("model", "spatial-sir");
        Path path = temp.resolve("scenario.properties");
        ScenarioStore.save(path, input);
        assertEquals("spatial-sir", ScenarioStore.load(path).getProperty("model"));
        assertEquals("1", ScenarioStore.load(path).getProperty("formatVersion"));
    }

    @Test
    void rejectsUnknownScenarioVersion() throws Exception {
        Path path = temp.resolve("old.properties");
        Files.writeString(path, "formatVersion=999\n");
        assertThrows(IOException.class, () -> ScenarioStore.load(path));
    }
}
