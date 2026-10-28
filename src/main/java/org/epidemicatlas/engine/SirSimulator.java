package org.epidemicatlas.engine;

import java.util.ArrayList;
import java.util.List;

public final class SirSimulator {
    private SirSimulator() { }

    public static List<SirState> simulate(SirParameters parameters) {
        List<SirState> states = new ArrayList<>();
        double s = parameters.initialSusceptible();
        double i = parameters.initialInfected();
        double r = parameters.initialRecovered();
        double time = 0;
        states.add(new SirState(time, s, i, r));
        while (time < parameters.duration()) {
            double h = Math.min(parameters.timeStep(), parameters.duration() - time);
            double[] next = rk4(s, i, r, parameters.beta(), parameters.gamma(),
                    parameters.population(), h);
            requireValid(next, time + h);
            s = next[0];
            i = next[1];
            r = next[2];
            time += h;
            if (parameters.duration() - time < parameters.timeStep() * 1e-10)
                time = parameters.duration();
            states.add(new SirState(time, s, i, r));
        }
        return List.copyOf(states);
    }

    static double[] rk4(double s, double i, double r, double beta, double gamma,
                        double population, double h) {
        double[] k1 = derivative(s, i, beta, gamma, population);
        double[] k2 = derivative(s + h * k1[0] / 2, i + h * k1[1] / 2, beta, gamma, population);
        double[] k3 = derivative(s + h * k2[0] / 2, i + h * k2[1] / 2, beta, gamma, population);
        double[] k4 = derivative(s + h * k3[0], i + h * k3[1], beta, gamma, population);
        return new double[] {
                s + h * (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0]) / 6,
                i + h * (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1]) / 6,
                r + h * (k1[2] + 2 * k2[2] + 2 * k3[2] + k4[2]) / 6
        };
    }

    static double[] derivative(double s, double i, double beta, double gamma, double population) {
        double infection = population == 0 ? 0 : beta * s * i / population;
        double recovery = gamma * i;
        return new double[] {-infection, infection - recovery, recovery};
    }

    static void requireValid(double[] state, double time) {
        for (double value : state) {
            if (!Double.isFinite(value) || value < 0)
                throw new ArithmeticException("Numerical instability at t=" + time
                        + ". Reduce the time step and retry.");
        }
    }
}
