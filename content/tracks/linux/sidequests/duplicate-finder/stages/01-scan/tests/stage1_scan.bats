setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    d="$BATS_TEST_TMPDIR/data"
    mkdir -p "$d"
}

@test "lists the files with the same content, sorted" {
    put "$d/a.txt" "hello"
    put "$d/notes/copy.txt" "hello"
    put "$d/other.txt" "world"
    put "$d/b.txt" "hello"
    run --separate-stderr dupes scan "$d"
    assert_success
    assert_output "a.txt
b.txt
notes/copy.txt"
}

@test "separates groups with an empty line, sorted by their first path" {
    put "$d/z/2" "xx"
    put "$d/b/1" "yyy"
    put "$d/a/1" "xx"
    put "$d/c/1" "yyy"
    run dupes scan "$d"
    assert_success
    assert_output "a/1
z/2

b/1
c/1"
}

@test "files of the same size with different content are not duplicates" {
    fill "$d/zeros" 4096 0
    fill "$d/ones" 4096 1
    fill "$d/twos" 4096 2
    run dupes scan "$d"
    assert_success
    assert_output ""
}

@test "handles names with spaces and leading dashes" {
    put "$d/my docs/report 1.txt" "same bytes"
    put "$d/-draft.txt" "same bytes"
    put "$d/report copy.txt" "same bytes"
    run dupes scan "$d"
    assert_success
    assert_output -- "-draft.txt
my docs/report 1.txt
report copy.txt"
}

@test "ignores empty files and symbolic links" {
    put "$d/a.txt" "data"
    put "$d/b.txt" "data"
    : > "$d/empty1"
    : > "$d/empty2"
    ln -s a.txt "$d/link-to-a"
    run dupes scan "$d"
    assert_success
    assert_output "a.txt
b.txt"
}

@test "never opens a file whose size no other file has" {
    put "$d/a.txt" "pair"
    put "$d/b.txt" "pair"
    fill "$d/secret.bin" 777 s
    chmod 000 "$d/secret.bin"
    run --separate-stderr dupes scan "$d"
    assert_success
    assert_output "a.txt
b.txt"
    [ -z "$stderr" ]
}

@test "prints nothing when there are no duplicates" {
    put "$d/a" "1"
    put "$d/b" "22"
    run --separate-stderr dupes scan "$d"
    assert_success
    assert_output ""
}

@test "rejects bad arguments" {
    run --separate-stderr dupes
    assert_failure 2
    [ "$stderr" = "usage: dupes.sh scan|report|link [options] DIR" ]
    run --separate-stderr dupes frobnicate "$d"
    assert_failure 2
    [ "$stderr" = "usage: dupes.sh scan|report|link [options] DIR" ]
    run --separate-stderr dupes scan "$d" "$d"
    assert_failure 2
    [ "$stderr" = "usage: dupes.sh scan|report|link [options] DIR" ]
    run --separate-stderr dupes scan "$d/missing"
    assert_failure 2
    [ "$stderr" = "dupes.sh: not a directory: $d/missing" ]
}
