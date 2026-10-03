param(
    [string]$OutputRoot = 'D:\Temp\morphe-manager-downloaders-build',
    [string]$SigningRoot = (Join-Path $env:USERPROFILE '.android\keystores\morphe-manager-downloaders')
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
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
    & (Join-Path $repoRoot 'gradlew.bat') :app:assembleRelease :app:lintRelease `
        "-PbuildRoot=$buildPath" "-PcompanionKeystore=$keyPath" `
        --project-cache-dir "$OutputRoot/project-cache" --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Companion verification build failed.' }
    $artifactRoot = Join-Path $repoRoot 'artifacts'
    New-Item -ItemType Directory -Path $artifactRoot -Force | Out-Null
    $artifact = Join-Path $artifactRoot 'Morphe-Downloader-0.2.0.apk'
    Copy-Item -LiteralPath "$OutputRoot/companion/outputs/apk/release/app-release.apk" -Destination $artifact
    Write-Output $artifact
} finally { $env:COMPANION_KEYSTORE_PASSWORD = $previousPassword }
