# Deployment

## Requirements and local build

- JDK 21+ with `jlink` and `jpackage`.
- Maven 3.9+.
- Windows x64 for the Windows app image/ZIP/MSI.
- WiX Toolset is required by `jpackage` for MSI creation.

Build and test:

```powershell
mvn clean verify
```

Create the Windows app image, portable ZIP, manifest, SHA-256 files, and (when WiX is installed) MSI:

```powershell
.\scripts\package-windows.ps1 -IncludeInstaller
```

The script builds a reduced runtime with `jlink`, then creates a `jpackage` application image. The portable ZIP contains the launcher and bundled runtime. User exports and scenarios are written to user-chosen paths; the app does not write mutable data under the installation directory. The script's final output files are under `dist\`.

The MSI and image are platform-specific. A locally generated package is not called tested until the launcher has been started on that Windows installation. The app image can be exercised before installing:

```powershell
.\dist\EpidemicAtlas\EpidemicAtlas.exe
```

## CI and releases

`.github\workflows\windows.yml` runs Maven verification on `windows-latest`, installs WiX when needed for MSI packaging, packages the app, and uploads ZIP/manifest/checksums/MSI as workflow artifacts. On a `v*` tag it attempts to create a GitHub Release from those files. The repository has no execution history or published release until that workflow is run on GitHub.

## Runtime data and updates

The application has no database and does not mutate installed files. Scenarios are explicit `.properties` files saved by the user; CSV exports are also user-selected. Package upgrades therefore do not overwrite experiments.

## Troubleshooting

- **MSI packaging cannot find WiX:** install a compatible WiX Toolset, reopen PowerShell so its tools are on `PATH`, or build the portable app image without `-IncludeInstaller`.
- **App does not launch:** use the bundled `EpidemicAtlas` executable from the extracted ZIP; do not copy the executable alone without its adjacent runtime files.
- **Slow UI:** reduce grid size or increase the time step within the stability constraints.
- **Numerical instability:** reduce the time step and rerun. Results are not silently clamped.
- **SmartScreen warning:** unsigned builds may show Windows warnings. Only obtain builds from a trusted release and verify the published SHA-256.
