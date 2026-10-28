package org.epidemicatlas.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SirSimulatorTest {
    @Test
    void derivativesMatchFrequencyDependentSirEquationsAndConservePopulation() {
        double[] derivative = SirSimulator.derivative(90, 8, 0.3, 0.1, 100);
        assertEquals(-2.16, derivative[0], 1e-12);
        assertEquals(1.36, derivative[1], 1e-12);
        assertEquals(0.8, derivative[2], 1e-12);
        assertEquals(0, derivative[0] + derivative[1] + derivative[2], 1e-12);
    }

    @Test
    void zeroInfectionAndEmptyCellHaveZeroIncidence() {
        assertArrayEquals(new double[] {0, 0, 0},
                SirSimulator.derivative(0, 0, 0.5, 0.2, 0), 0);
        assertArrayEquals(new double[] {0, 0, 0},
                SirSimulator.derivative(100, 0, 0.5, 0.2, 100), 1e-12);
    }

    @Test
    void runPreservesPopulationAndApproximatesKnownNoTransmissionSolution() {
        SirParameters parameters = new SirParameters(1000, 20, 0, 0, 0.2, 20, 0.05);
        List<SirState> states = SirSimulator.simulate(parameters);
        SirState end = states.get(states.size() - 1);
        assertEquals(1000, end.total(), 1e-9);
        assertEquals(20 * Math.exp(-4), end.infected(), 1e-7);
        assertEquals(980, end.susceptible(), 1e-9);
    }

    @Test
    void basicReproductionNumberUsesBetaOverGamma() {
        assertEquals(3, new SirParameters(100, 1, 0, 0.3, 0.1, 1, 0.1)
                .basicReproductionNumber(), 1e-12);
        assertEquals(Double.POSITIVE_INFINITY,
                new SirParameters(100, 1, 0, 0.3, 0, 1, 0.1).basicReproductionNumber());
    }

    @Test
    void invalidInitialConditionsAndUnstableNumericalResultAreReported() {
        assertThrows(IllegalArgumentException.class,
                () -> new SirParameters(10, 7, 4, 0.2, 0.1, 5, 0.1));
        assertThrows(ArithmeticException.class,
                () -> SirSimulator.requireValid(new double[] {-1, 3, 8}, 1));
    }
}
