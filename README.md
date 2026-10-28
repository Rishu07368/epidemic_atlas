# Epidemic Atlas

Epidemic Atlas is an offline desktop laboratory for exploring deterministic SIR epidemics and conservative movement on a two-dimensional grid. Its map and plots are rendered from arrays calculated by the simulation engine; there are no prerecorded animations or sample-result heatmaps.

> **Educational use only.** This software is not an outbreak surveillance, forecasting, clinical, or public-health decision tool.

## What works in this release

- Classical frequency-dependent SIR equations integrated with fourth-order Runge–Kutta (RK4).
- A grid of local SIR populations plus conservative, symmetric, four-neighbour movement of **each** compartment.
- Click-to-select initial infection cell, cell inspection, no-flux edge handling, and selectable computed heatmaps.
- Start/pause, one-step, reset, and playback-speed controls. Playback speed changes rendering cadence, not the numerical time step.
- Shared discrete Laplacian values with individual neighbour contributions in the Laplacian Explorer.
- Three computed infected curves: well-mixed SIR, independent local cells, and movement-coupled cells.
- Versioned local scenario files and CSV exports for aggregate time series and a selected-time grid.
- Windows packaging and CI definitions; see [DEPLOYMENT.md](DEPLOYMENT.md).

## Run from source

Requirements: JDK 21 or newer and Maven 3.9+. The Maven compiler targets Java 21. The interface uses Swing from the JDK rather than JavaFX to avoid an additional UI runtime and keep the first release small and fully offline.

```powershell
mvn clean verify
mvn package
java -jar target\epidemic-atlas.jar
```

Default experiment: a uniform 24,000-person grid with one localized seed of 20 infections, β = 0.32/day, γ = 0.10/day, movement rate 0.08/day, duration 120 days, and step 0.2 day. Click a map cell to set the seed, then select **Reset / apply** before starting. Inputs are validated before a run. At most 100,000 time steps and a 200 × 200 grid are accepted.

## Ordinary Windows users

Use the portable ZIP or MSI from a verified GitHub release when one is published. Extract the ZIP and run `EpidemicAtlas\EpidemicAtlas.exe`; no separate Java installation is required. This repository does not yet have a published release or verified installer. Build locally using the instructions in [DEPLOYMENT.md](DEPLOYMENT.md).

## Scientific model at a glance

The well-mixed and per-cell model use frequency-dependent transmission:

`dS/dt = -βSI/N`, `dI/dt = βSI/N - γI`, `dR/dt = γI`, where `N = S + I + R`.

β and γ have units day⁻¹; `R₀ = β/γ` when γ > 0 under the fully susceptible, closed-population assumptions. Grid coupling uses the graph Laplacian `Δu = Σ(neighbour − me)` with existing orthogonal neighbours and no flux beyond the grid. A symmetric pairwise flux moves susceptible, infected, and recovered people at the same configurable rate. See [MATHEMATICAL_MODEL.md](MATHEMATICAL_MODEL.md), [NUMERICAL_METHODS.md](NUMERICAL_METHODS.md), and [SPATIAL_MODELLING.md](SPATIAL_MODELLING.md).

The model represents continuous-valued population quantities, not discrete people. Spatial movement is an educational nearest-neighbour mixing rule, not calibrated human mobility. There are no births, deaths, imports, interventions, stochasticity, real maps, or local parameter fields in this release.

## Data, privacy, and reproducibility

Scenarios are user-selected `.properties` files with a format version and all current model inputs. CSV files contain computed output. The app does not contact a server or use external map tiles. Files are saved only where the user selects. A deterministic run has no random seed because it uses no random process.

## Documentation

- [User guide](USER_GUIDE.md)
- [Architecture](ARCHITECTURE.md)
- [Mathematical model](MATHEMATICAL_MODEL.md)
- [Numerical methods](NUMERICAL_METHODS.md)
- [Spatial modelling](SPATIAL_MODELLING.md)
- [Scientific limitations](SCIENTIFIC_LIMITATIONS.md)
- [Testing](TESTING.md)
- [Deployment](DEPLOYMENT.md)
- [Security](SECURITY.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

No screenshots are included because no desktop capture has been verified in this environment. No release links are claimed until a tagged build is published.
