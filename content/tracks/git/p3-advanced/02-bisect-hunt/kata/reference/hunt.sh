#!/usr/bin/env bash
# Usage: hunt.sh REPO GOOD CMD - finds the first commit after GOOD where CMD fails.
set -euo pipefail

if [ "$#" -ne 3 ]; then
    echo "usage: hunt.sh REPO GOOD CMD" >&2
    exit 2
fi
repo="$1"
good="$2"
cmd="$3"

fail() {
    echo "hunt.sh: $1" >&2
    exit 1
}

passes() {
    (cd "$repo" && bash -c "$cmd") > /dev/null 2>&1
}

git -C "$repo" rev-parse --git-dir > /dev/null 2>&1 || fail "not a git repository: $repo"
[ -z "$(git -C "$repo" status --porcelain --untracked-files=no)" ] || fail "uncommitted changes in $repo"
git -C "$repo" rev-parse -q --verify "$good^{commit}" > /dev/null || fail "unknown commit $good"

# Check both ends before searching: a bisect over a wrong range gives a wrong answer.
orig=$(git -C "$repo" symbolic-ref -q --short HEAD || git -C "$repo" rev-parse HEAD)
if passes; then
    fail "HEAD passes the test, nothing to hunt"
fi
git -C "$repo" checkout -q --detach "$good"
good_passes=true
passes || good_passes=false
git -C "$repo" checkout -q "$orig"
[ "$good_passes" = true ] || fail "$good fails the test too"

git -C "$repo" bisect start HEAD "$good" > /dev/null
git -C "$repo" bisect run bash -c "$cmd" > /dev/null 2>&1 || true
culprit=$(git -C "$repo" rev-parse refs/bisect/bad)
line=$(git -C "$repo" log -1 --format='%H %s' "$culprit")
git -C "$repo" bisect reset > /dev/null 2>&1
echo "$line"
