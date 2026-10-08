<#
.SYNOPSIS
Takes the store listing screenshots on a phone and a tablet emulator.

.DESCRIPTION
Builds the debug app and its instrumented test, runs the StoreScreenshots test on each emulator and
pulls what it saved to store-screenshots/<phone|tablet>/<language>/ in the repository (ignored by git).

An emulator that is already running is used as it is; one that is not is started, and shut down again
afterwards. While the test runs, the status bar is put in demo mode (a fixed clock, a full battery, no
notifications) and animations are switched off; both are put back when it ends.

.EXAMPLE
./tools/screenshots/take-screenshots.ps1

.EXAMPLE
./tools/screenshots/take-screenshots.ps1 -Phone Pixel_9 -Tablet Pixel_Tablet -SkipBuild
#>
param(
    # AVD names, as `emulator -list-avds` prints them.
    [string] $Phone = "Pixel_10",
    [string] $Tablet = "Pixel_Tablet",
    # Use the APKs the last build left behind.
    [switch] $SkipBuild
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$output = Join-Path $root "store-screenshots"

$appId = "com.spiraclestudios.autoskola.dev"
$runner = "$appId.test/androidx.test.runner.AndroidJUnitRunner"
$testClass = "com.spiraclestudios.autoskola.screenshots.StoreScreenshots"
# Where the test saves them: the app's external files directory, which adb can read.
$deviceOutput = "/sdcard/Android/data/$appId/files/store-screenshots"

$appApk = Join-Path $root "app/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
$testApk = Join-Path $root "app/androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk"

function Get-SdkDirectory {
    $properties = Join-Path $root "local.properties"
    if (Test-Path $properties) {
        $line = Get-Content $properties | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) {
            # local.properties escapes ':' and '\' the Java way.
            return ($line -replace '^\s*sdk\.dir\s*=\s*', '') -replace '\\:', ':' -replace '\\\\', '\'
        }
    }
    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
    throw "No Android SDK: set sdk.dir in local.properties or ANDROID_HOME."
}

$sdk = Get-SdkDirectory
$adb = Join-Path $sdk "platform-tools/adb.exe"
$emulator = Join-Path $sdk "emulator/emulator.exe"

# Takes the arguments as one array, so flags like -a and -e reach adb rather than PowerShell.
function Invoke-Adb([string] $Serial, [string[]] $Arguments) {
    $result = & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb $($Arguments -join ' ') failed on $Serial" }
    return $result
}

# The serial of the running emulator started from $Avd, or $null.
function Find-Emulator([string] $Avd) {
    $serials = & $adb devices | ForEach-Object { if ($_ -match '^(emulator-\d+)\s+device$') { $Matches[1] } }
    foreach ($serial in $serials) {
        $name = & $adb -s $serial emu avd name | Select-Object -First 1
        if ($name -and $name.Trim() -eq $Avd) { return $serial }
    }
    return $null
}

function Start-Emulator([string] $Avd) {
    Write-Host "Starting $Avd..."
    Start-Process -FilePath $emulator -ArgumentList "-avd", $Avd, "-no-snapshot-save" -WindowStyle Minimized
    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        $serial = Find-Emulator $Avd
        if ($serial -and ((& $adb -s $serial shell getprop sys.boot_completed) -join '').Trim() -eq "1") {
            return $serial
        }
        Start-Sleep -Seconds 3
    }
    throw "$Avd did not boot within 5 minutes."
}

function Invoke-DemoMode([string] $Serial, [string[]] $Extras) {
    Invoke-Adb $Serial (@("shell", "am", "broadcast", "-a", "com.android.systemui.demo") + $Extras) | Out-Null
}

function Enter-SteadyScreen([string] $Serial) {
    $scales = @{}
    foreach ($name in "window_animation_scale", "transition_animation_scale", "animator_duration_scale") {
        $scales[$name] = ((Invoke-Adb $Serial "shell", "settings", "get", "global", $name) -join '').Trim()
        Invoke-Adb $Serial "shell", "settings", "put", "global", $name, "0" | Out-Null
    }
    Invoke-Adb $Serial "shell", "settings", "put", "global", "sysui_demo_allowed", "1" | Out-Null
    Invoke-DemoMode $Serial @("-e", "command", "enter")
    Invoke-DemoMode $Serial @("-e", "command", "clock", "-e", "hhmm", "1000")
    Invoke-DemoMode $Serial @("-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
    Invoke-DemoMode $Serial @("-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4")
    Invoke-DemoMode $Serial @("-e", "command", "network", "-e", "mobile", "show", "-e", "datatype", "none", "-e", "level", "4")
    Invoke-DemoMode $Serial @("-e", "command", "notifications", "-e", "visible", "false")
    return $scales
}

function Exit-SteadyScreen([string] $Serial, [hashtable] $Scales) {
    Invoke-DemoMode $Serial @("-e", "command", "exit")
    foreach ($name in $Scales.Keys) {
        # "null" is a setting that was never written, which reads as the default of 1.
        $value = if ($Scales[$name] -eq "null") { "1" } else { $Scales[$name] }
        Invoke-Adb $Serial "shell", "settings", "put", "global", $name, $value | Out-Null
    }
}

function Invoke-Screenshots([string] $Avd, [string] $Folder) {
    $serial = Find-Emulator $Avd
    $started = $false
    if (-not $serial) {
        $serial = Start-Emulator $Avd
        $started = $true
    }
    Write-Host "$Folder`: $Avd ($serial)"
    try {
        Invoke-Adb $serial "install", "-r", "-t", $appApk | Out-Null
        Invoke-Adb $serial "install", "-r", "-t", $testApk | Out-Null
        $scales = Enter-SteadyScreen $serial
        try {
            $result = (& $adb -s $serial shell am instrument -w -e class $testClass $runner) -join "`n"
        } finally {
            Exit-SteadyScreen $serial $scales
        }
        if ($result -notmatch 'OK \(1 test\)') {
            Write-Host $result
            throw "The screenshot test failed on $Avd."
        }
        $destination = Join-Path $output $Folder
        if (Test-Path $destination) { Remove-Item -Recurse -Force $destination }
        New-Item -ItemType Directory -Force $output | Out-Null
        Invoke-Adb $serial "pull", $deviceOutput, $destination | Out-Null
        Write-Host "  -> $destination"
    } finally {
        if ($started) { & $adb -s $serial emu kill | Out-Null }
    }
}

if (-not $SkipBuild) {
    & (Join-Path $root "gradlew.bat") -p $root :app:androidApp:assembleDebug :app:androidApp:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw "The build failed." }
}

Invoke-Screenshots $Phone "phone"
Invoke-Screenshots $Tablet "tablet"
