# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
git config --global user.name "Dojo Tester"
git config --global user.email "tester@dojo.test"
git config --global init.defaultBranch main
git config --global advice.detachedHead false

SCRUB="$BATS_TEST_DIRNAME/../scrub.sh"
repo="$BATS_TEST_TMPDIR/shop"
git init -q "$repo"
day=0

# Fake secrets, built at run time so this file holds none.
AWS_KEY="AKIA""IOSFODNN7EXAMPLE"
GH_TOKEN="ghp_""0123456789abcdefghijABCDEFGHIJ012345"
KEY_HEADER="-----BEGIN"" OPENSSH PRIVATE KEY-----"

scrub() {
    bash "$SCRUB" "$@"
}

# put FILE TEXT: writes TEXT (printf format) into REPO/FILE, creating folders.
put() {
    mkdir -p "$(dirname "$repo/$1")"
    # shellcheck disable=SC2059
    printf -- "$2" > "$repo/$1"
}

# fill FILE BYTES CHAR: a file of BYTES copies of CHAR.
fill() {
    mkdir -p "$(dirname "$repo/$1")"
    head -c "$2" /dev/zero | tr '\0' "$3" > "$repo/$1"
}

# commit MESSAGE: stages everything and commits it, one day after the previous commit.
commit() {
    day=$((day + 1))
    local date
    date=$(printf '2024-05-%02dT12:00:00Z' "$day")
    git -C "$repo" add -A
    GIT_AUTHOR_NAME="Ana Souza" GIT_AUTHOR_EMAIL=ana@shop.example GIT_AUTHOR_DATE=$date \
        GIT_COMMITTER_DATE=$date git -C "$repo" commit -q --allow-empty -m "$1"
}

# id REV: the full object id of REV.
id() {
    git -C "$repo" rev-parse "$1"
}

# state: refs, HEAD, the index and working tree, and every object, to prove nothing changed.
state() {
    git -C "$repo" for-each-ref --format='%(refname) %(objectname)'
    git -C "$repo" symbolic-ref -q HEAD || true
    git -C "$repo" status --porcelain=v1 --untracked-files=all
    git -C "$repo" cat-file --batch-all-objects --batch-check
}
