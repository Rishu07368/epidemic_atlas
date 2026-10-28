package org.epidemicatlas.engine;

public record SirParameters(double population, double initialInfected, double initialRecovered,
                            double beta, double gamma, double duration, double timeStep) {
    public SirParameters {
        requireFiniteNonnegative("population", population);
        requireFiniteNonnegative("initial infected", initialInfected);
        requireFiniteNonnegative("initial recovered", initialRecovered);
        requireFiniteNonnegative("transmission rate", beta);
        requireFiniteNonnegative("recovery rate", gamma);
        if (population <= 0) throw new IllegalArgumentException("Population must be greater than zero.");
        if (initialInfected + initialRecovered > population)
            throw new IllegalArgumentException("Initial infected and recovered cannot exceed population.");
        if (!Double.isFinite(duration) || duration <= 0)
            throw new IllegalArgumentException("Duration must be finite and greater than zero.");
        if (!Double.isFinite(timeStep) || timeStep <= 0 || timeStep > duration)
            throw new IllegalArgumentException("Time step must be positive and no greater than duration.");
        if (duration / timeStep > 100_000)
            throw new IllegalArgumentException("The run exceeds 100,000 time steps. Increase the time step or shorten duration.");
    }

    private static void requireFiniteNonnegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0)
            throw new IllegalArgumentException(name + " must be finite and nonnegative.");
    }

    public double initialSusceptible() {
        return population - initialInfected - initialRecovered;
    }

    public double basicReproductionNumber() {
        return gamma == 0 ? (beta == 0 ? 0 : Double.POSITIVE_INFINITY) : beta / gamma;
    }
}
