setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    # A throwaway identity, so the tests never touch your own Git configuration.
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    # A bare "server", your clone and a colleague's clone.
    origin="$BATS_TEST_TMPDIR/origin.git"
    repo="$BATS_TEST_TMPDIR/me"
    other="$BATS_TEST_TMPDIR/colleague"
    git init -q --bare "$origin"
    git clone -q "$origin" "$other" 2> /dev/null
    echo "# Shop" > "$other/README.md"
    git -C "$other" add README.md
    git -C "$other" commit -q -m "Initial commit"
    git -C "$other" push -q origin main
    git clone -q "$origin" "$repo"
}

@test "rebases the branch onto the new main" {
    git -C "$repo" switch -q -c feature
    echo search > "$repo/search.txt"
    git -C "$repo" add search.txt
    git -C "$repo" commit -q -m "Add search"
    echo cart > "$other/cart.txt"
    git -C "$other" add cart.txt
    git -C "$other" commit -q -m "Add cart"
    git -C "$other" push -q origin main
    run bash sync.sh "$repo" feature
    assert_success
    assert_output "feature: 1 commits on top of origin/main"
    run git -C "$repo" log --format=%s feature
    assert_output "Add search
Add cart
Initial commit"
}
