setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "creates an archive named after the source and the stamp" {
    mkdir -p "$BATS_TEST_TMPDIR/app"
    echo "data" > "$BATS_TEST_TMPDIR/app/file.txt"
    BACKUP_NOW=20261114-020000 run bash backup.sh "$BATS_TEST_TMPDIR/app" "$BATS_TEST_TMPDIR/backups"
    assert_success
    assert_output "created $BATS_TEST_TMPDIR/backups/app-20261114-020000.tar.gz"
    [ -f "$BATS_TEST_TMPDIR/backups/app-20261114-020000.tar.gz" ]
}
