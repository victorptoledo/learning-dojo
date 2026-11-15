setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    msg="$BATS_TEST_TMPDIR/COMMIT_EDITMSG"
}

@test "accepts a conventional subject and rejects a free-form one" {
    echo "feat(cart): add coupon codes" > "$msg"
    run bash commit-msg.sh "$msg"
    assert_success
    echo "Added coupons" > "$msg"
    run --separate-stderr bash commit-msg.sh "$msg"
    assert_failure 1
    [ "$stderr" = "commit-msg: subject must look like 'type(scope): description'" ]
}
