# Imports the question banks and the sign catalogue into app/shared composeResources.
# See docs/importing-questions.md.
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path .venv)) { python -m venv .venv }
    & .venv\Scripts\python -m pip install -q -r requirements.txt
    $env:PYTHONIOENCODING = 'utf-8'
    & .venv\Scripts\python import_minv.py @args
    & .venv\Scripts\python import_znacky.py @args
} finally {
    Pop-Location
}
