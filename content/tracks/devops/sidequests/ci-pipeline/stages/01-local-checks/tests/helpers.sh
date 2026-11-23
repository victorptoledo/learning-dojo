# Shared by the tests of every stage (sourced by their setup).
load /opt/dojo/devops

# A copy of the learner's repository to break on purpose, without the grading tests.
repo="$BATS_TEST_TMPDIR/repo"
mkdir -p "$repo"
cp -r "$KATA/." "$repo/"
rm -rf "$repo/tests" "$repo/reports"

# ci ARGS...: runs the copy's ci.sh from an unrelated directory, with a time limit.
ci() {
    (cd "$BATS_TEST_TMPDIR" && timeout 60 bash "$repo/scripts/ci.sh" "$@" 3>&-)
}

# put FILE TEXT: writes TEXT to FILE inside the copy, creating its folders.
put() {
    mkdir -p "$(dirname "$repo/$1")"
    printf '%s\n' "$2" > "$repo/$1"
}

# A script with a ShellCheck warning (SC2164: cd without || exit).
broken_script() {
    put "$1" '#!/usr/bin/env bash
dir=$1
cd "$dir"
echo ok'
}

# How many .sh files ci.sh lint should check in the copy.
lintable() {
    find "$repo/bin" "$repo/scripts" -type f -name '*.sh' 2> /dev/null | wc -l
}
