setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "reports a world-writable file" {
    mkdir -p "$BATS_TEST_TMPDIR/tree"
    touch "$BATS_TEST_TMPDIR/tree/notes.txt"
    chmod 666 "$BATS_TEST_TMPDIR/tree/notes.txt"
    run bash audit.sh "$BATS_TEST_TMPDIR/tree"
    assert_failure 1
    assert_output "notes.txt: world-writable"
}
