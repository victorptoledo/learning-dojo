#!/usr/bin/env bash
# Usage: tidy.sh REPO BASE - folds the fixup!, squash! and amend! commits of the branch.
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "usage: tidy.sh REPO BASE" >&2
    exit 2
fi
repo="$1"
base="$2"
if ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "tidy.sh: not a git repository: $repo" >&2
    exit 1
fi
if [ -n "$(git -C "$repo" status --porcelain --untracked-files=no)" ]; then
    echo "tidy.sh: uncommitted changes in $repo" >&2
    exit 1
fi
if ! git -C "$repo" rev-parse -q --verify "$base^{commit}" > /dev/null; then
    echo "tidy.sh: unknown base $base" >&2
    exit 1
fi

count=$(git -C "$repo" log --format=%s "$base..HEAD" | grep -c -E '^(fixup|squash|amend)! ' || true)
if [ "$count" -eq 0 ]; then
    echo "nothing to fold"
    exit 0
fi

# An editor that accepts the todo list (and the combined messages) as Git arranged them.
if ! GIT_SEQUENCE_EDITOR=true GIT_EDITOR=true \
    git -C "$repo" rebase -q -i --autosquash --keep-base "$base" > /dev/null 2>&1; then
    git -C "$repo" rebase --abort
    echo "tidy.sh: cannot fold the fixups without conflicts" >&2
    exit 1
fi
echo "folded $count commits"
