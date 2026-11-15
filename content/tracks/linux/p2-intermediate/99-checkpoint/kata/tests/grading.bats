setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

make_source() {
    src="$BATS_TEST_TMPDIR/data/app"
    dest="$BATS_TEST_TMPDIR/backups"
    mkdir -p "$src/config"
    echo "one" > "$src/a.txt"
    echo "two" > "$src/config/b.conf"
}

old_archives() {
    mkdir -p "$dest"
    local stamp
    for stamp in "$@"; do
        echo "old" > "$dest/app-$stamp.tar.gz"
    done
}

@test "the archive holds the source under its name" {
    make_source
    BACKUP_NOW=20261114-020000 run bash backup.sh "$src" "$dest"
    assert_success
    run tar -tzf "$dest/app-20261114-020000.tar.gz"
    assert_line "app/a.txt"
    assert_line "app/config/b.conf"
    [ "$(tar -xzOf "$dest/app-20261114-020000.tar.gz" app/config/b.conf)" = "two" ]
}

@test "a trailing slash on the source does not change the name" {
    make_source
    BACKUP_NOW=20261114-020000 run bash backup.sh "$src/" "$dest"
    assert_success
    assert_output "created $dest/app-20261114-020000.tar.gz"
}

@test "keeps the newest archives and removes the others oldest first" {
    make_source
    old_archives 20261110-020000 20261111-020000 20261112-020000 20261113-020000
    BACKUP_NOW=20261114-020000 run bash backup.sh -k 2 "$src" "$dest"
    assert_success
    assert_output "created $dest/app-20261114-020000.tar.gz
removed $dest/app-20261110-020000.tar.gz
removed $dest/app-20261111-020000.tar.gz
removed $dest/app-20261112-020000.tar.gz"
    run ls "$dest"
    assert_output "app-20261113-020000.tar.gz
app-20261114-020000.tar.gz"
}

@test "keeps five by default" {
    make_source
    old_archives 20261108-020000 20261109-020000 20261110-020000 20261111-020000 20261112-020000
    BACKUP_NOW=20261114-020000 run bash backup.sh "$src" "$dest"
    assert_success
    assert_line --index 1 "removed $dest/app-20261108-020000.tar.gz"
    [ "$(ls "$dest" | wc -l)" -eq 5 ]
}

@test "never touches other files in the destination" {
    make_source
    old_archives 20261110-020000 20261111-020000
    echo x > "$dest/app-v2-20261101-020000.tar.gz"
    echo x > "$dest/app-latest.tar.gz"
    echo x > "$dest/notes.txt"
    BACKUP_NOW=20261114-020000 run bash backup.sh -k 1 "$src" "$dest"
    assert_success
    [ -f "$dest/app-v2-20261101-020000.tar.gz" ]
    [ -f "$dest/app-latest.tar.gz" ]
    [ -f "$dest/notes.txt" ]
    [ ! -e "$dest/app-20261110-020000.tar.gz" ]
}

@test "a dry run changes nothing and says what it would do" {
    make_source
    old_archives 20261112-020000 20261113-020000
    BACKUP_NOW=20261114-020000 run bash backup.sh -n -k 2 "$src" "$dest"
    assert_success
    assert_output "would create $dest/app-20261114-020000.tar.gz
would remove $dest/app-20261112-020000.tar.gz"
    run ls "$dest"
    assert_output "app-20261112-020000.tar.gz
app-20261113-020000.tar.gz"
}

@test "a dry run does not create the destination" {
    make_source
    BACKUP_NOW=20261114-020000 run bash backup.sh -n "$src" "$BATS_TEST_TMPDIR/nowhere"
    assert_success
    assert_output "would create $BATS_TEST_TMPDIR/nowhere/app-20261114-020000.tar.gz"
    [ ! -e "$BATS_TEST_TMPDIR/nowhere" ]
}

@test "refuses to overwrite an existing archive" {
    make_source
    old_archives 20261114-020000
    BACKUP_NOW=20261114-020000 run --separate-stderr bash backup.sh "$src" "$dest"
    assert_failure 1
    [ "$stderr" = "backup.sh: already exists: $dest/app-20261114-020000.tar.gz" ]
    [ "$(cat "$dest/app-20261114-020000.tar.gz")" = "old" ]
}

@test "leaves no temporary file when the archive cannot be written" {
    make_source
    mkdir -p "$dest"
    chmod 000 "$src/config"
    BACKUP_NOW=20261114-020000 run --separate-stderr bash backup.sh "$src" "$dest"
    chmod 755 "$src/config"
    assert_failure
    [ -z "$(ls -A "$dest")" ]
}

@test "rejects a bad keep, a missing source and wrong usage" {
    make_source
    run --separate-stderr bash backup.sh -k 0 "$src" "$dest"
    assert_failure 2
    [ "$stderr" = "backup.sh: invalid keep: 0" ]
    run --separate-stderr bash backup.sh -k two "$src" "$dest"
    assert_failure 2
    [ "$stderr" = "backup.sh: invalid keep: two" ]
    run --separate-stderr bash backup.sh "$BATS_TEST_TMPDIR/missing" "$dest"
    assert_failure 1
    [ "$stderr" = "backup.sh: not a directory: $BATS_TEST_TMPDIR/missing" ]
    run --separate-stderr bash backup.sh -z "$src" "$dest"
    assert_failure 2
    [ "$stderr" = "usage: backup.sh [-k KEEP] [-n] SRC DEST" ]
    run --separate-stderr bash backup.sh "$src"
    assert_failure 2
    [ "$stderr" = "usage: backup.sh [-k KEEP] [-n] SRC DEST" ]
}
