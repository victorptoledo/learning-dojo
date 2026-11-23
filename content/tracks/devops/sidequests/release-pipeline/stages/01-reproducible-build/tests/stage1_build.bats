bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "builds the three targets by default and prints their paths" {
    run --separate-stderr build 1.2.0
    assert_success
    assert_output "dist/greet-1.2.0-linux-amd64.tar.gz
dist/greet-1.2.0-linux-arm64.tar.gz
dist/greet-1.2.0-darwin-arm64.tar.gz"
    run ls "$repo/dist"
    assert_output "greet-1.2.0-darwin-arm64.tar.gz
greet-1.2.0-linux-amd64.tar.gz
greet-1.2.0-linux-arm64.tar.gz"
}

@test "builds only the targets given, in their order, and drops a leading v" {
    run --separate-stderr build v2.0.1 darwin-arm64 linux-amd64
    assert_success
    assert_output "dist/greet-2.0.1-darwin-arm64.tar.gz
dist/greet-2.0.1-linux-amd64.tar.gz"
}

@test "the archive has one top folder with the tool, its usage text and the license" {
    build 1.2.0 linux-arm64
    run listing "$repo/dist/greet-1.2.0-linux-arm64.tar.gz"
    assert_output "drwxr-xr-x 0/0 2023-11-14T22:13:20 greet-1.2.0/
-rw-r--r-- 0/0 2023-11-14T22:13:20 greet-1.2.0/LICENSE
drwxr-xr-x 0/0 2023-11-14T22:13:20 greet-1.2.0/bin/
-rwxr-xr-x 0/0 2023-11-14T22:13:20 greet-1.2.0/bin/greet
drwxr-xr-x 0/0 2023-11-14T22:13:20 greet-1.2.0/share/
drwxr-xr-x 0/0 2023-11-14T22:13:20 greet-1.2.0/share/greet/
-rw-r--r-- 0/0 2023-11-14T22:13:20 greet-1.2.0/share/greet/usage.txt"
}

@test "the packaged tool knows its version and target" {
    build 1.3.0-rc.1 linux-arm64
    dir=$(unpack "$repo/dist/greet-1.3.0-rc.1-linux-arm64.tar.gz")
    run "$dir/greet-1.3.0-rc.1/bin/greet" --version
    assert_success
    assert_output "greet 1.3.0-rc.1 (linux-arm64)"
    run "$dir/greet-1.3.0-rc.1/bin/greet" Ana
    assert_output "Hello, Ana!"
    cmp "$dir/greet-1.3.0-rc.1/LICENSE" "$repo/LICENSE"
    cmp "$dir/greet-1.3.0-rc.1/share/greet/usage.txt" "$repo/share/greet/usage.txt"
}

@test "two builds of the same sources give the same bytes" {
    build 1.2.0
    first=$(sha "$repo/dist/greet-1.2.0-linux-amd64.tar.gz")
    sleep 1
    build 1.2.0
    second=$(sha "$repo/dist/greet-1.2.0-linux-amd64.tar.gz")
    [ "$first" = "$second" ] || { echo "the second build differs: $first vs $second"; return 1; }
}

@test "a checkout elsewhere, with other times and modes, gives the same bytes" {
    build 1.2.0
    other="$BATS_TEST_TMPDIR/elsewhere/another-name"
    mkdir -p "$other"
    cp -r "$repo/." "$other/"
    rm -rf "$other/dist"
    touch -d "2001-02-03 04:05:06" "$other/LICENSE" "$other/src/greet.sh" "$other/share/greet/usage.txt"
    chmod 600 "$other/LICENSE" "$other/share/greet/usage.txt"
    chmod 700 "$other/src/greet.sh" "$other/share"
    (umask 077 && build_in "$other" 1.2.0)
    for target in linux-amd64 linux-arm64 darwin-arm64; do
        file="greet-1.2.0-$target.tar.gz"
        [ "$(sha "$repo/dist/$file")" = "$(sha "$other/dist/$file")" ] || {
            echo "$file differs between the two checkouts"
            diff <(listing "$repo/dist/$file") <(listing "$other/dist/$file")
            return 1
        }
    done
}

@test "the gzip header has no name and no time" {
    build 1.2.0 linux-amd64
    # Bytes 4..8 of a gzip file are its flags and mtime: 00 00 00 00 00 with -n.
    run bash -c "od -An -tx1 -j3 -N5 '$repo/dist/greet-1.2.0-linux-amd64.tar.gz' | tr -d ' \n'"
    assert_output "0000000000"
}

@test "SOURCE_DATE_EPOCH changes the archive, and so does the version" {
    build 1.2.0 linux-amd64
    first=$(sha "$repo/dist/greet-1.2.0-linux-amd64.tar.gz")
    SDE=1700000999 build 1.2.0 linux-amd64
    [ "$(sha "$repo/dist/greet-1.2.0-linux-amd64.tar.gz")" != "$first" ] || { echo "SOURCE_DATE_EPOCH was ignored"; return 1; }
    run listing "$repo/dist/greet-1.2.0-linux-amd64.tar.gz"
    assert_line --index 0 "drwxr-xr-x 0/0 2023-11-14T22:29:59 greet-1.2.0/"
}

@test "each build starts from an empty dist folder" {
    mkdir -p "$repo/dist"
    touch "$repo/dist/greet-0.9.0-linux-amd64.tar.gz" "$repo/dist/notes.txt"
    build 1.2.0 linux-amd64
    run ls "$repo/dist"
    assert_output "greet-1.2.0-linux-amd64.tar.gz"
}

@test "a wrong version, an unknown target or no argument exit 2" {
    run --separate-stderr build 1.2
    assert_failure 2
    [ "$stderr" = "build.sh: invalid version: 1.2" ] || { echo "stderr: $stderr"; return 1; }
    run --separate-stderr build 01.2.0
    assert_failure 2
    run --separate-stderr build 1.2.0 windows-amd64
    assert_failure 2
    [ "$stderr" = "build.sh: unknown target: windows-amd64" ] || { echo "stderr: $stderr"; return 1; }
    run --separate-stderr build
    assert_failure 2
    [ "$stderr" = "usage: build.sh VERSION [TARGET...]" ] || { echo "stderr: $stderr"; return 1; }
}

@test "SOURCE_DATE_EPOCH is required, as a number of seconds" {
    run --separate-stderr bash -c "cd '$BATS_TEST_TMPDIR' && env -u SOURCE_DATE_EPOCH bash '$repo/scripts/build.sh' 1.2.0"
    assert_failure 2
    [ "$stderr" = "build.sh: SOURCE_DATE_EPOCH is not set" ] || { echo "stderr: $stderr"; return 1; }
    SDE=yesterday run --separate-stderr build 1.2.0
    assert_failure 2
    [ "$stderr" = "build.sh: SOURCE_DATE_EPOCH is not a number of seconds" ] || { echo "stderr: $stderr"; return 1; }
}
