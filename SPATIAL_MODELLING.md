# Spatial modelling

## Grid and map semantics

The offline map is a rectangular abstract lattice. It is rendered in a single custom-painted Swing canvas; each displayed cell is derived from the current `SpatialState`. The current view offers infected count, infected proportion, susceptible proportion, recovered proportion, and population. Each view is normalized to its current maximum for color intensity; the color legend is descriptive (low-to-high), and cell inspection provides numerical values.

Clicking a cell selects it. At simulation time zero, clicking also changes the proposed initial seed; **Reset / apply** rebuilds the initial state at that location. Initial conditions are uniform total population, uniformly distributed recovered population, and one localized infected seed. No-radius/multiple-site painting or heterogeneous density editor is implemented.

## Movement, boundaries, and conservation

The four-neighbour graph includes only horizontal and vertical neighbours that are inside the grid. There are no wraparound edges and no outward flux; a corner has two, an edge three, and an interior cell four neighbours. The same symmetric flux is applied to S, I, and R, so each compartment's grid sum is conserved by movement. Local reactions conserve S+I+R per cell. In a closed grid the global population is therefore conserved up to floating-point summation error.

For each undirected edge `(a,b)`, calculate flux from the original pre-movement values and apply equal-and-opposite updates. Since a node's maximum outgoing movement fraction is bounded by `m h degree ≤ 1`, the explicit movement step does not request more than the available amount. This movement is an educational nearest-neighbour transport assumption, not calibrated mobility data and not numerical diffusion masquerading as human movement.

## Laplacian explorer

The inspector uses the same `Lattice.laplacian` routine used to explain the movement update. It displays the selected cell, each existing neighbour's contribution, `ΔI`, and `mΔI`. Because movement is performed as the same edge flux after reaction, the shown continuous-time movement tendency is directly linked to the discrete transport operator.

## Not included

Real maps, coordinate reference systems, imported population rasters, local mobility matrices, barriers, regional rate variation, and map projection are outside the first release. No geographic or public-health interpretation should be inferred from this abstract grid.
