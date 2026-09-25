#!/usr/bin/env sh
# Imports the question banks into app/shared composeResources. See docs/importing-questions.md.
set -e
cd "$(dirname "$0")"
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
if [ -x .venv/bin/python ]; then PY=.venv/bin/python; else PY=.venv/Scripts/python; fi
"$PY" -m pip install -q -r requirements.txt
PYTHONIOENCODING=utf-8 "$PY" import_minv.py "$@"
