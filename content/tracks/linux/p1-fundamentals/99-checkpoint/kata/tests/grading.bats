setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

make_project() {
    project="$BATS_TEST_TMPDIR/project"
    mkdir -p "$project/data" "$project/docs" "$project/bin"
    head -c 1000 /dev/zero > "$project/data/archive.tar.gz"
    head -c 300 /dev/zero > "$project/docs/notes.txt"
    head -c 100 /dev/zero > "$project/Makefile"
    head -c 50 /dev/zero > "$project/docs/todo.txt"
    head -c 30 /dev/zero > "$project/photo.JPG"
    head -c 20 /dev/zero > "$project/bin/run.sh"
    chmod 666 "$project/docs/todo.txt"
    chmod 777 "$project/bin"
}

@test "prints the whole inventory" {
    make_project
    run bash inventory.sh "$project"
    assert_success
    assert_output "files: 6
directories: 3
bytes: 1500
extensions:
2 txt
1 (none)
1 gz
1 jpg
1 sh
largest:
1000 data/archive.tar.gz
300 docs/notes.txt
100 Makefile
world-writable: 2"
}

@test "names with a leading or trailing dot have no extension" {
    mkdir -p "$BATS_TEST_TMPDIR/dots"
    touch "$BATS_TEST_TMPDIR/dots/.bashrc" "$BATS_TEST_TMPDIR/dots/draft." "$BATS_TEST_TMPDIR/dots/.config.yml"
    run bash inventory.sh "$BATS_TEST_TMPDIR/dots"
    assert_success
    assert_line --index 4 "2 (none)"
    assert_line --index 5 "1 yml"
}

@test "equal sizes are listed by path" {
    mkdir -p "$BATS_TEST_TMPDIR/same"
    for name in c b a d; do head -c 10 /dev/zero > "$BATS_TEST_TMPDIR/same/$name.log"; done
    run bash inventory.sh "$BATS_TEST_TMPDIR/same"
    assert_success
    assert_output --partial "largest:
10 a.log
10 b.log
10 c.log
world-writable: 0"
}

@test "an empty directory gives zeros" {
    mkdir -p "$BATS_TEST_TMPDIR/empty"
    run bash inventory.sh "$BATS_TEST_TMPDIR/empty"
    assert_success
    assert_output "files: 0
directories: 0
bytes: 0
extensions:
largest:
world-writable: 0"
}

@test "the directory itself is not counted, even when world-writable" {
    mkdir -p "$BATS_TEST_TMPDIR/open"
    chmod 777 "$BATS_TEST_TMPDIR/open"
    run bash inventory.sh "$BATS_TEST_TMPDIR/open"
    assert_success
    assert_line --index 1 "directories: 0"
    assert_line --index 5 "world-writable: 0"
}

@test "symbolic links are not counted as files" {
    mkdir -p "$BATS_TEST_TMPDIR/links"
    head -c 5 /dev/zero > "$BATS_TEST_TMPDIR/links/real.txt"
    ln -s real.txt "$BATS_TEST_TMPDIR/links/alias.txt"
    run bash inventory.sh "$BATS_TEST_TMPDIR/links"
    assert_success
    assert_line --index 0 "files: 1"
    assert_line --index 2 "bytes: 5"
}

@test "without an argument prints the usage and exits 2" {
    run --separate-stderr bash inventory.sh
    assert_failure 2
    [ "$stderr" = "usage: inventory.sh DIR" ]
}

@test "rejects something that is not a directory" {
    run --separate-stderr bash inventory.sh /etc/hostname-missing
    assert_failure 2
    [ "$stderr" = "inventory.sh: not a directory: /etc/hostname-missing" ]
}
