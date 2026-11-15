#!/usr/bin/env bash
# Usage: sync.sh REPO BRANCH - rebases BRANCH onto origin/main and pushes it with a lease.
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "usage: sync.sh REPO BRANCH" >&2
    exit 2
fi
repo="$1"
branch="$2"
if ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "sync.sh: not a git repository: $repo" >&2
    exit 1
fi
if [ -n "$(git -C "$repo" status --porcelain --untracked-files=no)" ]; then
    echo "sync.sh: uncommitted changes in $repo" >&2
    exit 1
fi
if ! git -C "$repo" show-ref --verify -q "refs/heads/$branch"; then
    echo "sync.sh: no branch $branch" >&2
    exit 1
fi

git -C "$repo" fetch -q origin
# The lease below compares with origin/BRANCH, which the fetch just updated: check it ourselves.
if git -C "$repo" rev-parse -q --verify "refs/remotes/origin/$branch" > /dev/null \
    && ! git -C "$repo" merge-base --is-ancestor "refs/remotes/origin/$branch" "refs/heads/$branch"; then
    echo "sync.sh: origin/$branch has commits that $branch does not have" >&2
    exit 1
fi

if ! git -C "$repo" rebase -q origin/main "$branch" > /dev/null 2>&1; then
    git -C "$repo" rebase --abort
    echo "sync.sh: $branch conflicts with origin/main" >&2
    exit 1
fi
git -C "$repo" push -q --force-with-lease -u origin "$branch" > /dev/null
count=$(git -C "$repo" rev-list --count "origin/main..refs/heads/$branch")
echo "$branch: $count commits on top of origin/main"
