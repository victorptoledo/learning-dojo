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

@test "undoes a hard reset of the checked-out branch" {
    for step in "Add cart" "Add tax rules" "Add coupons"; do
        echo "$step" > "$repo/$(echo "$step" | tr ' ' '-').txt"
        git -C "$repo" add -A
        git -C "$repo" commit -q -m "$step"
    done
    tip=$(git -C "$repo" rev-parse HEAD)
    git -C "$repo" reset -q --hard HEAD~2
    run bash rescue.sh "$repo" main
    assert_success
    assert_output "restored main to $tip Add coupons"
    [ "$(git -C "$repo" rev-parse main)" = "$tip" ]
}
