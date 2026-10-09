#!/usr/bin/env sh
# Imports the question banks and the sign catalog into app/shared composeResources.
# Usage: import.sh [sk|cz] [--refresh] [--version N]. Without a country it imports both.
# See docs/importing-questions.md.
set -e
cd "$(dirname "$0")"
COUNTRY=all
case "$1" in
  sk|cz) COUNTRY=$1; shift ;;
esac
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
if [ -x .venv/bin/python ]; then PY=.venv/bin/python; else PY=.venv/Scripts/python; fi
"$PY" -m pip install -q -r requirements.txt
if [ "$COUNTRY" != cz ]; then
  PYTHONIOENCODING=utf-8 "$PY" import_minv.py "$@"
  PYTHONIOENCODING=utf-8 "$PY" import_znacky.py "$@"
fi
if [ "$COUNTRY" != sk ]; then
  PYTHONIOENCODING=utf-8 "$PY" import_md_cz.py "$@"
fi
