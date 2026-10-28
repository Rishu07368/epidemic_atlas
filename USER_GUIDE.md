# User guide

## First experiment

1. Launch Epidemic Atlas and keep the default uniform population, rates, 32 × 22 grid, and duration.
2. Click a cell on the map. At day zero, this chooses the proposed localized seed.
3. Select **Apply parameters**. An error is shown if the localized infection count exceeds that cell's population or the model/time-step values are invalid.
4. Select **Run** to animate calculated steps, or **Step forward** for one deterministic step. While running, the same control becomes **Pause**; playback speed changes only how often the UI advances, not the model step.
5. Choose a map variable; click cells to inspect S/I/R counts and proportions, β/γ, and the infection Laplacian.
6. Open **Laplacian explorer** for neighbour-by-neighbour contributions. Open **Model comparison**, then choose **Compare models** to calculate three infected trajectories using consistent initial totals.
7. Save a versioned scenario or export the computed aggregate time series/grid at the selected time as CSV.

## Inputs and meanings

- Population is the grid-wide total, divided equally among cells.
- Initial infected are placed at the selected cell; initial recovered are divided equally.
- β and γ are per day; movement is per day. All are nonnegative.
- Duration and time step are days. The duration is reached exactly; the last step can be shorter.
- A selected cell at zero population is safe; local incidence is zero there.

The application shows model-generated results only. The heatmap uses each current state and its own maximum for color scaling; inspect exact values numerically in the cell panel.

## Save, reload, and exports

Use **Save scenario** to create a versioned Properties file; **Load scenario** restores its grid, rates, time settings, and seed coordinate, then resets the simulation. The deterministic model has no random seed. CSV time-series export contains only computed rows reached so far; grid export records every cell's current S/I/R and total.

## Controls

- **Run / pause / resume:** controls playback.
- **Step forward:** advances one solver step.
- **Apply parameters:** validates current controls and initializes a new run.
- **Compare models:** runs well-mixed, independent-cell, and coupled-grid trajectories from the same population and initial compartment totals.

For equation conventions, numerical behavior, and limitations see [MATHEMATICAL_MODEL.md](MATHEMATICAL_MODEL.md), [NUMERICAL_METHODS.md](NUMERICAL_METHODS.md), and [SCIENTIFIC_LIMITATIONS.md](SCIENTIFIC_LIMITATIONS.md).
