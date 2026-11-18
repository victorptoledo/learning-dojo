setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    d="$BATS_TEST_TMPDIR/data"
    mkdir -p "$d"
}

teardown() {
    chmod -R u+w "$BATS_TEST_TMPDIR" 2> /dev/null || true
}

listing() {
    (cd "$d" && find . | LC_ALL=C sort)
}

@test "replaces every copy with a hard link to the first path" {
    fill "$d/a.bin" 3000 a
    fill "$d/x/b.bin" 3000 a
    fill "$d/y/c.bin" 3000 a
    put "$d/note" "hello"
    put "$d/note copy" "hello"
    run --separate-stderr dupes link "$d"
    assert_success
    assert_output "linked x/b.bin -> a.bin
linked y/c.bin -> a.bin
linked note copy -> note
reclaimed: 6005 bytes"
    [ "$(inode "$d/x/b.bin")" = "$(inode "$d/a.bin")" ]
    [ "$(inode "$d/y/c.bin")" = "$(inode "$d/a.bin")" ]
    [ "$(inode "$d/note copy")" = "$(inode "$d/note")" ]
    [ "$(cat "$d/note copy")" = "hello" ]
    [ "$(stat -c %h "$d/a.bin")" = 3 ]
}

@test "leaves no temporary files and no missing paths" {
    fill "$d/a" 100 z
    fill "$d/sub/b" 100 z
    fill "$d/sub/c" 100 z
    before=$(listing)
    run dupes link "$d"
    assert_success
    [ "$(listing)" = "$before" ]
}

@test "afterwards report finds nothing to reclaim" {
    fill "$d/a" 700 q
    fill "$d/b" 700 q
    run dupes link "$d"
    assert_success
    run dupes report "$d"
    assert_output "total: 0 bytes wasted in 0 groups"
    run dupes link "$d"
    assert_success
    assert_output "reclaimed: 0 bytes"
}

@test "copies that are already hard links are counted once" {
    fill "$d/a" 400 k
    fill "$d/b" 400 k
    ln "$d/b" "$d/c"
    ln "$d/a" "$d/d"
    run dupes link "$d"
    assert_success
    assert_output "linked b -> a
linked c -> a
reclaimed: 400 bytes"
    [ "$(stat -c %h "$d/a")" = 4 ]
}

@test "skips a copy whose permissions differ" {
    fill "$d/a.sh" 300 e
    fill "$d/b.sh" 300 e
    fill "$d/c.sh" 300 e
    chmod 644 "$d/a.sh" "$d/c.sh"
    chmod 755 "$d/b.sh"
    run dupes link "$d"
    assert_success
    assert_output "skipped b.sh: different permissions
linked c.sh -> a.sh
reclaimed: 300 bytes"
    [ "$(inode "$d/b.sh")" != "$(inode "$d/a.sh")" ]
    [ "$(stat -c %a "$d/b.sh")" = 755 ]
}

@test "--dry-run changes nothing" {
    fill "$d/a" 900 w
    fill "$d/b" 900 w
    inode_b=$(inode "$d/b")
    before=$(listing)
    run dupes link --dry-run "$d"
    assert_success
    assert_output "would link b -> a
would reclaim: 900 bytes"
    [ "$(inode "$d/b")" = "$inode_b" ]
    [ "$(listing)" = "$before" ]
}

@test "a copy in a read-only folder is reported and the rest is linked" {
    fill "$d/a" 200 r
    fill "$d/locked/b" 200 r
    fill "$d/open/c" 200 r
    chmod 555 "$d/locked"
    run --separate-stderr dupes link "$d"
    assert_failure 1
    [ "$stderr" = "dupes.sh: cannot link locked/b" ]
    assert_output "linked open/c -> a
reclaimed: 200 bytes"
    [ "$(inode "$d/open/c")" = "$(inode "$d/a")" ]
    [ "$(cat "$d/locked/b" | wc -c)" = 200 ]
    [ "$(ls -A "$d/locked")" = "b" ]
}

@test "--min-size limits what is linked" {
    fill "$d/big1" 3000 b
    fill "$d/big2" 3000 b
    fill "$d/small1" 10 s
    fill "$d/small2" 10 s
    run dupes link --min-size 1K "$d"
    assert_success
    assert_output "linked big2 -> big1
reclaimed: 3000 bytes"
    [ "$(inode "$d/small1")" != "$(inode "$d/small2")" ]
}

@test "--dry-run is only for link" {
    run --separate-stderr dupes scan --dry-run "$d"
    assert_failure 2
    [ "$stderr" = "usage: dupes.sh scan|report|link [options] DIR" ]
}
