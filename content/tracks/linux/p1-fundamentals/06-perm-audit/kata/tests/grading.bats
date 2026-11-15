setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

make_tree() {
    tree="$BATS_TEST_TMPDIR/tree"
    mkdir -p "$tree/bin" "$tree/shared" "$tree/scratch" "$tree/docs"
    touch "$tree/notes.txt" "$tree/bin/tool" "$tree/bin/group-tool" "$tree/docs/readme.md" "$tree/deploy.sh"
    chmod 666 "$tree/notes.txt"
    chmod 4755 "$tree/bin/tool"
    chmod 2755 "$tree/bin/group-tool"
    chmod 777 "$tree/shared"
    chmod 1777 "$tree/scratch"
    chmod 644 "$tree/docs/readme.md"
    chmod 600 "$tree/deploy.sh"
    chmod 755 "$tree" "$tree/bin" "$tree/docs"
}

mode() {
    stat -c '%a' "$1"
}

@test "audit lists every finding sorted, and skips sticky directories" {
    make_tree
    run bash audit.sh "$tree"
    assert_failure 1
    assert_output "bin/group-tool: setgid
bin/tool: setuid
notes.txt: world-writable
shared: world-writable"
}

@test "audit reports two findings for the same file" {
    mkdir -p "$BATS_TEST_TMPDIR/two"
    touch "$BATS_TEST_TMPDIR/two/risky"
    chmod 4777 "$BATS_TEST_TMPDIR/two/risky"
    run bash audit.sh "$BATS_TEST_TMPDIR/two"
    assert_failure 1
    assert_output "risky: setuid
risky: world-writable"
}

@test "audit of a clean tree prints no findings and exits 0" {
    mkdir -p "$BATS_TEST_TMPDIR/clean/sub"
    touch "$BATS_TEST_TMPDIR/clean/sub/file"
    chmod 644 "$BATS_TEST_TMPDIR/clean/sub/file"
    run bash audit.sh "$BATS_TEST_TMPDIR/clean"
    assert_success
    assert_output "no findings"
}

@test "audit does not report the directory it was given" {
    mkdir -p "$BATS_TEST_TMPDIR/open"
    chmod 777 "$BATS_TEST_TMPDIR/open"
    run bash audit.sh "$BATS_TEST_TMPDIR/open"
    assert_success
    assert_output "no findings"
}

@test "fix resets modes and counts the changes" {
    make_tree
    run bash fix.sh "$tree"
    assert_success
    assert_output "fixed 6"
    [ "$(mode "$tree/notes.txt")" = 644 ]
    [ "$(mode "$tree/bin/tool")" = 644 ]
    [ "$(mode "$tree/bin/group-tool")" = 644 ]
    [ "$(mode "$tree/shared")" = 755 ]
    [ "$(mode "$tree/scratch")" = 755 ]
    [ "$(mode "$tree/docs/readme.md")" = 644 ]
    [ "$(mode "$tree/deploy.sh")" = 755 ]
}

@test "fix a second time changes nothing" {
    make_tree
    bash fix.sh "$tree"
    run bash fix.sh "$tree"
    assert_success
    assert_output "fixed 0"
}

@test "fix leaves symbolic links alone" {
    make_tree
    ln -s docs/readme.md "$tree/link"
    run bash fix.sh "$tree"
    assert_success
    [ -L "$tree/link" ]
}

@test "both scripts print their usage without an argument" {
    run --separate-stderr bash audit.sh
    assert_failure 2
    [ "$stderr" = "usage: audit.sh DIR" ]
    run --separate-stderr bash fix.sh
    assert_failure 2
    [ "$stderr" = "usage: fix.sh DIR" ]
}

@test "both scripts reject something that is not a directory" {
    touch "$BATS_TEST_TMPDIR/file"
    run --separate-stderr bash audit.sh "$BATS_TEST_TMPDIR/file"
    assert_failure 2
    [ "$stderr" = "audit.sh: not a directory: $BATS_TEST_TMPDIR/file" ]
    run --separate-stderr bash fix.sh nowhere
    assert_failure 2
    [ "$stderr" = "fix.sh: not a directory: nowhere" ]
}
