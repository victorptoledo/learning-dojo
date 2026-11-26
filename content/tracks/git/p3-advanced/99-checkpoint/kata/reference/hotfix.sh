#!/usr/bin/env bash
# Usage: hotfix.sh REPO GOOD BAD TEST - finds the commit between two release tags that broke
# TEST, reverts it on a hotfix branch in a throwaway worktree and tags the next patch version.
set -euo pipefail

if [ "$#" -ne 4 ]; then
    echo "usage: hotfix.sh REPO GOOD BAD TEST" >&2
    exit 2
fi
repo="$1"
good="$2"
bad="$3"
test_cmd="$4"

fail() {
    echo "hotfix.sh: $1" >&2
    exit 1
}

git -C "$repo" rev-parse --git-dir > /dev/null 2>&1 || fail "not a git repository: $repo"

# A release tag is annotated (a tag object, not just a ref) and named vMAJOR.MINOR.PATCH.
version_re='^v([0-9]+)\.([0-9]+)\.([0-9]+)$'
for tag in "$good" "$bad"; do
    if ! [[ $tag =~ $version_re ]] \
        || [ "$(git -C "$repo" cat-file -t "refs/tags/$tag" 2> /dev/null)" != tag ]; then
        fail "$tag is not an annotated vX.Y.Z tag"
    fi
done
git -C "$repo" merge-base --is-ancestor "refs/tags/$good" "refs/tags/$bad" \
    || fail "$good is not an ancestor of $bad"

[[ $bad =~ $version_re ]]
next="v${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))"
branch="hotfix/$next"
if git -C "$repo" rev-parse -q --verify "refs/tags/$next" > /dev/null \
    || git -C "$repo" rev-parse -q --verify "refs/heads/$branch" > /dev/null; then
    fail "$next already exists"
fi

# All the work happens in a worktree of its own: the user's checkout, their changes and even
# a bisect they have going on stay as they are (refs/bisect and BISECT_LOG are per worktree).
tmp=$(mktemp -d)
wt="$tmp/wt"
done_ok=false
cleanup() {
    if [ -d "$wt" ]; then
        git -C "$wt" bisect reset > /dev/null 2>&1 || true
        git -C "$wt" revert --abort > /dev/null 2>&1 || true
        git -C "$repo" worktree remove --force "$wt" > /dev/null 2>&1 || true
    fi
    git -C "$repo" worktree prune
    if [ "$done_ok" != true ]; then
        git -C "$repo" branch -q -D "$branch" > /dev/null 2>&1 || true
    fi
    rm -rf "$tmp"
}
trap cleanup EXIT

git -C "$repo" worktree add -q -b "$branch" "$wt" "refs/tags/$bad^{commit}" > /dev/null 2>&1

passes() {
    (cd "$wt" && bash -c "$test_cmd") > /dev/null 2>&1
}

# Check both ends before searching: a bisect over a wrong range gives a wrong answer.
if passes; then
    fail "$bad passes the test, nothing to fix"
fi
git -C "$wt" checkout -q --detach "refs/tags/$good^{commit}"
good_passes=true
passes || good_passes=false
git -C "$wt" checkout -q "$branch"
[ "$good_passes" = true ] || fail "$good fails the test"

git -C "$wt" bisect start "refs/tags/$bad^{commit}" "refs/tags/$good^{commit}" > /dev/null
git -C "$wt" bisect run bash -c "$test_cmd" > /dev/null 2>&1 || true
culprit=$(git -C "$wt" rev-parse refs/bisect/bad)
subject=$(git -C "$wt" log -1 --format=%s "$culprit")
git -C "$wt" bisect reset > /dev/null 2>&1

# git revert commits without running pre-commit and commit-msg, so revert into the index and
# let git commit (no --no-verify) record it: the repository's hooks check it like any commit.
if ! git -C "$wt" revert --no-commit "$culprit" > /dev/null 2>&1 \
    || ! git -C "$wt" commit -q --no-edit > /dev/null 2>&1; then
    fail "could not revert $culprit"
fi
passes || fail "the test still fails after reverting $culprit"
git -C "$wt" tag -a "$next" -m "Hotfix $next"

done_ok=true
echo "culprit: $culprit $subject"
echo "tagged $next on $branch"
