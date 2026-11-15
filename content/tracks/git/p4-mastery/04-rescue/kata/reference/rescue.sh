#!/usr/bin/env bash
# Usage: rescue.sh REPO BRANCH - undoes the last reset or rebase of BRANCH, or restores it if deleted.
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "usage: rescue.sh REPO BRANCH" >&2
    exit 2
fi
repo="$1"
branch="$2"

fail() {
    echo "rescue.sh: $1" >&2
    exit 1
}

# find_entry REF PREFIX... - index n of the newest REF@{n} whose message starts with a PREFIX.
find_entry() {
    local ref="$1"
    shift
    git -C "$repo" log -g --format=%gs "$ref" -- 2> /dev/null | awk '
        BEGIN { for (i = 1; i < ARGC; i++) prefix[i] = ARGV[i]; ARGC = 1 }
        found { next }
        { for (p in prefix) if (index($0, prefix[p]) == 1) { print NR - 1; found = 1 } }' "$@"
    # awk reads to the end instead of exiting early: an early exit would kill git log with
    # SIGPIPE, and pipefail would turn that into a failure.
}

git -C "$repo" rev-parse --git-dir > /dev/null 2>&1 || fail "not a git repository: $repo"

if git -C "$repo" show-ref --verify -q "refs/heads/$branch"; then
    n=$(find_entry "refs/heads/$branch" "reset: " "branch: Reset to " "rebase (finish): ")
    [ -n "$n" ] || fail "nothing to undo on $branch"
    target=$(git -C "$repo" rev-parse "refs/heads/$branch@{$((n + 1))}")
    if [ "$(git -C "$repo" symbolic-ref -q --short HEAD || true)" = "$branch" ]; then
        [ -z "$(git -C "$repo" status --porcelain --untracked-files=no)" ] || fail "uncommitted changes in $repo"
        # Logged as "rescue: ...", so a second run does not take this move for a mistake.
        GIT_REFLOG_ACTION=rescue git -C "$repo" reset -q --hard "$target"
    else
        git -C "$repo" update-ref -m "rescue: restore $branch" "refs/heads/$branch" "$target"
    fi
else
    n=$(find_entry HEAD "checkout: moving from $branch to ")
    [ -n "$n" ] || fail "no trace of $branch in the reflog"
    target=$(git -C "$repo" rev-parse "HEAD@{$((n + 1))}")
    git -C "$repo" branch "$branch" "$target"
fi

echo "restored $branch to $(git -C "$repo" log -1 --format='%H %s' "$target")"
