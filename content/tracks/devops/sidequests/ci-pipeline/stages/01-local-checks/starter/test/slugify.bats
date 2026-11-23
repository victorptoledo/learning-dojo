#!/usr/bin/env bats
# The toolbox's own tests: `bats test` (or `scripts/ci.sh test`).

setup() {
    BIN="$BATS_TEST_DIRNAME/../bin"
}

@test "slugify lowercases and joins the words with dashes" {
    run bash "$BIN/slugify.sh" "Hello, World!"
    [ "$status" -eq 0 ]
    [ "$output" = "hello-world" ]
}

@test "slugify joins several arguments" {
    run bash "$BIN/slugify.sh" "Release" "Notes 2.0"
    [ "$output" = "release-notes-2-0" ]
}

@test "slugify needs some text" {
    run bash "$BIN/slugify.sh"
    [ "$status" -eq 2 ]
}
