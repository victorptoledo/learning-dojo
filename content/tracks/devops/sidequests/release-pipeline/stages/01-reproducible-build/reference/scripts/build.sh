#!/usr/bin/env bash
# Usage: build.sh VERSION [TARGET...] - reproducible release archives in dist/.
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$root"

all_targets=(linux-amd64 linux-arm64 darwin-arm64)

die() {
    echo "build.sh: $*" >&2
    exit 2
}

if [ "$#" -lt 1 ]; then
    echo "usage: build.sh VERSION [TARGET...]" >&2
    exit 2
fi
version=${1#v}
shift
semver='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z.-]+)?$'
[[ $version =~ $semver ]] || die "invalid version: $version"

targets=("$@")
[ "${#targets[@]}" -gt 0 ] || targets=("${all_targets[@]}")
for target in "${targets[@]}"; do
    case " ${all_targets[*]} " in
        *" $target "*) ;;
        *) die "unknown target: $target" ;;
    esac
done

# Every timestamp in the archive is this one, so the build does not depend on when the
# files were checked out or built.
[ -n "${SOURCE_DATE_EPOCH:-}" ] || die "SOURCE_DATE_EPOCH is not set"
[[ $SOURCE_DATE_EPOCH =~ ^[0-9]+$ ]] || die "SOURCE_DATE_EPOCH is not a number of seconds"

rm -rf dist
mkdir dist
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

for target in "${targets[@]}"; do
    top="greet-$version"
    tree="$work/$target/$top"
    mkdir -p "$tree/bin" "$tree/share/greet"
    sed -e "s/@VERSION@/$version/g" -e "s/@TARGET@/$target/g" src/greet.sh > "$tree/bin/greet"
    cp share/greet/usage.txt "$tree/share/greet/usage.txt"
    cp LICENSE "$tree/LICENSE"
    # Modes come from the rules, not from the checkout (umask, Windows, chmod by hand).
    find "$tree" -type d -exec chmod 755 {} +
    find "$tree" -type f -exec chmod 644 {} +
    chmod 755 "$tree/bin/greet"

    archive="dist/greet-$version-$target.tar.gz"
    # Sorted entries, no user names or ids, one mtime, and gzip without a name or time.
    tar --sort=name --format=gnu --owner=0 --group=0 --numeric-owner \
        --mtime="@$SOURCE_DATE_EPOCH" -C "$work/$target" -cf - "$top" \
        | gzip -n -9 > "$archive"
    echo "$archive"
done
