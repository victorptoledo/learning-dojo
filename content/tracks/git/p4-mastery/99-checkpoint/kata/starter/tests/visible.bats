setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    # A throwaway identity, so the tests never touch your own Git configuration.
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    # A bare "server" with your hook installed, and your clone.
    origin="$BATS_TEST_TMPDIR/origin.git"
    repo="$BATS_TEST_TMPDIR/me"
    git init -q --bare "$origin"
    git clone -q "$origin" "$repo" 2> /dev/null
    git -C "$repo" commit -q --allow-empty -m "Initial commit"
    git -C "$repo" push -q origin main 2> /dev/null
    cp guard.sh "$origin/hooks/pre-receive"
    chmod +x "$origin/hooks/pre-receive"
}

@test "lets a clean fast-forward of main through and stops a force-push" {
    echo export > "$repo/export.txt"
    git -C "$repo" add export.txt
    git -C "$repo" commit -q -m "Add export"
    run git -C "$repo" push origin main
    assert_success
    git -C "$repo" commit -q --amend -m "Add the export"
    run git -C "$repo" push --force origin main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: non-fast-forward push is not allowed"
    assert_equal "$(git -C "$origin" log -1 --format=%s main)" "Add export"
}
