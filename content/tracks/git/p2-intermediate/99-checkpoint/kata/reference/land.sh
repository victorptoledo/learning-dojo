#!/usr/bin/env bash
# Usage: land.sh REPO BRANCH - folds, rebases and fast-forwards BRANCH onto main on origin.
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "usage: land.sh REPO BRANCH" >&2
    exit 2
fi
repo="$1"
branch="$2"

fail() {
    echo "land.sh: $1" >&2
    exit 1
}

git -C "$repo" rev-parse --git-dir > /dev/null 2>&1 || fail "not a git repository: $repo"
[ -z "$(git -C "$repo" status --porcelain --untracked-files=no)" ] || fail "uncommitted changes in $repo"
if [ "$branch" = main ] || ! git -C "$repo" show-ref --verify -q "refs/heads/$branch"; then
    fail "no branch to land: $branch"
fi

git -C "$repo" fetch -q origin
if git -C "$repo" rev-parse -q --verify "refs/remotes/origin/$branch" > /dev/null \
    && ! git -C "$repo" merge-base --is-ancestor "refs/remotes/origin/$branch" "refs/heads/$branch"; then
    fail "origin/$branch has commits that $branch does not have"
fi
git -C "$repo" merge-base --is-ancestor refs/heads/main refs/remotes/origin/main \
    || fail "local main has commits that origin/main does not have"

# Fold the fixups and move onto origin/main in one rebase, without any editor.
if ! GIT_SEQUENCE_EDITOR=true GIT_EDITOR=true \
    git -C "$repo" rebase -q -i --autosquash origin/main "$branch" > /dev/null 2>&1; then
    git -C "$repo" rebase --abort
    fail "$branch conflicts with origin/main"
fi
count=$(git -C "$repo" rev-list --count "refs/remotes/origin/main..refs/heads/$branch")
[ "$count" -gt 0 ] || fail "$branch has nothing to land"

# The branch now sits on origin/main, so this push is a fast-forward of the server's main.
git -C "$repo" push -q origin "refs/heads/$branch:refs/heads/main" || fail "origin/main moved, run land.sh again"
git -C "$repo" switch -q main
git -C "$repo" merge -q --ff-only refs/remotes/origin/main
# Server first: while origin/BRANCH still holds the old commits, branch -d would call it unmerged.
if git -C "$repo" rev-parse -q --verify "refs/remotes/origin/$branch" > /dev/null; then
    git -C "$repo" push -q origin --delete "$branch"
fi
git -C "$repo" branch -q -d "$branch"
echo "landed $branch: $count commits on main"
