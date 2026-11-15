#!/usr/bin/env bash
# Usage: fix.sh DIR - resets directories to 755, files to 644 and scripts to 755.
if [ "$#" -ne 1 ]; then
    echo "usage: fix.sh DIR" >&2
    exit 2
fi
dir="$1"
if [ ! -d "$dir" ]; then
    echo "fix.sh: not a directory: $dir" >&2
    exit 2
fi

changed=$(
    {
        find "$dir" -type d -exec chmod -c 755 {} +
        find "$dir" -type f ! -name '*.sh' -exec chmod -c 644 {} +
        find "$dir" -type f -name '*.sh' -exec chmod -c 755 {} +
    } | wc -l
)
echo "fixed $changed"
