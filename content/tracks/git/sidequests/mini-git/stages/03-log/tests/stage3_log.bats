setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# history: main with a merged branch, a tag and a message whose subject spans two lines.
history() {
    put README.md "# Shop\n"
    commit "first commit"
    put src/cart.txt "cart\n"
    commit "feat: cart"
    git -C "$repo" tag -a -m "Release 1" v1
    git -C "$repo" switch -q -c search
    put src/search.txt "search\n"
    commit "feat: search"
    put src/search.txt "search 2\n"
    commit "fix: search
with a subject on two lines

and a body."
    git -C "$repo" switch -q main
    put src/cart.txt "cart 2\n"
    commit "fix: cart"
    git -C "$repo" merge -q --no-ff -m "Merge branch 'search'" search
}

@test "log: first parents from HEAD, newest first" {
    history
    run --separate-stderr minigit log "$repo"
    assert_success
    assert_output "$(git -C "$repo" log --first-parent --format='%H %s')"
    no_porcelain
}

@test "log: a branch, a tag, an id and an expression" {
    history
    for rev in search v1 "$(git -C "$repo" rev-parse main~1)" "main^2~1" HEAD; do
        run minigit log "$repo" "$rev"
        assert_success
        assert_output "$(git -C "$repo" log --first-parent --format='%H %s' "$rev")"
    done
    run minigit log "$repo" search
    assert_line --index 0 "$(git -C "$repo" rev-parse search) fix: search with a subject on two lines"
    no_porcelain
}

@test "log: unknown revisions and an empty repository" {
    run --separate-stderr minigit log "$repo"
    assert_failure 1
    [ "$stderr" = "minigit.sh: unknown revision HEAD" ]
    history
    run --separate-stderr minigit log "$repo" nope
    assert_failure 1
    [ "$stderr" = "minigit.sh: unknown revision nope" ]
    blob=$(git -C "$repo" rev-parse HEAD:README.md)
    run --separate-stderr minigit log "$repo" "$blob"
    assert_failure 1
    [ "$stderr" = "minigit.sh: unknown revision $blob" ]
}

@test "ls-tree: every file, recursively, in tree order" {
    history
    run --separate-stderr minigit ls-tree "$repo" HEAD
    assert_success
    assert_output "$(git -C "$repo" ls-tree -r HEAD)"
    run minigit ls-tree "$repo" v1
    assert_success
    assert_output "$(git -C "$repo" ls-tree -r v1)"
    no_porcelain
}

@test "ls-tree: executables, links, spaces, deep folders and submodules" {
    put "a.txt" "a\n"
    put "a/inner.txt" "i\n"
    put "my docs/read me.txt" "r\n"
    put "deep/er/and/deeper/x" "x\n"
    put bin/run "#!/bin/sh\n"
    chmod +x "$repo/bin/run"
    ln -s "my docs/read me.txt" "$repo/link"
    git -C "$repo" add -A
    git -C "$repo" update-index --add --cacheinfo "160000,$(git -C "$repo" hash-object -t commit --literally /dev/null),vendor/lib"
    git -C "$repo" commit -q -m "everything"
    run minigit ls-tree "$repo" HEAD
    assert_success
    assert_output "$(git -C "$repo" ls-tree -r HEAD)"
    assert_line "$(printf '160000 commit %s\tvendor/lib' "$(git -C "$repo" rev-parse HEAD:vendor/lib)")"
    no_porcelain
}

@test "ls-tree: unknown revisions and bad arguments" {
    history
    run --separate-stderr minigit ls-tree "$repo" nope
    assert_failure 1
    [ "$stderr" = "minigit.sh: unknown revision nope" ]
    mkdir -p "$BATS_TEST_TMPDIR/plain"
    for args in "ls-tree $repo" "ls-tree $repo HEAD extra" "log $repo HEAD extra" \
        "log $BATS_TEST_TMPDIR/plain"; do
        # shellcheck disable=SC2086
        run --separate-stderr minigit $args
        assert_failure 2
        [ "$stderr" = "usage: minigit.sh COMMAND ARGS" ]
    done
}
