#!/usr/bin/env sh
# Runs the importer's tests (offline). See AGENTS.md, "Tests first".
set -e
cd "$(dirname "$0")"
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
if [ -x .venv/bin/python ]; then PY=.venv/bin/python; else PY=.venv/Scripts/python; fi
"$PY" -m pip install -q -r requirements.txt
PYTHONIOENCODING=utf-8 "$PY" -m unittest discover -p "test_*.py" "$@"
