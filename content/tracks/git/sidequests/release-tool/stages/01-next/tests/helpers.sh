# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
git config --global user.name "Release Bot"
git config --global user.email "release@shop.example"
git config --global init.defaultBranch main
git config --global advice.detachedHead false

RELEASE="$BATS_TEST_DIRNAME/../release.sh"
repo="$BATS_TEST_TMPDIR/shop"
git init -q "$repo"
day=0

release() {
    bash "$RELEASE" "$@"
}

# commit MESSAGE: a commit that adds changes/DAY.txt, with MESSAGE (printf format, so "\n\n"
# starts a body). Each commit is one day after the previous one, from 2024-03-01.
commit() {
    day=$((day + 1))
    local date
    date=$(printf '2024-03-%02dT12:00:00Z' "$day")
    mkdir -p "$repo/changes"
    echo "change $day" > "$repo/changes/$day.txt"
    git -C "$repo" add changes
    # shellcheck disable=SC2059
    printf "$1" > "$BATS_TEST_TMPDIR/msg"
    GIT_AUTHOR_DATE=$date GIT_COMMITTER_DATE=$date \
        git -C "$repo" commit -q -F "$BATS_TEST_TMPDIR/msg"
}

# short REV: the first 7 characters of the commit's hash.
short() {
    git -C "$repo" rev-parse "$1" | cut -c1-7
}

# release_tag NAME: an annotated tag on HEAD.
release_tag() {
    git -C "$repo" tag -a -m "Release $1" "$1"
}

# refs: every ref with what it points at, to prove a command changed nothing.
refs() {
    git -C "$repo" for-each-ref --format='%(refname) %(objectname)'
    git -C "$repo" status --porcelain=v1 --untracked-files=all
}
