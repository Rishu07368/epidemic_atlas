# Mathematical model

## Classical frequency-dependent SIR

For susceptible `S`, infected `I`, recovered/non-infectious `R`, and closed total population `N = S + I + R`:

```
dS/dt = -β S I / N
dI/dt =  β S I / N - γ I
dR/dt =  γ I
```

The model uses frequency-dependent transmission. Both β and γ have units day⁻¹ when time is measured in days. They are not parameters for the mass-action equation `βSI`; that alternative is not implemented. Empty cells use zero incidence rather than dividing by zero. The equations sum to zero, so local reaction conserves each cell's total population.

The basic reproduction number displayed by the application is `R₀ = β/γ` for γ > 0, with a fully susceptible population at the disease-free state, homogeneous mixing, and no intervention or importation. When γ = 0, it is reported as infinity for β > 0 and zero for β = 0.

## Spatial SIR

Each cell has local compartments and local SIR reaction using the cell's current total population. After reaction, each compartment is exchanged across every existing horizontal/vertical neighbouring edge using a symmetric flux. For an edge `(a,b)`, one step transfers a signed amount `m h (X_a − X_b)` from a to b, where `X` is S, I, or R, movement rate `m` is day⁻¹, and step length is `h` days. The exchange is conservative because the same flux is added to one cell and subtracted from the other.

Summing edge fluxes gives the graph Laplacian `m ΔX`, with `ΔX_a = Σ_b (X_b − X_a)`. S, I, and R use the same mobility in this release. No-flux boundaries mean there is no edge outside the rectangular grid. Empty cells are safe and can receive people only if movement from a populated neighbour is enabled.

## Units and interpretation

Time is days; population is a continuous quantity; rates are per day; grid coordinates are abstract cells. A heatmap is a model output, not measured geographic or surveillance data. The application's uniform-population initializer distributes total population equally across cells, distributes initial recovered equally, and puts initial infected at one selected cell. Infections must fit in that cell's available population.
