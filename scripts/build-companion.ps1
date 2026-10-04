param(
    [string]$OutputRoot = 'D:\Temp\morphe-manager-downloaders-build',
    [string]$SigningRoot = (Join-Path $env:USERPROFILE '.android\keystores\morphe-manager-downloaders'),
    [string]$BuildGate = (Join-Path $env:USERPROFILE '.codex\skills\gradle-build-gate\scripts\invoke_gradle_build_gate.py')
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
if (!(Test-Path -LiteralPath $BuildGate)) {
    throw 'Install the shared gradle-build-gate helper or supply its path with -BuildGate. Direct Gradle launch is not permitted.'
}
$keyFile = Join-Path $SigningRoot 'companion.p12'
$passwordFile = Join-Path $SigningRoot 'password.txt'
if ((Test-Path -LiteralPath $keyFile) -ne (Test-Path -LiteralPath $passwordFile)) {
    throw 'Incomplete signing identity. Recover the existing key/password pair; do not replace it.'
}
New-Item -ItemType Directory -Path $SigningRoot -Force | Out-Null
$previousPassword = $env:COMPANION_KEYSTORE_PASSWORD
try {
    if (!(Test-Path -LiteralPath $keyFile)) {
        $secretBytes = [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
        $env:COMPANION_KEYSTORE_PASSWORD = [Convert]::ToBase64String($secretBytes)
        [System.IO.File]::WriteAllText($passwordFile, $env:COMPANION_KEYSTORE_PASSWORD)
        & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $keyFile -storetype PKCS12 `
            -storepass:env COMPANION_KEYSTORE_PASSWORD -keypass:env COMPANION_KEYSTORE_PASSWORD `
            -alias morphe-downloaders -keyalg RSA -keysize 3072 -validity 10000 `
            -dname 'CN=Morphe Manager Downloaders'
        if ($LASTEXITCODE -ne 0) { throw 'Signing key generation failed; recover the partial signing identity before retrying.' }
    } else {
        $env:COMPANION_KEYSTORE_PASSWORD = [System.IO.File]::ReadAllText($passwordFile)
    }
    $buildPath = $OutputRoot.Replace('\', '/')
    $keyPath = $keyFile.Replace('\', '/')
    # Kotlin 2.3.10 / AGP 8.13.2 in-process compilation is qualified by the
    # focused companion build; it shares the gate's explicit 3 GiB Gradle heap.
    & python $BuildGate run --project $repoRoot --kotlin-strategy in-process `
        --log "$OutputRoot/release-build.log" -- :app:assembleRelease :app:lintRelease `
        "-PbuildRoot=$buildPath" "-PcompanionKeystore=$keyPath" `
        --project-cache-dir "$OutputRoot/project-cache" --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Companion verification build failed.' }
    $artifactRoot = Join-Path $repoRoot 'artifacts'
    New-Item -ItemType Directory -Path $artifactRoot -Force | Out-Null
    $metadata = Get-Content -LiteralPath "$OutputRoot/companion/outputs/apk/release/output-metadata.json" -Raw | ConvertFrom-Json
    $releaseVersion = $metadata.elements[0].versionName
    if ([string]::IsNullOrWhiteSpace($releaseVersion) -or $releaseVersion -match '[^0-9A-Za-z._-]') {
        throw 'Built APK has no safe artifact version.'
    }
    $artifact = Join-Path $artifactRoot "Morphe-Downloader-$releaseVersion.apk"
    Copy-Item -LiteralPath "$OutputRoot/companion/outputs/apk/release/app-release.apk" -Destination $artifact
    Write-Output $artifact
} finally { $env:COMPANION_KEYSTORE_PASSWORD = $previousPassword }
