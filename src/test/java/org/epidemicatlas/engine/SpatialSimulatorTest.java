package org.epidemicatlas.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpatialSimulatorTest {
    @Test
    void laplacianMatchesFourNeighbourDefinitionAndNoFluxBoundary() {
        double[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        assertEquals(0, Lattice.laplacian(values, 3, 3, 1, 1), 1e-12);
        assertEquals(4, Lattice.laplacian(values, 3, 3, 0, 0), 1e-12);
        assertEquals(-4, Lattice.laplacian(values, 3, 3, 2, 2), 1e-12);
        assertEquals(0, Lattice.laplacian(new double[] {4}, 1, 1, 0, 0), 1e-12);
    }

    @Test
    void movementConservesEachCompartmentAndPopulation() {
        SpatialParameters parameters = parameters(3, 2, new double[] {100, 100, 100, 100, 100, 100},
                new double[] {10, 0, 0, 0, 0, 0}, new double[6], 0, 0, 0.2, 3, 0.1);
        SpatialState initial = new SpatialSimulator(parameters).state();
        List<SpatialState> states = new SpatialSimulator(parameters).simulate();
        SpatialState end = states.get(states.size() - 1);
        assertEquals(initial.totalSusceptible(), end.totalSusceptible(), 1e-9);
        assertEquals(initial.totalInfected(), end.totalInfected(), 1e-9);
        assertEquals(initial.totalRecovered(), end.totalRecovered(), 1e-9);
        assertEquals(initial.totalPopulation(), end.totalPopulation(), 1e-9);
        assertTrue(end.infected(1) > 0);
    }

    @Test
    void isolatedGridCellRunsLocalSirWithoutDiffusion() {
        SpatialParameters parameters = parameters(1, 1, new double[] {100},
                new double[] {5}, new double[] {0}, 0.2, 0.1, 0.8, 2, 0.1);
        List<SpatialState> states = new SpatialSimulator(parameters).simulate();
        assertTrue(states.get(states.size() - 1).infected(0) > 5);
        assertEquals(100, states.get(states.size() - 1).totalPopulation(), 1e-9);
    }

    @Test
    void emptyCellsRemainFiniteAndNoPeopleAppear() {
        SpatialParameters parameters = parameters(2, 1, new double[] {0, 0},
                new double[] {0, 0}, new double[] {0, 0}, 0.5, 0.1, 0.2, 1, 0.1);
        SpatialState end = new SpatialSimulator(parameters).simulate().get(10);
        assertEquals(0, end.totalPopulation());
        assertTrue(Double.isFinite(end.infected(0)));
    }

    @Test
    void rejectsMovementStepThatCouldProduceNegativePopulation() {
        assertThrows(IllegalArgumentException.class, () -> parameters(3, 1,
                new double[] {10, 10, 10}, new double[] {1, 0, 0}, new double[3],
                0.2, 0.1, 1, 1, 0.6));
    }

    @Test
    void movementUpdateMatchesSharedLaplacianFlux() {
        SpatialParameters parameters = parameters(2, 1, new double[] {10, 10},
                new double[] {4, 0}, new double[2], 0, 0, 0.1, 0.1, 0.1);
        SpatialState end = new SpatialSimulator(parameters).step();
        assertEquals(4 + 0.1 * 0.1 * Lattice.laplacian(new double[] {4, 0}, 2, 1, 0, 0),
                end.infected(0), 1e-12);
        assertEquals(0.04, end.infected(1), 1e-12);
    }

    @Test
    void fullGridHistoryIsBoundedForLargeRuns() {
        double[] population = fill(40_000, 10);
        double[] infected = new double[40_000];
        infected[0] = 1;
        SpatialParameters parameters = parameters(200, 200, population, infected,
                new double[40_000], 0, 0.1, 0, 1000, 0.01);
        assertThrows(IllegalStateException.class, () -> new SpatialSimulator(parameters).simulate());
    }

    @Test
    void repeatedRunsAreDeterministicAndPreservePerCellNonnegativity() {
        SpatialParameters parameters = parameters(4, 3, fill(12, 100), seed(12, 20),
                new double[12], 0.3, 0.12, 0.08, 12, 0.1);
        List<SpatialState> first = new SpatialSimulator(parameters).simulate();
        List<SpatialState> second = new SpatialSimulator(parameters).simulate();
        assertEquals(first.size(), second.size());
        for (int k = 0; k < first.size(); k++) {
            for (int c = 0; c < first.get(k).size(); c++) {
                assertEquals(first.get(k).infected(c), second.get(k).infected(c), 0);
                assertTrue(first.get(k).susceptible(c) >= 0);
                assertTrue(first.get(k).infected(c) >= 0);
                assertTrue(first.get(k).recovered(c) >= 0);
            }
        }
    }

    private static SpatialParameters parameters(int width, int height, double[] population,
                                                double[] infected, double[] recovered, double beta,
                                                double gamma, double movement, double duration, double step) {
        return new SpatialParameters(width, height, population, infected, recovered, beta, gamma,
                movement, duration, step);
    }

    private static double[] fill(int size, double value) {
        double[] values = new double[size];
        java.util.Arrays.fill(values, value);
        return values;
    }

    private static double[] seed(int size, double value) {
        double[] values = new double[size];
        values[0] = value;
        return values;
    }
}
