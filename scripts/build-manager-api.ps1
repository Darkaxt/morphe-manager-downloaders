param(
    [string]$OutputRoot = 'D:\Temp\morphe-manager-downloaders-build',
    [string]$SdkRoot = $env:ANDROID_HOME
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$revision = 'b7e94efb7a2c35496e11174c6d1892685d95b443' # api@1.0.0-dev.4
$sourceRoot = Join-Path $OutputRoot 'api-source'
$adapterRoot = Join-Path $OutputRoot 'api-build'
New-Item -ItemType Directory -Path $sourceRoot, $adapterRoot -Force | Out-Null
$archive = Join-Path $sourceRoot 'source.zip'
$apiRoot = Join-Path $sourceRoot "revanced-manager-$revision\api"
if (!(Test-Path -LiteralPath $apiRoot)) {
    Invoke-WebRequest "https://codeload.github.com/ReVanced/revanced-manager/zip/$revision" -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $sourceRoot
}
$apiPath = $apiRoot.Replace('\', '/')
$sdkPath = $SdkRoot.Replace('\', '/')
@'
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "manager-api"
'@ | Set-Content -LiteralPath (Join-Path $adapterRoot 'settings.gradle.kts') -Encoding utf8
@"
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    id("com.android.library") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.3.10"
}
android {
    namespace = "app.revanced.manager.downloader"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    sourceSets["main"].apply {
        manifest.srcFile("$apiPath/src/main/AndroidManifest.xml")
        java.setSrcDirs(listOf("$apiPath/src/main/kotlin"))
        res.setSrcDirs(listOf("$apiPath/src/main/res"))
        aidl.setSrcDirs(listOf("$apiPath/src/main/aidl"))
    }
    buildFeatures { aidl = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.addAll("-Xexplicit-backing-fields", "-Xcontext-parameters")
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
}
"@ | Set-Content -LiteralPath (Join-Path $adapterRoot 'build.gradle.kts') -Encoding utf8
"sdk.dir=$sdkPath" | Set-Content -LiteralPath (Join-Path $adapterRoot 'local.properties') -Encoding utf8
"android.useAndroidX=true`norg.gradle.jvmargs=-Xmx2048m" | Set-Content -LiteralPath (Join-Path $adapterRoot 'gradle.properties') -Encoding utf8
& (Join-Path $repoRoot 'gradlew.bat') --project-dir $adapterRoot assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Exact-source manager API build failed.' }
Write-Output (Join-Path $adapterRoot 'build\outputs\aar\manager-api-release.aar')
