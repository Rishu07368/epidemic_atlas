# Numerical methods

## Local reaction

Each cell's SIR reaction is integrated with classical explicit fourth-order Runge–Kutta (RK4). Derivatives are evaluated at the beginning, two half steps, and the end of each substep, then combined with weights `1,2,2,1` divided by six. The well-mixed simulator uses the same RK4 implementation.

## Movement and splitting

Spatial steps use first-order Lie operator splitting: local SIR RK4 reaction for step `h`, followed by explicit pairwise graph-flux movement using the reacted arrays. This is not a single fourth-order solution of the coupled reaction-movement equations; overall splitting error depends on the selected time step. The movement rate must satisfy `m × h × maximumDegree ≤ 1` for the grid's explicit movement update to preserve nonnegativity. The maximum degree is four for a non-degenerate rectangular grid.

## Failure behavior and constraints

Non-finite or negative compartment values are rejected with a numerical-instability error; the application stops and recommends reducing the time step. Invalid values are never silently clamped. Parameters require finite nonnegative rates and populations, valid initial totals, positive duration/time step, and no more than 100,000 steps. Grid dimensions are limited to 200 in either direction.

The step is shortened to land exactly on the requested final duration when needed. Numerical outputs are continuous-valued populations and can be fractional. RK4 and the movement update are deterministic; there is no random seed or stochastic mode.

## Accuracy

RK4 is fourth-order for a smooth ODE at sufficiently small steps. The reaction/movement split is only first-order overall in the coupled system, and explicit movement has a positivity restriction. Results should be checked for stability by repeating a run with a smaller step. Small populations and stiff rates require particular care. The application does not provide automatic adaptive steps or error bars.
