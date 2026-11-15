#!/usr/bin/env bash
# Usage: audit.sh DIR - reports world-writable, setuid and setgid entries.
if [ "$#" -ne 1 ]; then
    echo "usage: audit.sh DIR" >&2
    exit 2
fi
dir="$1"
if [ ! -d "$dir" ]; then
    echo "audit.sh: not a directory: $dir" >&2
    exit 2
fi

findings=$(
    {
        find "$dir" -mindepth 1 -perm -o+w ! \( -type d -perm -1000 \) -printf '%P: world-writable\n'
        find "$dir" -type f -perm -4000 -printf '%P: setuid\n'
        find "$dir" -type f -perm -2000 -printf '%P: setgid\n'
    } | LC_ALL=C sort
)

if [ -z "$findings" ]; then
    echo "no findings"
    exit 0
fi
echo "$findings"
exit 1
