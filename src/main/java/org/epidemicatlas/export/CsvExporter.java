package org.epidemicatlas.export;

import org.epidemicatlas.engine.SpatialParameters;
import org.epidemicatlas.engine.SpatialState;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() { }

    public static void writeTimeSeries(Path path, List<double[]> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("time,susceptible,infected,recovered,total_population");
            writer.newLine();
            for (double[] row : rows) {
                if (row.length != 5) throw new IllegalArgumentException("Each time-series row must contain five values.");
                writer.write(row[0] + "," + row[1] + "," + row[2] + "," + row[3] + "," + row[4]);
                writer.newLine();
            }
        }
    }

    public static void writeGrid(Path path, SpatialParameters parameters, SpatialState state)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("x,y,susceptible,infected,recovered,population");
            writer.newLine();
            for (int y = 0; y < parameters.height(); y++) {
                for (int x = 0; x < parameters.width(); x++) {
                    int c = y * parameters.width() + x;
                    writer.write(x + "," + y + "," + state.susceptible(c) + ","
                            + state.infected(c) + "," + state.recovered(c) + ","
                            + state.total(c));
                    writer.newLine();
                }
            }
        }
    }
}
