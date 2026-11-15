setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    # A throwaway identity, so the tests never touch your own Git configuration.
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
}

@test "creates a repository with one commit" {
    run bash newrepo.sh "$BATS_TEST_TMPDIR/notes"
    assert_success
    run git -C "$BATS_TEST_TMPDIR/notes" log --format=%s
    assert_output "Initial commit"
}
