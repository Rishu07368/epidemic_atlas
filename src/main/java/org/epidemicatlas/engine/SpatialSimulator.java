package org.epidemicatlas.engine;

import java.util.ArrayList;
import java.util.List;

public final class SpatialSimulator {
    private final SpatialParameters parameters;
    private double[] susceptible;
    private double[] infected;
    private double[] recovered;
    private double time;

    public SpatialSimulator(SpatialParameters parameters) {
        this.parameters = parameters;
        susceptible = new double[parameters.size()];
        infected = new double[parameters.size()];
        recovered = new double[parameters.size()];
        for (int c = 0; c < parameters.size(); c++) {
            susceptible[c] = parameters.susceptible(c);
            infected[c] = parameters.infected(c);
            recovered[c] = parameters.recovered(c);
        }
    }

    public SpatialState state() {
        return new SpatialState(time, susceptible, infected, recovered);
    }

    public boolean finished() {
        return time >= parameters.duration();
    }

    public SpatialState step() {
        if (finished()) return state();
        double h = Math.min(parameters.timeStep(), parameters.duration() - time);
        double[] nextS = new double[parameters.size()];
        double[] nextI = new double[parameters.size()];
        double[] nextR = new double[parameters.size()];
        for (int c = 0; c < parameters.size(); c++) {
            double n = susceptible[c] + infected[c] + recovered[c];
            double[] result = SirSimulator.rk4(susceptible[c], infected[c], recovered[c],
                    parameters.beta(), parameters.gamma(), n, h);
            SirSimulator.requireValid(result, time + h);
            nextS[c] = result[0];
            nextI[c] = result[1];
            nextR[c] = result[2];
        }
        move(nextS, h);
        move(nextI, h);
        move(nextR, h);
        validate(nextS, nextI, nextR, time + h);
        susceptible = nextS;
        infected = nextI;
        recovered = nextR;
        time += h;
        if (parameters.duration() - time < parameters.timeStep() * 1e-10)
            time = parameters.duration();
        return state();
    }

    public List<SpatialState> simulate() {
        long snapshotCount = (long) Math.ceil(parameters.duration() / parameters.timeStep()) + 1;
        if (snapshotCount * parameters.size() > 1_000_000)
            throw new IllegalStateException("Full-grid history is too large to retain safely. "
                    + "Advance the simulator with step() and retain only required outputs.");
        List<SpatialState> states = new ArrayList<>();
        states.add(state());
        while (!finished()) states.add(step());
        return List.copyOf(states);
    }

    private void move(double[] values, double h) {
        if (parameters.movementRate() == 0) return;
        double[] moved = values.clone();
        double amount = h * parameters.movementRate();
        int width = parameters.width();
        int height = parameters.height();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = y * width + x;
                if (x + 1 < width) exchange(values, moved, c, c + 1, amount);
                if (y + 1 < height) exchange(values, moved, c, c + width, amount);
            }
        }
        System.arraycopy(moved, 0, values, 0, values.length);
    }

    private static void exchange(double[] original, double[] moved, int a, int b, double amount) {
        double flux = amount * (original[a] - original[b]);
        moved[a] -= flux;
        moved[b] += flux;
    }

    private static void validate(double[] s, double[] i, double[] r, double atTime) {
        for (int c = 0; c < s.length; c++) {
            if (!Double.isFinite(s[c]) || !Double.isFinite(i[c]) || !Double.isFinite(r[c])
                    || s[c] < 0 || i[c] < 0 || r[c] < 0)
                throw new ArithmeticException("Invalid compartment in cell " + c + " at t=" + atTime
                        + ". Reduce the time step and retry.");
        }
    }
}
