setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    git config --global advice.detachedHead false
    repo="$BATS_TEST_TMPDIR/shop"
    git init -q "$repo"
    TEST='grep -qx "total=42" total.txt'
}

# commit FILE TEXT SUBJECT
commit() {
    printf '%s\n' "$2" > "$repo/$1"
    git -C "$repo" add "$1"
    git -C "$repo" commit -q -m "$3"
}

release() {
    git -C "$repo" tag -a "$1" -m "Release $1"
}

# v1.0.0 is fine, v1.1.9 has total=41: "Round prices" broke it.
history() {
    commit total.txt "total=42" "Add total"
    release v1.0.0
    commit a.txt "a" "Add a"
    commit b.txt "b" "Add b"
    commit total.txt "total=41" "Round prices"
    commit c.txt "c" "Add c"
    commit d.txt "d" "Add d"
    release v1.1.9
}

culprit_of() {
    git -C "$repo" log -1 --format=%H --grep="^$1\$"
}

worktree_count() {
    git -C "$repo" worktree list --porcelain | grep -c '^worktree '
}

# Nothing of the run is left: no extra worktree, no hotfix branch, no new tag.
assert_nothing_left() {
    assert_equal "$(worktree_count)" 1
    assert_equal "$(git -C "$repo" branch --list 'hotfix/*')" ""
    assert_equal "$(git -C "$repo" tag --list | tr '\n' ' ')" "$1"
}

@test "names the culprit and tags the next patch version" {
    history
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_success
    assert_output "culprit: $(culprit_of "Round prices") Round prices
tagged v1.1.10 on hotfix/v1.1.10"
}

@test "the hotfix branch is the bad release plus one revert of the culprit" {
    history
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_success
    assert_equal "$(git -C "$repo" rev-parse hotfix/v1.1.10^)" "$(git -C "$repo" rev-parse 'v1.1.9^{commit}')"
    assert_equal "$(git -C "$repo" log -1 --format=%s hotfix/v1.1.10)" 'Revert "Round prices"'
    assert_equal "$(git -C "$repo" show hotfix/v1.1.10:total.txt)" "total=42"
    assert_equal "$(git -C "$repo" show hotfix/v1.1.10:d.txt)" "d"
}

@test "the new tag is annotated, says Hotfix and points at the hotfix branch" {
    history
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_success
    assert_equal "$(git -C "$repo" cat-file -t refs/tags/v1.1.10)" tag
    assert_equal "$(git -C "$repo" tag -l --format='%(contents:subject)' v1.1.10)" "Hotfix v1.1.10"
    assert_equal "$(git -C "$repo" rev-parse 'v1.1.10^{commit}')" "$(git -C "$repo" rev-parse hotfix/v1.1.10)"
}

@test "works in its own worktree and leaves the user's checkout and changes alone" {
    history
    git -C "$repo" switch -q -c wip
    printf '%s\n' "total=42" > "$repo/total.txt"
    echo notes > "$repo/notes.txt"
    before=$(git -C "$repo" status --porcelain)
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_success
    assert_line "culprit: $(culprit_of "Round prices") Round prices"
    assert_equal "$(git -C "$repo" symbolic-ref --short HEAD)" wip
    assert_equal "$(git -C "$repo" status --porcelain)" "$before"
    assert_equal "$(cat "$repo/total.txt")" "total=42"
    assert_equal "$(worktree_count)" 1
    assert_equal "$(find "$repo/.git/worktrees" -mindepth 1 2> /dev/null | wc -l)" 0
}

@test "a bisect the user has going on in their checkout survives" {
    history
    git -C "$repo" bisect start v1.1.9 v1.0.0 > /dev/null
    mine=$(git -C "$repo" rev-parse refs/bisect/bad)
    at=$(git -C "$repo" rev-parse HEAD)
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_success
    assert_equal "$(git -C "$repo" rev-parse refs/bisect/bad)" "$mine"
    assert_equal "$(git -C "$repo" rev-parse HEAD)" "$at"
    run git -C "$repo" bisect log
    assert_success
}

@test "skips commits the test cannot check (exit 125)" {
    commit total.txt "total=42" "Add total"
    release v1.0.0
    commit BROKEN "x" "Break the build"
    git -C "$repo" rm -q BROKEN
    git -C "$repo" commit -q -m "Fix the build"
    commit total.txt "total=41" "Round prices"
    commit c.txt "c" "Add c"
    release v1.1.0
    run bash hotfix.sh "$repo" v1.0.0 v1.1.0 "[ -e BROKEN ] && exit 125; $TEST"
    assert_success
    assert_line "culprit: $(culprit_of "Round prices") Round prices"
    assert_line "tagged v1.1.1 on hotfix/v1.1.1"
}

@test "the revert goes through the repository's commit-msg hook" {
    history
    printf '%s\n' '#!/bin/sh' "head -1 \"\$1\" >> \"$BATS_TEST_TMPDIR/hook.log\"" > "$repo/.git/hooks/commit-msg"
    chmod +x "$repo/.git/hooks/commit-msg"
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_success
    assert_equal "$(cat "$BATS_TEST_TMPDIR/hook.log")" 'Revert "Round prices"'
}

@test "refuses when a hook rejects the revert, and leaves nothing behind" {
    history
    printf '%s\n' '#!/bin/sh' 'echo "rejected" >&2' 'exit 1' > "$repo/.git/hooks/commit-msg"
    chmod +x "$repo/.git/hooks/commit-msg"
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_failure 1
    assert_output --partial "hotfix.sh: could not revert $(culprit_of "Round prices")"
    assert_nothing_left "v1.0.0 v1.1.9 "
}

@test "refuses when the revert conflicts, and leaves nothing behind" {
    commit total.txt "total=42" "Add total"
    release v1.0.0
    commit total.txt "total=41" "Round prices"
    commit total.txt "total=40" "Apply discount"
    release v1.1.0
    run bash hotfix.sh "$repo" v1.0.0 v1.1.0 "$TEST"
    assert_failure 1
    assert_output --partial "hotfix.sh: could not revert $(culprit_of "Round prices")"
    assert_nothing_left "v1.0.0 v1.1.0 "
    assert_equal "$(git -C "$repo" status --porcelain)" ""
}

@test "refuses when the test still fails after the revert" {
    commit total.txt "total=42" "Add total"
    commit a.txt "a" "Add a"
    release v1.0.0
    commit total.txt "total=41" "Round prices"
    git -C "$repo" rm -q a.txt
    git -C "$repo" commit -q -m "Remove a"
    release v1.1.0
    run bash hotfix.sh "$repo" v1.0.0 v1.1.0 "$TEST && [ -e a.txt ]"
    assert_failure 1
    assert_output "hotfix.sh: the test still fails after reverting $(culprit_of "Round prices")"
    assert_nothing_left "v1.0.0 v1.1.0 "
}

@test "checks both ends before searching" {
    history
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "true"
    assert_failure 1
    assert_output "hotfix.sh: v1.1.9 passes the test, nothing to fix"
    assert_nothing_left "v1.0.0 v1.1.9 "
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "false"
    assert_failure 1
    assert_output "hotfix.sh: v1.0.0 fails the test"
    assert_nothing_left "v1.0.0 v1.1.9 "
}

@test "accepts only annotated vX.Y.Z tags" {
    history
    git -C "$repo" tag v1.2.0
    git -C "$repo" tag -a release-1 -m "Release 1" v1.0.0
    run bash hotfix.sh "$repo" v1.0.0 v1.2.0 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: v1.2.0 is not an annotated vX.Y.Z tag"
    run bash hotfix.sh "$repo" release-1 v1.1.9 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: release-1 is not an annotated vX.Y.Z tag"
    run bash hotfix.sh "$repo" v1.0.0 v9.9.9 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: v9.9.9 is not an annotated vX.Y.Z tag"
}

@test "refuses when the good release is not an ancestor of the bad one" {
    history
    run bash hotfix.sh "$repo" v1.1.9 v1.0.0 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: v1.1.9 is not an ancestor of v1.0.0"
}

@test "refuses when the next version or its branch already exists" {
    history
    git -C "$repo" tag -a v1.1.10 -m "Release v1.1.10" v1.1.9
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: v1.1.10 already exists"
    git -C "$repo" tag -d v1.1.10 > /dev/null
    git -C "$repo" branch hotfix/v1.1.10 v1.1.9
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: v1.1.10 already exists"
    assert_equal "$(git -C "$repo" rev-parse hotfix/v1.1.10)" "$(git -C "$repo" rev-parse 'v1.1.9^{commit}')"
}

@test "checks its arguments and the repository" {
    run bash hotfix.sh "$repo" v1.0.0 v1.1.9
    assert_failure 2
    assert_output "usage: hotfix.sh REPO GOOD BAD TEST"
    mkdir "$BATS_TEST_TMPDIR/plain"
    run bash hotfix.sh "$BATS_TEST_TMPDIR/plain" v1.0.0 v1.1.9 "$TEST"
    assert_failure 1
    assert_output "hotfix.sh: not a git repository: $BATS_TEST_TMPDIR/plain"
}
