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

@test "finds the commit that broke the total" {
    for n in 1 2 3 4 5 6; do
        if [ "$n" -lt 4 ]; then echo "total: 41.50" > "$repo/receipt.txt"; else echo "total: 40.00" > "$repo/receipt.txt"; fi
        echo "$n" > "$repo/step.txt"
        git -C "$repo" add -A
        git -C "$repo" commit -q -m "Step $n"
    done
    culprit=$(git -C "$repo" log --format=%H --grep="^Step 4$")
    run bash hunt.sh "$repo" HEAD~5 'grep -q "total: 41.50" receipt.txt'
    assert_success
    assert_output "$culprit Step 4"
}
