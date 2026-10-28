package org.epidemicatlas.engine;

public record SirState(double time, double susceptible, double infected, double recovered) {
    public double total() {
        return susceptible + infected + recovered;
    }
}
