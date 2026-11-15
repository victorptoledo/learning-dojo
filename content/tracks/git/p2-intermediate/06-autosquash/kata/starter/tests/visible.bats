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
    git -C "$repo" commit -q --allow-empty -m "Initial commit"
    git -C "$repo" switch -q -c feature
}

@test "folds a fixup into its target" {
    echo "export" > "$repo/export.txt"
    git -C "$repo" add export.txt
    git -C "$repo" commit -q -m "Add export"
    echo "export, fixed" > "$repo/export.txt"
    git -C "$repo" commit -q -a --fixup=HEAD
    run bash tidy.sh "$repo" main
    assert_success
    assert_output "folded 1 commits"
    run git -C "$repo" log --format=%s main..HEAD
    assert_output "Add export"
}
