#!/usr/bin/env bash
# Usage: slugify.sh TEXT... - prints TEXT as a lowercase slug, words joined by dashes.
set -euo pipefail

if [ "$#" -eq 0 ]; then
    echo "usage: slugify.sh TEXT..." >&2
    exit 2
fi
printf '%s\n' "$*" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -e 's/[^a-z0-9]\{1,\}/-/g' -e 's/^-//' -e 's/-$//'
