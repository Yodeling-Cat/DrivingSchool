# Runs the importer's tests (offline). See AGENTS.md, "Tests first".
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path .venv)) { python -m venv .venv }
    & .venv\Scripts\python -m pip install -q -r requirements.txt
    $env:PYTHONIOENCODING = 'utf-8'
    & .venv\Scripts\python -m unittest discover -p "test_*.py" @args
} finally {
    Pop-Location
}
