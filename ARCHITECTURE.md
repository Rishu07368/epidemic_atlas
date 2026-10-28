# Architecture

## Runtime shape

The repository is one Maven Java module (`org.epidemicatlas:epidemic-atlas`) targeting Java 21. It intentionally uses Swing and AWT from `java.desktop`, rather than JavaFX: this avoids separate UI dependencies and makes the runtime image smaller and the offline build/package path simpler. The domain/engine packages have no Swing dependencies.

## Packages

- `engine`: immutable parameter and state types; well-mixed SIR equations and RK4; grid movement, snapshots, and no-flux discrete Laplacian.
- `app`: Swing window and controls, plus versioned scenario Properties persistence.
- `export`: CSV writers for aggregate time series and one grid snapshot.

The UI constructs inputs, creates a simulation, and displays engine states. `SpatialState` copies its arrays at the API boundary, so the canvas and engine do not share mutable grid arrays. Simulation code can be tested without initializing Swing.

## Data flow

1. The user sets aggregate population, initial recovered/infected, grid size, rates, duration, and step.
2. The app converts this into a uniform total-population grid and puts the initial infection at the selected cell. Initial recovered population is distributed uniformly.
3. `SpatialSimulator.step()` integrates each cell's local SIR reaction, then applies pairwise conservative movement to each compartment.
4. The canvas, cell inspector, chart, comparison, and CSV exporters read the computed state/snapshots.
5. Scenario files preserve the current uniform-grid configuration, selected seed location, boundary convention, and model format version.

## Threading and resource bounds

The first release runs bounded grid steps from a Swing timer on the event-dispatch thread. Grid dimensions are limited to 200 × 200 and a run to 100,000 steps. This keeps the implementation simple, but very large grids or stiff settings can make interaction sluggish; a worker/cancellation design is future work. There is no unbounded executor or per-cell UI object.

The chart stores aggregate values, not full grid history. Time-series export stores five aggregate doubles per completed step. Full grid snapshots are exported only on request. The convenience API that records every full-grid state rejects histories exceeding one million cell-time snapshots.

## Deliberate scope limits

There is no database, geographic projection, stochastic engine, configurable population raster, local β/γ map, barrier editor, PDF report, or scenario comparison editor. The model comparison is a reproducible three-curve infected-count comparison for the current aggregate inputs. These absences are documented rather than represented as available features.
