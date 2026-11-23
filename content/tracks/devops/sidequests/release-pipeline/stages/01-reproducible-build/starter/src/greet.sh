#!/usr/bin/env bash
# greet - says hello. scripts/build.sh fills in the version and the target.
set -euo pipefail

version="@VERSION@"
target="@TARGET@"

case "${1:-}" in
    --version) echo "greet $version ($target)" ;;
    "") echo "Hello, world!" ;;
    *) echo "Hello, $1!" ;;
esac
