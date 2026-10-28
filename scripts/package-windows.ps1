param(
    [switch]$IncludeInstaller
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    mvn -B clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE." }

    [xml]$pom = Get-Content -Raw (Join-Path $root "pom.xml")
    $version = $pom.project.version
    $dist = Join-Path $root "dist"
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    $runtime = Join-Path $root "target\runtime"
    $jar = Join-Path $root "target\epidemic-atlas.jar"
    $packageInput = Join-Path $root "target\package-input"
    New-Item -ItemType Directory -Force -Path $packageInput | Out-Null
    Copy-Item $jar $packageInput
    $icon = Join-Path $root "assets\EpidemicAtlas.ico"
    $appImage = Join-Path $dist "EpidemicAtlas"
    if (Test-Path $appImage) {
        Remove-Item -LiteralPath $appImage -Recurse -Force
    }

    jlink --add-modules java.base,java.desktop,java.logging,java.prefs,java.xml `
        --strip-debug --no-header-files --no-man-pages --output $runtime
    if ($LASTEXITCODE -ne 0) { throw "jlink failed with exit code $LASTEXITCODE." }

    jpackage --type app-image --input $packageInput --dest $dist `
        --name EpidemicAtlas --main-jar (Split-Path -Leaf $jar) `
        --main-class org.epidemicatlas.app.EpidemicAtlasApp `
        --app-version $version --vendor "Epidemic Atlas" --runtime-image $runtime --icon $icon
    if ($LASTEXITCODE -ne 0) { throw "jpackage app-image creation failed with exit code $LASTEXITCODE." }

    $zip = Join-Path $dist "EpidemicAtlas-$version-windows-x64.zip"
    Compress-Archive -Path (Join-Path $dist "EpidemicAtlas") -DestinationPath $zip -Force

    $artifacts = [System.Collections.Generic.List[string]]::new()
    $artifacts.Add($zip)
    if ($IncludeInstaller) {
        jpackage --type msi --input $packageInput --dest $dist `
            --name EpidemicAtlas --main-jar (Split-Path -Leaf $jar) `
            --main-class org.epidemicatlas.app.EpidemicAtlasApp `
            --app-version $version --vendor "Epidemic Atlas" --runtime-image $runtime --icon $icon `
            --win-menu --win-shortcut
        if ($LASTEXITCODE -ne 0) { throw "jpackage MSI creation failed with exit code $LASTEXITCODE." }
        $msi = Join-Path $dist "EpidemicAtlas-$version.msi"
        if (-not (Test-Path $msi)) { throw "jpackage returned success but the expected MSI was not created." }
        $artifacts.Add($msi)
    }

    $javaVersion = (& jlink --version).Trim()
    $manifest = Join-Path $dist "release-manifest.txt"
    @(
        "Application: Epidemic Atlas"
        "Version: $version"
        "Platform: Windows x64"
        "Java runtime: JDK $javaVersion"
        "Packaging: jpackage app-image with jlink runtime"
        "Built UTC: $([DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'))"
        "Files:"
    ) | Set-Content -Encoding utf8 $manifest
    $checksums = [System.Collections.Generic.List[string]]::new()
    foreach ($artifact in $artifacts) {
        $hash = (Get-FileHash -Algorithm SHA256 $artifact).Hash.ToLowerInvariant()
        Add-Content -Encoding utf8 $manifest "$hash  $(Split-Path -Leaf $artifact)"
        $checksums.Add("$hash *$(Split-Path -Leaf $artifact)")
    }
    Set-Content -Encoding utf8 (Join-Path $dist "SHA256SUMS.txt") $checksums
    Write-Output "Windows package output:"
    Get-ChildItem -File $dist | Select-Object Name,Length
}
finally {
    Pop-Location
}
