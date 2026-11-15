#!/usr/bin/env bash
# Usage: install.sh REPO - installs commit-msg.sh as the commit-msg hook of REPO.
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "usage: install.sh REPO" >&2
    exit 2
fi
repo="$1"
if ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "install.sh: not a git repository: $repo" >&2
    exit 1
fi

source_hook="$(dirname "${BASH_SOURCE[0]}")/commit-msg.sh"
# Honours core.hooksPath; the answer is relative to the repository unless it is absolute.
hooks=$(git -C "$repo" rev-parse --git-path hooks)
case "$hooks" in
    /*) ;;
    *) hooks="$repo/$hooks" ;;
esac
mkdir -p "$hooks"

target="$hooks/commit-msg"
if [ -e "$target" ] && ! cmp -s "$source_hook" "$target"; then
    mv "$target" "$target.bak"
    echo "saved the previous hook as commit-msg.bak"
fi
cp "$source_hook" "$target"
chmod +x "$target"
echo "installed commit-msg hook"
