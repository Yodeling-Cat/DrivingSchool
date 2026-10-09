# Imports the question banks and the sign catalog into app/shared composeResources.
# Usage: import.ps1 [sk|cz] [--refresh] [--version N]. Without a country it imports both.
# See docs/importing-questions.md.
$ErrorActionPreference = 'Stop'
$country = 'all'
$rest = @($args)
if ($rest.Count -gt 0 -and ($rest[0] -eq 'sk' -or $rest[0] -eq 'cz')) {
    $country = $rest[0]
    $rest = @($rest | Select-Object -Skip 1)
}
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path .venv)) { python -m venv .venv }
    & .venv\Scripts\python -m pip install -q -r requirements.txt
    $env:PYTHONIOENCODING = 'utf-8'
    if ($country -eq 'all' -or $country -eq 'sk') {
        & .venv\Scripts\python import_minv.py @rest
        & .venv\Scripts\python import_znacky.py @rest
    }
    if ($country -eq 'all' -or $country -eq 'cz') {
        & .venv\Scripts\python import_md_cz.py @rest
    }
} finally {
    Pop-Location
}
