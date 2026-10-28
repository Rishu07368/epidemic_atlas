package org.epidemicatlas.engine;

public final class Lattice {
    private Lattice() { }

    public static double laplacian(double[] values, int width, int height, int x, int y) {
        if (width < 1 || height < 1 || values.length != width * height
                || x < 0 || x >= width || y < 0 || y >= height)
            throw new IllegalArgumentException("Invalid grid or selected cell.");
        int index = y * width + x;
        double center = values[index];
        double result = 0;
        if (x > 0) result += values[index - 1] - center;
        if (x + 1 < width) result += values[index + 1] - center;
        if (y > 0) result += values[index - width] - center;
        if (y + 1 < height) result += values[index + width] - center;
        return result;
    }
}
