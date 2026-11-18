setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# fsck_unreachable: what Git itself finds, sorted by id.
fsck_unreachable() {
    git -C "$repo" fsck --unreachable --no-progress 2> /dev/null | grep '^unreachable ' |
        LC_ALL=C sort -k3,3 || true
}

# expire_reflogs: forgets every reflog entry, so only refs, HEAD and the index are roots.
expire_reflogs() {
    git -C "$repo" reflog expire --expire=now --expire-unreachable=now --all
}

# check: the script agrees with fsck, without porcelain or --batch-all-objects.
check() {
    run --separate-stderr minigit unreachable "$repo"
    assert_success
    assert_output "$(fsck_unreachable)"
    no_porcelain
    if grep -q -- '--batch-all-objects' "$calls"; then
        echo "--batch-all-objects is not allowed"
        return 1
    fi
}

@test "unreachable: a clean repository has nothing" {
    put a.txt "a\n"
    commit "one"
    put b/c.txt "c\n"
    commit "two"
    check
    assert_output ""
}

@test "unreachable: a blob written but never committed, an amend and a deleted branch" {
    put a.txt "a\n"
    commit "one"
    printf 'orphan\n' | git -C "$repo" hash-object -w --stdin > /dev/null
    put a.txt "a2\n"
    put dir/b.txt "b\n"
    commit "two"
    git -C "$repo" commit -q --amend -m "two, reworded"
    git -C "$repo" switch -q -c topic
    put topic/t.txt "t\n"
    commit "topic"
    git -C "$repo" switch -q main
    git -C "$repo" branch -q -D topic
    expire_reflogs
    check
    [ "${#lines[@]}" -ge 5 ]
}

@test "unreachable: reflogs and the index are roots" {
    put a.txt "a\n"
    commit "one"
    put a.txt "a2\n"
    commit "two"
    git -C "$repo" reset -q --hard HEAD~1
    put staged.txt "staged, never committed\n"
    git -C "$repo" add staged.txt
    check
    assert_output ""
    expire_reflogs
    check
    assert_line --partial "unreachable commit "
    refute_line --partial "$(git -C "$repo" rev-parse :staged.txt)"
}

@test "unreachable: HEAD keeps a detached commit, stash and tags are refs" {
    put a.txt "a\n"
    commit "one"
    git -C "$repo" switch -q --detach
    put d.txt "detached\n"
    commit "detached work"
    git -C "$repo" switch -q main
    git -C "$repo" switch -q --detach "$(git -C "$repo" rev-parse HEAD@{1})"
    put s.txt "stashed\n"
    git -C "$repo" add s.txt
    git -C "$repo" stash -q
    git -C "$repo" tag -a -m "kept" keep
    git -C "$repo" tag -a -m "gone" gone
    git -C "$repo" tag -d gone > /dev/null
    expire_reflogs
    check
    assert_line --index 0 --regexp '^unreachable tag [0-9a-f]{40}$'
    [ "${#lines[@]}" -eq 1 ]
}

@test "unreachable: packed objects count, reachable or not" {
    put a.txt "a\n"
    commit "one"
    git -C "$repo" switch -q -c old
    put old.txt "old\n"
    commit "old work"
    git -C "$repo" switch -q main
    put b.txt "b\n"
    commit "two"
    git -C "$repo" repack -A -d -q
    git -C "$repo" branch -q -D old
    put c.txt "loose\n"
    commit "three"
    printf 'loose orphan\n' | git -C "$repo" hash-object -w --stdin > /dev/null
    expire_reflogs
    [ -n "$(ls "$repo"/.git/objects/pack/*.idx)" ]
    check
    [ "${#lines[@]}" -eq 4 ]
}

@test "unreachable: submodule commits are not followed" {
    put a.txt "a\n"
    git -C "$repo" add -A
    git -C "$repo" update-index --add --cacheinfo "160000,1111111111111111111111111111111111111111,vendor/lib"
    git -C "$repo" commit -q -m "with a submodule"
    check
    assert_output ""
}

@test "unreachable: bad arguments" {
    mkdir -p "$BATS_TEST_TMPDIR/plain"
    for args in "unreachable" "unreachable $repo extra" "unreachable $BATS_TEST_TMPDIR/plain"; do
        # shellcheck disable=SC2086
        run --separate-stderr minigit $args
        assert_failure 2
        [ "$stderr" = "usage: minigit.sh COMMAND ARGS" ]
    done
}
