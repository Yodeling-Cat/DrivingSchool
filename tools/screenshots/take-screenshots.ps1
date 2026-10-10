<#
.SYNOPSIS
Takes the store listing screenshots on a phone, a 7-inch tablet and a 10-inch tablet emulator.

.DESCRIPTION
Builds the debug app and its instrumented test, runs the StoreScreenshots test on each emulator and
pulls what it saved to store-screenshots/phone/, store-screenshots/tablet/7-inch/ and
store-screenshots/tablet/10-inch/ in the repository (ignored by git), one folder per language in each.

At the end it checks the tablet screenshots against what the store accepts, and fails if one breaks it.

An emulator that is already running is used as it is; one that is not is started, and shut down again
afterwards. While the test runs, the status bar is put in demo mode (a fixed clock, a full battery, no
notifications) and animations are switched off; both are put back when it ends.

.EXAMPLE
./tools/screenshots/take-screenshots.ps1

.EXAMPLE
./tools/screenshots/take-screenshots.ps1 -Only tablet -SkipBuild
#>
param(
    # AVD names, as `emulator -list-avds` prints them.
    [string] $Phone = "Pixel_10",
    [string] $Tablet7 = "Pixel_Tablet_7-inch",
    [string] $Tablet10 = "Pixel_Tablet_10-inch",
    # Which screenshots to take: all of them, the phone's only, or the two tablets' only.
    [ValidateSet("all", "phone", "tablet")]
    [string] $Only = "all",
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

$appApk = Join-Path $root "app/androidApp/build/outputs/apk/sk/debug/androidApp-sk-debug.apk"
$testApk = Join-Path $root "app/androidApp/build/outputs/apk/androidTest/sk/debug/androidApp-sk-debug-androidTest.apk"

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
# The exit code is what says whether adb failed: it reports progress on stderr (`adb pull` does), which
# Windows PowerShell turns into an error when the script's output is redirected.
function Invoke-Adb([string] $Serial, [string[]] $Arguments) {
    $ErrorActionPreference = "Continue"
    $result = & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb $($Arguments -join ' ') failed on $Serial" }
    return $result
}

# The serial of the running emulator started from $Avd, or $null.
function Find-Emulator([string] $Avd) {
    $ErrorActionPreference = "Continue"
    $serials = & $adb devices | ForEach-Object { if ($_ -match '^(emulator-\d+)\s+device$') { $Matches[1] } }
    foreach ($serial in $serials) {
        $name = & $adb -s $serial emu avd name | Select-Object -First 1
        if ($name -and $name.Trim() -eq $Avd) { return $serial }
    }
    return $null
}

function Start-Emulator([string] $Avd) {
    $ErrorActionPreference = "Continue"
    Write-Host "Starting $Avd..."
    $process = Start-Process -FilePath $emulator -ArgumentList "-avd", $Avd, "-no-snapshot-save" `
        -WindowStyle Minimized -PassThru
    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
        $serial = Find-Emulator $Avd
        if ($serial -and ((& $adb -s $serial shell getprop sys.boot_completed) -join '').Trim() -eq "1") {
            return $serial
        }
        Start-Sleep -Seconds 3
    }
    # An emulator left booting holds gigabytes of memory, so it does not outlive the attempt.
    if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
    throw "$Avd did not boot within 5 minutes. Is it an AVD that 'emulator -list-avds' lists?"
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
    # adb and the emulator write to stderr freely; failures are caught by their output and exit codes.
    $ErrorActionPreference = "Continue"
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
        if (Test-Path $destination) { Remove-Item -Recurse -Force $destination -ErrorAction Stop }
        New-Item -ItemType Directory -Force (Split-Path $destination) -ErrorAction Stop | Out-Null
        Invoke-Adb $serial "pull", $deviceOutput, $destination | Out-Null
        Write-Host "  -> $destination"
    } finally {
        if ($started) { Stop-Emulator $serial }
    }
}

# Shuts down an emulator this script started, and waits until it is gone, so two never run at once.
function Stop-Emulator([string] $Serial) {
    $ErrorActionPreference = "Continue"
    & $adb -s $Serial emu kill | Out-Null
    $deadline = (Get-Date).AddMinutes(1)
    while ((Get-Date) -lt $deadline -and (& $adb devices | Where-Object { $_ -match "^$Serial\s" })) {
        Start-Sleep -Seconds 2
    }
}

# Width and height of a PNG, from its header: the first chunk is IHDR, and it starts with them.
function Get-PngSize([string] $Path) {
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $header = New-Object byte[] 24
        [void] $stream.Read($header, 0, 24)
    } finally {
        $stream.Dispose()
    }
    $width = ([int] $header[16] -shl 24) + ([int] $header[17] -shl 16) + ([int] $header[18] -shl 8) + $header[19]
    $height = ([int] $header[20] -shl 24) + ([int] $header[21] -shl 16) + ([int] $header[22] -shl 8) + $header[23]
    return @($width, $height)
}

# What the store accepts for one kind of device, broken by each screenshot in $Folder. Up to 8 MB, 16:9 or
# 9:16 (to the pixel), and each side between $MinSide and $MaxSide.
function Test-StoreRequirements([string] $Folder, [int] $MinSide, [int] $MaxSide) {
    $problems = @()
    foreach ($file in Get-ChildItem -Path (Join-Path $output $Folder) -Recurse -Filter *.png) {
        $name = $file.FullName.Substring($output.Length + 1)
        $width, $height = Get-PngSize $file.FullName
        $long = [Math]::Max($width, $height)
        $short = [Math]::Min($width, $height)
        if ($file.Length -gt 8MB) { $problems += "$name is $([Math]::Round($file.Length / 1MB, 1)) MB, over 8 MB" }
        if ([Math]::Abs($long * 9 - $short * 16) -gt 16) { $problems += "$name is ${width}x${height}, not 16:9 or 9:16" }
        if ($short -lt $MinSide -or $long -gt $MaxSide) {
            $problems += "$name is ${width}x${height}, outside $MinSide-$MaxSide px a side"
        }
    }
    return $problems
}

if (-not $SkipBuild) {
    & (Join-Path $root "gradlew.bat") -p $root :app:androidApp:assembleSkDebug :app:androidApp:assembleSkDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw "The build failed." }
}

# Not $tablet7: PowerShell's names ignore case, so that would be the -Tablet7 parameter.
$folder7 = Join-Path "tablet" "7-inch"
$folder10 = Join-Path "tablet" "10-inch"

if ($Only -ne "tablet") { Invoke-Screenshots $Phone "phone" }
if ($Only -ne "phone") {
    Invoke-Screenshots $Tablet7 $folder7
    Invoke-Screenshots $Tablet10 $folder10

    $problems = @(Test-StoreRequirements $folder7 320 3840) + @(Test-StoreRequirements $folder10 1080 7680)
    if ($problems.Count -gt 0) {
        $problems | ForEach-Object { Write-Host "  $_" }
        throw "$($problems.Count) tablet screenshot problem(s): the store will not accept them as they are."
    }
    Write-Host "The tablet screenshots meet the store's requirements."
}
