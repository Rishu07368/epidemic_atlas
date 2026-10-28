package org.epidemicatlas.engine;

public final class SpatialState {
    private final double time;
    private final double[] susceptible;
    private final double[] infected;
    private final double[] recovered;

    SpatialState(double time, double[] susceptible, double[] infected, double[] recovered) {
        this.time = time;
        this.susceptible = susceptible.clone();
        this.infected = infected.clone();
        this.recovered = recovered.clone();
    }

    public double time() { return time; }
    public int size() { return infected.length; }
    public double susceptible(int cell) { return susceptible[cell]; }
    public double infected(int cell) { return infected[cell]; }
    public double recovered(int cell) { return recovered[cell]; }
    public double total(int cell) { return susceptible[cell] + infected[cell] + recovered[cell]; }
    public double[] susceptibleValues() { return susceptible.clone(); }
    public double[] infectedValues() { return infected.clone(); }
    public double[] recoveredValues() { return recovered.clone(); }
    public double totalSusceptible() { return sum(susceptible); }
    public double totalInfected() { return sum(infected); }
    public double totalRecovered() { return sum(recovered); }
    public double totalPopulation() { return totalSusceptible() + totalInfected() + totalRecovered(); }

    private static double sum(double[] values) {
        double total = 0;
        for (double value : values) total += value;
        return total;
    }
}
