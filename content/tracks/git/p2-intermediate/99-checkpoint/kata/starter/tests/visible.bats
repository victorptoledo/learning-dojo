setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    # A throwaway identity, so the tests never touch your own Git configuration.
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    # A bare "server" and your clone.
    origin="$BATS_TEST_TMPDIR/origin.git"
    repo="$BATS_TEST_TMPDIR/me"
    git init -q --bare "$origin"
    git clone -q "$origin" "$repo" 2> /dev/null
    git -C "$repo" commit -q --allow-empty -m "Initial commit"
    git -C "$repo" push -q origin main
}

@test "lands a branch on the server's main" {
    git -C "$repo" switch -q -c feature
    echo export > "$repo/export.txt"
    git -C "$repo" add export.txt
    git -C "$repo" commit -q -m "Add export"
    run bash land.sh "$repo" feature
    assert_success
    assert_output "landed feature: 1 commits on main"
    run git -C "$origin" log --format=%s main
    assert_output "Add export
Initial commit"
}
