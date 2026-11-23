#!/usr/bin/env bash
# Usage: verify.sh DIST - checks the archives of DIST against its SHA256SUMS and manifest.json.
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "usage: verify.sh DIST" >&2
    exit 2
fi
dist=$1
if [ ! -d "$dist" ]; then
    echo "verify.sh: not a directory: $dist" >&2
    exit 2
fi
cd "$dist"

for needed in manifest.json SHA256SUMS; do
    if [ ! -f "$needed" ]; then
        echo "verify: missing $needed"
        exit 1
    fi
done
if ! jq -e '.version and (.artifacts | type == "array")' manifest.json > /dev/null 2>&1; then
    echo "verify: manifest.json is not a release manifest"
    exit 1
fi

problems=0
problem() {
    echo "verify: $1: $2"
    problems=$((problems + 1))
}

version=$(jq -r '.version' manifest.json)
declare -A listed=()
while IFS=$'\t' read -r file size want; do
    listed[$file]=1
    if [ ! -f "$file" ]; then
        problem "$file" missing
        continue
    fi
    [ "$(stat -c %s "$file")" = "$size" ] || problem "$file" "size mismatch"
    [ "$(sha256sum "$file" | cut -d' ' -f1)" = "$want" ] || problem "$file" "checksum mismatch"
    sums=$(awk -v file="$file" '$2 == file || $2 == "*" file { print $1 }' SHA256SUMS)
    [ "$sums" = "$want" ] || problem "$file" "SHA256SUMS disagrees"
    top=$(tar -tzf "$file" 2> /dev/null | head -1 || true)
    [ "$top" = "greet-$version/" ] || problem "$file" "is not version $version inside"
done < <(jq -r '.artifacts[] | [.file, .size, .sha256] | @tsv' manifest.json)

# Nothing may come along that the manifest does not describe.
while read -r file; do
    [ -n "${listed[$file]:-}" ] || problem "$file" "not in the manifest"
done < <({ find . -maxdepth 1 -type f -name '*.tar.gz' -printf '%f\n'; awk '{ sub(/^\*/, "", $2); print $2 }' SHA256SUMS; } | LC_ALL=C sort -u)

if [ "$problems" -gt 0 ]; then
    exit 1
fi
echo "verify: ok ($(jq '.artifacts | length' manifest.json) artifacts, version $version)"
