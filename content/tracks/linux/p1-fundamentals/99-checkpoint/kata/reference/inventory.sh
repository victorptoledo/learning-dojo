#!/usr/bin/env bash
# Usage: inventory.sh DIR - prints an inventory of a directory tree.
if [ "$#" -ne 1 ]; then
    echo "usage: inventory.sh DIR" >&2
    exit 2
fi
dir="$1"
if [ ! -d "$dir" ]; then
    echo "inventory.sh: not a directory: $dir" >&2
    exit 2
fi

echo "files: $(find "$dir" -type f | wc -l)"
echo "directories: $(find "$dir" -mindepth 1 -type d | wc -l)"
sizes=$(find "$dir" -type f -printf '%s+')
echo "bytes: $(( ${sizes}0 ))"
echo "extensions:"
find "$dir" -type f -printf '%f\n' \
    | sed -E -e 's/^\.//' -e 's/^[^.]*$/(none)/' -e 's/\.$/.(none)/' -e 's/.*\.//' \
    | tr '[:upper:]' '[:lower:]' \
    | sort | uniq -c | sort -k1,1nr -k2,2 | sed 's/^ *//'
echo "largest:"
find "$dir" -type f -printf '%s %P\n' | sort -k1,1nr -k2,2 | head -n 3
echo "world-writable: $(find "$dir" -mindepth 1 -perm -o+w \( -type f -o -type d \) | wc -l)"
