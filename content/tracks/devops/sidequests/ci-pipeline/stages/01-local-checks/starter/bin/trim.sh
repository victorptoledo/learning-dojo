#!/usr/bin/env bash
# Usage: trim.sh < FILE - removes the blanks at the start and end of every line.
set -euo pipefail

sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
