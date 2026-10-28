package org.epidemicatlas.engine;

public final class SpatialParameters {
    private final int width;
    private final int height;
    private final double[] population;
    private final double[] infected;
    private final double[] recovered;
    private final double beta;
    private final double gamma;
    private final double movementRate;
    private final double duration;
    private final double timeStep;

    public SpatialParameters(int width, int height, double[] population, double[] infected,
                             double[] recovered, double beta, double gamma, double movementRate,
                             double duration, double timeStep) {
        if (width < 1 || height < 1 || width > 200 || height > 200)
            throw new IllegalArgumentException("Grid dimensions must be between 1 and 200.");
        int size = Math.multiplyExact(width, height);
        if (population.length != size || infected.length != size || recovered.length != size)
            throw new IllegalArgumentException("Grid arrays must match width × height.");
        this.width = width;
        this.height = height;
        this.population = population.clone();
        this.infected = infected.clone();
        this.recovered = recovered.clone();
        for (int c = 0; c < size; c++) {
            validNonnegative("population", population[c]);
            validNonnegative("infected", infected[c]);
            validNonnegative("recovered", recovered[c]);
            if (infected[c] + recovered[c] > population[c])
                throw new IllegalArgumentException("Initial compartments exceed population in cell " + c + ".");
        }
        validNonnegative("transmission rate", beta);
        validNonnegative("recovery rate", gamma);
        validNonnegative("movement rate", movementRate);
        if (!Double.isFinite(duration) || duration <= 0 || !Double.isFinite(timeStep)
                || timeStep <= 0 || timeStep > duration)
            throw new IllegalArgumentException("Duration and time step must be finite and positive; step ≤ duration.");
        if (duration / timeStep > 100_000)
            throw new IllegalArgumentException("The run exceeds 100,000 time steps. Increase the time step or shorten duration.");
        int maxDegree = width == 1 && height == 1 ? 0
                : Math.min(4, (width > 1 ? 2 : 0) + (height > 1 ? 2 : 0));
        if (movementRate * timeStep * maxDegree > 1 + 1e-12)
            throw new IllegalArgumentException("Movement is too large for this time step. Require rate × step × maxDegree ≤ 1.");
        this.beta = beta;
        this.gamma = gamma;
        this.movementRate = movementRate;
        this.duration = duration;
        this.timeStep = timeStep;
    }

    private static void validNonnegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0)
            throw new IllegalArgumentException(name + " must be finite and nonnegative.");
    }

    public int width() { return width; }
    public int height() { return height; }
    public int size() { return population.length; }
    public double population(int cell) { return population[cell]; }
    public double infected(int cell) { return infected[cell]; }
    public double recovered(int cell) { return recovered[cell]; }
    public double susceptible(int cell) { return population[cell] - infected[cell] - recovered[cell]; }
    public double beta() { return beta; }
    public double gamma() { return gamma; }
    public double movementRate() { return movementRate; }
    public double duration() { return duration; }
    public double timeStep() { return timeStep; }
}
