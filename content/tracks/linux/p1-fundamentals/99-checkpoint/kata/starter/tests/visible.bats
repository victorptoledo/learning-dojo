setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "counts the files of a small tree" {
    mkdir -p "$BATS_TEST_TMPDIR/small/sub"
    touch "$BATS_TEST_TMPDIR/small/a.txt" "$BATS_TEST_TMPDIR/small/sub/b.txt"
    run bash inventory.sh "$BATS_TEST_TMPDIR/small"
    assert_success
    assert_line --index 0 "files: 2"
}
