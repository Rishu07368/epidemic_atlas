# Testing

Run the unit and packaging-independent verification suite with:

```powershell
mvn clean verify
```

JUnit 5 tests cover:

- Frequency-dependent SIR derivatives, derivative population balance, zero-infection/empty-cell behavior, RK4 against the closed-form no-transmission recovery solution, R₀, invalid initial conditions, and numerical instability rejection.
- Four-neighbour Laplacians and no-flux boundaries, conservation of each transported compartment, isolated-cell dynamics, empty grids, movement-step validation, nonnegative states, and deterministic repeatability.
- Scenario format-version round trips and rejection of unknown versions.
- CSV time series and grid output derived from actual engine states.

Windows packaging is defined separately in `scripts\package-windows.ps1` and `.github\workflows\windows.yml`. CI performs the packaging step on a Windows runner; local validation should execute the generated app image outside the IDE. A successful Maven test alone does not establish that the installer launches.

Tests for pause/resume UI interaction, corrupted scenario values in every possible form, clean-room offline builds, accessibility, and actual user-data persistence after installation remain manual/future validation. See the final delivery status reported with each release.
