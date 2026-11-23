#!/usr/bin/env bash
# Usage: manifest.sh DIST - writes DIST/SHA256SUMS and DIST/manifest.json for the archives.
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "usage: manifest.sh DIST" >&2
    exit 2
fi
dist=$1
if [ ! -d "$dist" ]; then
    echo "manifest.sh: not a directory: $dist" >&2
    exit 2
fi
cd "$dist"

mapfile -t archives < <(find . -maxdepth 1 -type f -name 'greet-*.tar.gz' -printf '%f\n' | LC_ALL=C sort)
if [ "${#archives[@]}" -eq 0 ]; then
    echo "manifest.sh: no archives in $dist" >&2
    exit 1
fi

# greet-VERSION-TARGET.tar.gz, where TARGET is os-arch and VERSION may contain dashes.
pattern='^greet-(.+)-([a-z0-9]+-[a-z0-9]+)\.tar\.gz$'
version=
entries=()
for file in "${archives[@]}"; do
    [[ $file =~ $pattern ]] || { echo "manifest.sh: unexpected name: $file" >&2; exit 1; }
    if [ -z "$version" ]; then
        version=${BASH_REMATCH[1]}
    elif [ "$version" != "${BASH_REMATCH[1]}" ]; then
        echo "manifest.sh: archives of several versions ($version and ${BASH_REMATCH[1]})" >&2
        exit 1
    fi
    entries+=("$(jq -n --arg file "$file" --arg target "${BASH_REMATCH[2]}" \
        --arg sha "$(sha256sum "$file" | cut -d' ' -f1)" --argjson size "$(stat -c %s "$file")" \
        '{file: $file, target: $target, size: $size, sha256: $sha}')")
done

# Written next to the archives first, then renamed: a reader never sees half a file.
sha256sum "${archives[@]}" > SHA256SUMS.tmp
mv SHA256SUMS.tmp SHA256SUMS
printf '%s\n' "${entries[@]}" \
    | jq -s --arg version "$version" --arg commit "${GIT_COMMIT:-unknown}" \
        '{name: "greet", version: $version, commit: $commit, artifacts: .}' > manifest.json.tmp
mv manifest.json.tmp manifest.json
echo "manifest: $version, ${#archives[@]} artifacts"
