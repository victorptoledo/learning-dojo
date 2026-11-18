setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    d="$BATS_TEST_TMPDIR/dir"
    mkdir -p "$d"
}

@test "hash: blob ids of text, empty and binary files" {
    printf 'hello\n' > "$d/hello.txt"
    : > "$d/empty"
    printf 'bin\0\1\2\377\0tail' > "$d/data.bin"
    head -c 100000 /dev/urandom > "$d/random.bin"
    for f in hello.txt empty data.bin random.bin; do
        run --separate-stderr minigit hash blob "$d/$f"
        assert_success
        assert_output "$(git hash-object "$d/$f")"
    done
    no_porcelain
}

@test "hash: a flat tree" {
    printf 'a\n' > "$d/a.txt"
    printf 'b\n' > "$d/b.txt"
    printf 'z\n' > "$d/Z.txt"
    run --separate-stderr minigit hash tree "$d"
    assert_success
    assert_output "$(git_tree "$d")"
    no_porcelain
}

@test "hash: nested trees sort folders as if they ended in a slash" {
    mkdir -p "$d/a" "$d/a-b" "$d/src/lib/deep"
    printf '1\n' > "$d/a.txt"
    printf '2\n' > "$d/a/x"
    printf '3\n' > "$d/a-b/y"
    printf '4\n' > "$d/a0"
    printf '5\n' > "$d/src/lib/deep/z.txt"
    printf '6\n' > "$d/src/main.txt"
    run minigit hash tree "$d"
    assert_success
    assert_output "$(git_tree "$d")"
}

@test "hash: executable files, symbolic links, spaces and dashes" {
    mkdir -p "$d/bin" "$d/my docs"
    printf '#!/bin/sh\necho hi\n' > "$d/bin/run"
    chmod 755 "$d/bin/run"
    printf 'notes\n' > "$d/my docs/read me.txt"
    printf 'dash\n' > "$d/-flag"
    ln -s "my docs/read me.txt" "$d/link"
    run minigit hash tree "$d"
    assert_success
    assert_output "$(git_tree "$d")"
}

@test "hash: empty folders are skipped and .git is never an entry" {
    mkdir -p "$d/empty/also-empty" "$d/kept"
    printf 'k\n' > "$d/kept/file"
    git init -q "$d"
    run minigit hash tree "$d"
    assert_success
    assert_output "$(git_tree "$d")"
    rm -rf "$d"/*  "$d/.git"
    mkdir -p "$d/nothing/here"
    run minigit hash tree "$d"
    assert_success
    assert_output "4b825dc642cb6eb9a060e54bf8d69288fbee4904"
}

@test "hash: the id of this repository's own commit tree" {
    put README.md "# Repo\n"
    put src/app.sh "echo app\n"
    chmod +x "$repo/src/app.sh"
    commit "first"
    run minigit hash tree "$repo"
    assert_success
    assert_output "$(git -C "$repo" rev-parse 'HEAD^{tree}')"
}

@test "minigit rejects bad arguments" {
    printf 'x\n' > "$d/x"
    for args in "" "frobnicate" "hash" "hash blob" "hash commit $d/x" "hash blob $d" \
        "hash tree $d/x" "hash blob $d/x extra"; do
        # shellcheck disable=SC2086
        run --separate-stderr minigit $args
        assert_failure 2
        [ "$stderr" = "usage: minigit.sh COMMAND ARGS" ]
    done
}
