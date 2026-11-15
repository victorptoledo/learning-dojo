#!/usr/bin/env bash
# Usage: branches.sh REPO [BASE] - how every local branch stands against BASE (default main).
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    echo "usage: branches.sh REPO [BASE]" >&2
    exit 2
fi
repo="$1"
base="${2:-main}"
if ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "branches.sh: not a git repository: $repo" >&2
    exit 1
fi
if ! git -C "$repo" show-ref --verify -q "refs/heads/$base"; then
    echo "branches.sh: no branch $base" >&2
    exit 1
fi

total=0
merged=0
while IFS= read -r name; do
    [ "$name" = "$base" ] && continue
    # Left of A...B is what only BASE has (behind), right is what only the branch has (ahead).
    read -r behind ahead < <(git -C "$repo" rev-list --left-right --count "refs/heads/$base...refs/heads/$name")
    total=$((total + 1))
    if [ "$ahead" -eq 0 ]; then
        state=merged
        merged=$((merged + 1))
    else
        state=open
    fi
    echo "$name ahead $ahead behind $behind $state"
done < <(git -C "$repo" for-each-ref --format='%(refname:lstrip=2)' refs/heads)
echo "total: $total branches, $merged merged"
