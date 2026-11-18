setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# git_diff A B: what Git itself says.
git_diff() {
    git -C "$repo" diff --name-status --no-renames "$1" "$2"
}

@test "diff: added, deleted and modified files in nested folders" {
    put README.md "# Shop\n"
    put src/cart.txt "cart\n"
    put src/old.txt "old\n"
    put docs/guide.md "guide\n"
    commit "one"
    put README.md "# Shop, now with search\n"
    git -C "$repo" rm -q src/old.txt
    put src/search/index.txt "index\n"
    put src/search/deep/more.txt "more\n"
    commit "two"
    run --separate-stderr minigit diff "$repo" HEAD~1 HEAD
    assert_success
    assert_output "$(git_diff HEAD~1 HEAD)"
    run minigit diff "$repo" HEAD HEAD~1
    assert_success
    assert_output "$(git_diff HEAD HEAD~1)"
    no_porcelain
}

@test "diff: modes, links, folders replacing files and submodules" {
    put start.sh "echo start\n"
    put run.sh "echo run\n"
    put config "a file\n"
    put a.txt "a\n"
    put "my docs/read me.txt" "r\n"
    git -C "$repo" add -A
    git -C "$repo" update-index --add --cacheinfo "160000,$(git -C "$repo" hash-object -t commit --literally /dev/null),vendor/lib"
    git -C "$repo" commit -q -m "one"
    chmod +x "$repo/run.sh"
    rm "$repo/start.sh"
    ln -s run.sh "$repo/start.sh"
    rm "$repo/config"
    put config/app.yaml "now a folder\n"
    put a/x.txt "x\n"
    put "my docs/read me.txt" "r2\n"
    git -C "$repo" add -A
    git -C "$repo" update-index --add --cacheinfo "160000,$(printf 'other' | git -C "$repo" hash-object -t commit --literally --stdin),vendor/lib"
    git -C "$repo" commit -q -m "two"
    run minigit diff "$repo" HEAD~1 HEAD
    assert_success
    assert_output "$(git_diff HEAD~1 HEAD)"
    assert_line "$(printf 'T\tstart.sh')"
    assert_line "$(printf 'M\trun.sh')"
    assert_line "$(printf 'M\tvendor/lib')"
    run minigit diff "$repo" HEAD HEAD~1
    assert_output "$(git_diff HEAD HEAD~1)"
    no_porcelain
}

@test "diff: never opens a tree that did not change" {
    for i in 1 2 3 4 5; do
        put "vendor/lib$i/src/deep/file$i.txt" "lib $i\n"
        put "vendor/lib$i/README" "readme $i\n"
    done
    put src/app/main.txt "main\n"
    put src/app/util/helpers.txt "helpers\n"
    commit "one"
    put src/app/main.txt "main 2\n"
    commit "two"
    git -C "$repo" ls-tree -r -d --format='%(objectname)' HEAD:vendor > "$BATS_TEST_TMPDIR/same"
    git -C "$repo" rev-parse HEAD:vendor HEAD:src/app/util >> "$BATS_TEST_TMPDIR/same"
    : > "$calls"
    run minigit diff "$repo" HEAD~1 HEAD
    assert_success
    assert_output "$(printf 'M\tsrc/app/main.txt')"
    while read -r tree; do
        if grep -q "$tree" "$calls"; then
            echo "opened the unchanged tree $tree:"
            cat "$calls"
            return 1
        fi
    done < "$BATS_TEST_TMPDIR/same"
    no_porcelain
}

@test "diff: the same tree prints nothing" {
    put a.txt "a\n"
    commit "one"
    git -C "$repo" commit -q --allow-empty -m "empty"
    git -C "$repo" tag -a -m "tag" v1
    run --separate-stderr minigit diff "$repo" HEAD~1 v1
    assert_success
    assert_output ""
    run minigit diff "$repo" HEAD HEAD
    assert_success
    assert_output ""
}

@test "diff: unknown revisions and bad arguments" {
    put a.txt "a\n"
    commit "one"
    run --separate-stderr minigit diff "$repo" HEAD nope
    assert_failure 1
    [ "$stderr" = "minigit.sh: unknown revision nope" ]
    for args in "diff $repo HEAD" "diff $repo HEAD HEAD extra"; do
        # shellcheck disable=SC2086
        run --separate-stderr minigit $args
        assert_failure 2
        [ "$stderr" = "usage: minigit.sh COMMAND ARGS" ]
    done
}
