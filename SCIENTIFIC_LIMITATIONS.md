# Scientific limitations

Epidemic Atlas is a teaching and experimentation tool. Its trajectories are not forecasts, surveillance, diagnosis, medical advice, or public-health recommendations.

- SIR assumes homogeneous mixing within each cell, fixed β and γ, no latent period, no births/deaths, no vaccination, no waning immunity, and a closed population.
- Population values are deterministic continuous quantities, not integer people. The approximation is especially questionable at small counts.
- Transmission is frequency-dependent and all rates are constant. R₀ = β/γ is an idealized threshold quantity under a fully susceptible closed population.
- The default grid is abstract and uniform. Spatial coordinates have no kilometres or geographic calibration.
- Movement is symmetric nearest-neighbour exchange of all three compartments. It does not represent journeys, contact networks, directional commuting, barriers, or observed mobility.
- Reaction RK4 is followed by first-order explicit movement splitting. Results depend on time step; no adaptive solver, uncertainty interval, stochastic variation, parameter fitting, or statistical significance is supplied.
- Extending or editing the source can change equations and results; saved model-version metadata must be treated as part of experiment reproducibility.
