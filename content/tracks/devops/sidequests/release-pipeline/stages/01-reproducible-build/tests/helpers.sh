# Shared by the tests of every stage (sourced by their setup).
load /opt/dojo/devops

# A copy of the learner's repository to work in, without the grading tests.
repo="$BATS_TEST_TMPDIR/repo"
mkdir -p "$repo"
cp -r "$KATA/." "$repo/"
rm -rf "$repo/tests" "$repo/dist"

SDE=1700000000

# build ARGS...: the copy's build.sh from an unrelated directory, with SOURCE_DATE_EPOCH.
build() {
    (cd "$BATS_TEST_TMPDIR" && SOURCE_DATE_EPOCH=$SDE timeout 60 bash "$repo/scripts/build.sh" "$@" 3>&-)
}

# build_in DIR ARGS...: build.sh of another copy of the repository.
build_in() {
    local dir=$1
    shift
    (cd "$BATS_TEST_TMPDIR" && SOURCE_DATE_EPOCH=$SDE timeout 60 bash "$dir/scripts/build.sh" "$@" 3>&-)
}

# sha FILE: the SHA-256 of FILE.
sha() {
    sha256sum "$1" | cut -d' ' -f1
}

# listing ARCHIVE: one line per entry: mode owner mtime path.
listing() {
    tar --numeric-owner --full-time -tvzf "$1" \
        | awk '{ print $1, $2, $4 "T" $5, $6 }'
}

# unpack ARCHIVE: unpacks into a fresh folder and prints its path.
unpack() {
    local dir
    dir=$(mktemp -d "$BATS_TEST_TMPDIR/unpack.XXXX")
    tar -xzf "$1" -C "$dir"
    echo "$dir"
}
