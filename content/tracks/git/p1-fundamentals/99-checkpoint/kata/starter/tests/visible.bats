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

@test "reports a branch that is one commit ahead" {
    git -C "$repo" commit -q --allow-empty -m "Initial"
    git -C "$repo" switch -q -c feature
    git -C "$repo" commit -q --allow-empty -m "Search"
    run bash branches.sh "$repo"
    assert_success
    assert_output "feature ahead 1 behind 0 open
total: 1 branches, 0 merged"
}
