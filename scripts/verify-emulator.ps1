param(
    [Parameter(Mandatory)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [string]$OutputRoot = 'D:\Temp\morphe-manager-downloaders-build'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
if ((& $adb -s $Serial get-state) -ne 'device') { throw 'The specified emulator is not connected.' }
$evidence = Join-Path $OutputRoot 'verification'
New-Item -ItemType Directory -Path $evidence -Force | Out-Null
foreach ($apk in @('companion/outputs/apk/debug/app-debug.apk',
                  'companion/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
    & $adb -s $Serial install -r (Join-Path $OutputRoot $apk)
    if ($LASTEXITCODE -ne 0) { throw 'Emulator APK installation failed.' }
}
function Invoke-ContractTest([string]$Classes, [string]$LogName) {
    $result = & $adb -s $Serial shell am instrument -w -r -e class $Classes `
        app.morphe.manager.downloaders.test/androidx.test.runner.AndroidJUnitRunner
    $result | Set-Content -LiteralPath (Join-Path $evidence "$LogName.txt") -Encoding utf8
    $result | Write-Output
    if ($LASTEXITCODE -ne 0 -or ($result -join "`n") -notmatch 'OK \([0-9]+ tests?\)') {
        throw "Android contract verification failed: $LogName"
    }
}
Invoke-ContractTest 'app.morphe.manager.downloaders.LifecycleTest,app.morphe.manager.downloaders.VerticalSliceTest,app.morphe.manager.downloaders.PresentationTest' 'android-contracts'
Invoke-ContractTest 'app.morphe.manager.downloaders.ProcessPersistenceTest#aPreparePendingDownload' 'process-prepare'
& $adb -s $Serial shell am force-stop app.morphe.manager.downloaders
if ($LASTEXITCODE -ne 0) { throw 'Could not stop the verification process.' }
Invoke-ContractTest 'app.morphe.manager.downloaders.ProcessPersistenceTest#bRestoreCompletedDownload' 'process-restore'
& $adb -s $Serial pull /sdcard/Android/data/app.morphe.manager.downloaders/files/verification-ready.png `
    (Join-Path $evidence 'ready.png')
if ($LASTEXITCODE -ne 0) { throw 'Could not collect the UI verification artifact.' }
