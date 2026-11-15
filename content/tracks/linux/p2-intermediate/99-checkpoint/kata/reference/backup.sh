#!/usr/bin/env bash
# Usage: backup.sh [-k KEEP] [-n] SRC DEST - archives SRC into DEST and rotates old archives.
set -euo pipefail

usage() {
    echo "usage: backup.sh [-k KEEP] [-n] SRC DEST" >&2
    exit 2
}

keep=5
dry_run=false
while getopts ':k:n' opt; do
    case "$opt" in
        k) keep="$OPTARG" ;;
        n) dry_run=true ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))
[ "$#" -eq 2 ] || usage

if [[ ! "$keep" =~ ^[0-9]+$ ]] || (( keep < 1 )); then
    echo "backup.sh: invalid keep: $keep" >&2
    exit 2
fi
src="${1%/}"
dest="${2%/}"
if [ ! -d "$src" ]; then
    echo "backup.sh: not a directory: $1" >&2
    exit 1
fi

name=$(basename "$src")
stamp="${BACKUP_NOW:-$(date +%Y%m%d-%H%M%S)}"
archive="$dest/$name-$stamp.tar.gz"

if [ -e "$archive" ]; then
    echo "backup.sh: already exists: $archive" >&2
    exit 1
fi

if "$dry_run"; then
    echo "would create $archive"
else
    mkdir -p "$dest"
    tmp="$dest/.$name-$stamp.tar.gz.tmp"
    trap 'rm -f "$tmp"' EXIT
    tar -czf "$tmp" -C "$(dirname "$src")" "$name"
    mv "$tmp" "$archive"
    echo "created $archive"
fi

archives=()
if [ -d "$dest" ]; then
    for file in "$dest/$name"-*.tar.gz; do
        [[ -e "$file" ]] || continue
        if [[ "${file##*/}" =~ ^"$name"-[0-9]{8}-[0-9]{6}\.tar\.gz$ ]]; then
            archives+=("$file")
        fi
    done
fi
if "$dry_run"; then
    archives+=("$archive")
fi
[ "${#archives[@]}" -gt 0 ] || exit 0
mapfile -t archives < <(printf '%s\n' "${archives[@]}" | LC_ALL=C sort)

excess=$(( ${#archives[@]} - keep ))
for (( i = 0; i < excess; i++ )); do
    if "$dry_run"; then
        echo "would remove ${archives[i]}"
    else
        rm -f "${archives[i]}"
        echo "removed ${archives[i]}"
    fi
done
