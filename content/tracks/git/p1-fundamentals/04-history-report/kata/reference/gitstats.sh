#!/usr/bin/env bash
# Usage: gitstats.sh REPO - summarises the history reachable from HEAD.
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "usage: gitstats.sh REPO" >&2
    exit 2
fi
repo="$1"
if ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "gitstats.sh: not a git repository: $repo" >&2
    exit 1
fi
if ! git -C "$repo" rev-parse --verify -q HEAD > /dev/null; then
    printf '%s\n' "commits: 0" "authors: 0" "first: -" "last: -" "top files:"
    exit 0
fi

echo "commits: $(git -C "$repo" rev-list --count HEAD)"
echo "authors: $(git -C "$repo" log --format=%ae | sort -u | wc -l)"
echo "first: $(git -C "$repo" log --format=%s | tail -n 1)"
echo "last: $(git -C "$repo" log -1 --format=%s)"
echo "top files:"
git -C "$repo" log --no-merges --format= --name-only \
    | sed '/^$/d' \
    | sort | uniq -c \
    | sort -k1,1nr -k2,2 \
    | awk 'NR <= 3' \
    | sed 's/^ *//'
