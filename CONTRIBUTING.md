# Contributing

Contributions should preserve the separation between scientific engine and user interface and must not present model output as real-world surveillance or forecasting.

## Development

- Use Java 21-compatible source and Maven.
- Keep equations in `engine` and test them without Swing.
- Document parameter units, assumptions, boundary behavior, and numerical stability changes.
- Add regression tests for changed equations, movement conservation, exported formats, or input validation.
- Use standard JUnit 5 and run `mvn clean verify` before submitting.
- Do not add a model to the UI or documentation as available until the equations, validation, implementation, tests, and UI are complete.

## Pull requests

Describe the behavior changed, scientific assumptions, test commands/results, and any unverified packaging claims. Keep commits focused and do not include generated `target\` or `dist\` outputs. No code of unrelated projects is vendored into this repository.
