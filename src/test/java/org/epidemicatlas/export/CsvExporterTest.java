package org.epidemicatlas.export;

import org.epidemicatlas.engine.SpatialParameters;
import org.epidemicatlas.engine.SpatialSimulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {
    @TempDir Path temp;

    @Test
    void exportsComputedTimeSeriesAndGridValues() throws Exception {
        SpatialParameters parameters = new SpatialParameters(1, 1, new double[] {10},
                new double[] {1}, new double[] {0}, 0.2, 0.1, 0, 1, 0.5);
        List<org.epidemicatlas.engine.SpatialState> states = new SpatialSimulator(parameters).simulate();
        Path series = temp.resolve("series.csv");
        CsvExporter.writeTimeSeries(series, states.stream().map(state -> new double[] {
                state.time(), state.totalSusceptible(), state.totalInfected(),
                state.totalRecovered(), state.totalPopulation()}).toList());
        assertTrue(Files.readString(series).contains("time,susceptible,infected,recovered,total_population"));
        assertTrue(Files.readString(series).contains("10.0"));
        Path grid = temp.resolve("grid.csv");
        CsvExporter.writeGrid(grid, parameters, states.get(0));
        assertTrue(Files.readString(grid).contains("0,0,9.0,1.0,0.0,10.0"));
    }
}
