setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    # A throwaway identity, so the tests never touch your own Git configuration.
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    repo="$BATS_TEST_TMPDIR/shop"
    git init -q "$repo"
}

# commit FILE TEXT SUBJECT
commit() {
    printf '%s\n' "$2" > "$repo/$1"
    git -C "$repo" add "$1"
    git -C "$repo" commit -q -m "$3"
}

@test "finds the commit that broke the release and tags a hotfix" {
    commit total.txt "total=42" "Add total"
    git -C "$repo" tag -a v1.0.0 -m "Release v1.0.0"
    commit a.txt "a" "Add a"
    commit total.txt "total=41" "Round prices"
    commit b.txt "b" "Add b"
    git -C "$repo" tag -a v1.1.0 -m "Release v1.1.0"
    culprit=$(git -C "$repo" rev-parse HEAD~1)
    run bash hotfix.sh "$repo" v1.0.0 v1.1.0 'grep -qx "total=42" total.txt'
    assert_success
    assert_output "culprit: $culprit Round prices
tagged v1.1.1 on hotfix/v1.1.1"
    run git -C "$repo" show hotfix/v1.1.1:total.txt
    assert_output "total=42"
}
