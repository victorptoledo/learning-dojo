# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

DUPES="$BATS_TEST_DIRNAME/../dupes.sh"

dupes() {
    bash "$DUPES" "$@"
}

# put FILE TEXT: writes TEXT (no newline added), creating the folders it needs.
put() {
    mkdir -p "$(dirname "$1")"
    printf '%s' "$2" > "$1"
}

# fill FILE BYTES CHAR: a file of BYTES copies of CHAR.
fill() {
    mkdir -p "$(dirname "$1")"
    head -c "$2" /dev/zero | tr '\0' "$3" > "$1"
}

inode() {
    stat -c %i "$1"
}
