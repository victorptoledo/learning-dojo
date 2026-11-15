setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    # A throwaway identity, so the tests never touch your own Git configuration.
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    repo="$BATS_TEST_TMPDIR/shop"
    git init -q "$repo"
}

@test "counts the commits" {
    echo one > "$repo/a.txt"
    git -C "$repo" add a.txt
    git -C "$repo" commit -q -m "First"
    echo two >> "$repo/a.txt"
    git -C "$repo" commit -q -am "Second"
    run bash gitstats.sh "$repo"
    assert_success
    assert_line --index 0 "commits: 2"
}
