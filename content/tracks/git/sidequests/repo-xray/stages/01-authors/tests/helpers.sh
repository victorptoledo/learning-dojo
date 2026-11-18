# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
git config --global user.name "Dojo Tester"
git config --global user.email "tester@dojo.test"
git config --global init.defaultBranch main
git config --global advice.detachedHead false

GITX="$BATS_TEST_DIRNAME/../gitx.sh"
repo="$BATS_TEST_TMPDIR/shop"
git init -q "$repo"

gitx() {
    bash "$GITX" "$@"
}

# put FILE TEXT: writes TEXT (printf format, so \n works) into REPO/FILE, creating folders.
put() {
    mkdir -p "$(dirname "$repo/$1")"
    # shellcheck disable=SC2059
    printf "$2" > "$repo/$1"
}

# lines FILE FROM TO: FILE gets the numbered lines FROM..TO (one per line).
lines() {
    mkdir -p "$(dirname "$repo/$1")"
    seq "$2" "$3" | sed 's/^/line /' > "$repo/$1"
}

# commit_as "NAME <EMAIL>" DATE MESSAGE: stages everything and commits it as that author,
# at that date (YYYY-MM-DD, noon UTC).
commit_as() {
    local who=$1 date="$2T12:00:00Z" name email
    name=${who% <*}
    email=${who##*<}
    email=${email%>}
    git -C "$repo" add -A
    GIT_AUTHOR_NAME=$name GIT_AUTHOR_EMAIL=$email GIT_AUTHOR_DATE=$date \
        GIT_COMMITTER_NAME=$name GIT_COMMITTER_EMAIL=$email GIT_COMMITTER_DATE=$date \
        git -C "$repo" commit -q --allow-empty -m "$3"
}

# snapshot: everything about the repository that the script must not change.
snapshot() {
    git -C "$repo" for-each-ref --format='%(refname) %(objectname)'
    git -C "$repo" rev-parse HEAD 2>/dev/null || true
    git -C "$repo" symbolic-ref -q HEAD || true
    git -C "$repo" status --porcelain=v1 --untracked-files=all
    git -C "$repo" config --local --list
}
