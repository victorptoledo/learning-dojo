setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    root=$BATS_TEST_TMPDIR/root
    mkdir -p "$root/etc"
}

@test "a first run on an empty server creates the app directory" {
    run bash converge.sh "$root"
    assert_success
    assert_line "changed: dir /etc/app"
    [ -d "$root/etc/app" ]
}
