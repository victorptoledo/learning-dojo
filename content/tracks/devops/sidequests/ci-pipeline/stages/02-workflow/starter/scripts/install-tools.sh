#!/usr/bin/env bash
# Usage: install-tools.sh - installs the versions in tools.lock into TOOLS_DIR
# (default ~/.cache/toolbox), unless that folder already has exactly them (a cache hit).
# Prints the folder to add to PATH.
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
lock="$root/tools.lock"
dir=${TOOLS_DIR:-$HOME/.cache/toolbox}

if [ -f "$dir/tools.lock" ] && cmp -s "$lock" "$dir/tools.lock"; then
    echo "tools: cached in $dir" >&2
    echo "$dir/bin"
    exit 0
fi

version() {
    awk -v name="$1" '$1 == name { print $2 }' "$lock"
}

case "$(uname -s)-$(uname -m)" in
    Linux-x86_64) platform=linux.x86_64 ;;
    Linux-aarch64 | Linux-arm64) platform=linux.aarch64 ;;
    Darwin-arm64) platform=darwin.aarch64 ;;
    Darwin-x86_64) platform=darwin.x86_64 ;;
    *) echo "install-tools.sh: unsupported platform $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

rm -rf "$dir"
mkdir -p "$dir/bin"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

shellcheck_version=$(version shellcheck)
curl -fsSL "https://github.com/koalaman/shellcheck/releases/download/v${shellcheck_version}/shellcheck-v${shellcheck_version}.${platform}.tar.xz" \
    | tar -xJ -C "$work"
install -m 755 "$work/shellcheck-v${shellcheck_version}/shellcheck" "$dir/bin/shellcheck"

bats_version=$(version bats)
curl -fsSL "https://github.com/bats-core/bats-core/archive/refs/tags/v${bats_version}.tar.gz" \
    | tar -xz -C "$work"
"$work/bats-core-${bats_version}/install.sh" "$dir/bats" > /dev/null
ln -s ../bats/bin/bats "$dir/bin/bats"

cp "$lock" "$dir/tools.lock"
echo "tools: installed in $dir" >&2
echo "$dir/bin"
