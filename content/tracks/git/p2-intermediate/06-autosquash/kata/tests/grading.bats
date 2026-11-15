setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    repo="$BATS_TEST_TMPDIR/shop"
    git init -q "$repo"
    echo "# Shop" > "$repo/README.md"
    git -C "$repo" add README.md
    git -C "$repo" commit -q -m "Initial commit"
    git -C "$repo" switch -q -c feature
}

# put FILE TEXT SUBJECT - writes TEXT into FILE and commits it with SUBJECT.
put() {
    printf '%s\n' "$2" > "$repo/$1"
    git -C "$repo" add "$1"
    git -C "$repo" commit -q -m "$3"
}

# fix FILE TEXT TARGET-SUBJECT [fixup|squash] - a fixup! or squash! commit for the commit with that subject.
fix() {
    local target
    target=$(git -C "$repo" log --format=%H --grep="^$3\$" -n 1)
    printf '%s\n' "$2" > "$repo/$1"
    git -C "$repo" add "$1"
    if [ "${4:-fixup}" = squash ]; then
        git -C "$repo" commit -q --squash="$target" -m "Mention the separator"
    else
        git -C "$repo" commit -q --fixup="$target"
    fi
}

review_round() {
    put export.txt "export v1" "Add export"
    put header.txt "header v1" "Add CSV header"
    fix export.txt "export v2" "Add export"
    put totals.txt "totals" "Add totals"
    fix header.txt "header v2" "Add CSV header"
}

@test "folds every fixup into its target and reports how many" {
    review_round
    run bash tidy.sh "$repo" main
    assert_success
    assert_output "folded 2 commits"
    run git -C "$repo" log --format=%s main..HEAD
    assert_output "Add totals
Add CSV header
Add export"
}

@test "the final content does not change" {
    review_round
    before=$(git -C "$repo" rev-parse "HEAD^{tree}")
    bash tidy.sh "$repo" main
    [ "$(git -C "$repo" rev-parse "HEAD^{tree}")" = "$before" ]
}

@test "each fix lands in the commit it targets" {
    review_round
    bash tidy.sh "$repo" main
    run git -C "$repo" show "HEAD~2:export.txt"
    assert_output "export v2"
    run git -C "$repo" show "HEAD~1:header.txt"
    assert_output "header v2"
    run git -C "$repo" show --format= --name-only HEAD~2
    assert_output "export.txt"
}

@test "squash! commits are folded and keep the target subject first" {
    put export.txt "export v1" "Add export"
    fix export.txt "export v2" "Add export" squash
    run bash tidy.sh "$repo" main
    assert_success
    assert_output "folded 1 commits"
    run git -C "$repo" log --format=%s main..HEAD
    assert_output "Add export"
    run git -C "$repo" log -1 --format=%B
    assert_line "Mention the separator"
}

@test "with nothing to fold the branch is not rewritten" {
    put export.txt "export" "Add export"
    put header.txt "header" "Add CSV header"
    before=$(git -C "$repo" rev-parse HEAD)
    run bash tidy.sh "$repo" main
    assert_success
    assert_output "nothing to fold"
    [ "$(git -C "$repo" rev-parse HEAD)" = "$before" ]
}

@test "does not move the branch onto a newer base" {
    review_round
    git -C "$repo" switch -q main
    put news.txt "news" "Add news page"
    git -C "$repo" switch -q feature
    base_before=$(git -C "$repo" merge-base main feature)
    bash tidy.sh "$repo" main
    [ "$(git -C "$repo" merge-base main feature)" = "$base_before" ]
    assert_file_not_exist "$repo/news.txt"
}

@test "a fixup that conflicts is aborted and nothing changes" {
    put version.txt "v1" "Set version"
    put version.txt "v2" "Bump version"
    fix version.txt "v2-fixed" "Set version"
    before=$(git -C "$repo" rev-parse HEAD)
    run --separate-stderr bash tidy.sh "$repo" main
    assert_failure 1
    [ "$stderr" = "tidy.sh: cannot fold the fixups without conflicts" ]
    [ "$(git -C "$repo" rev-parse HEAD)" = "$before" ]
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "feature"
    run git -C "$repo" status --porcelain
    assert_output ""
}

@test "refuses uncommitted changes" {
    review_round
    before=$(git -C "$repo" rev-parse HEAD)
    echo "draft" >> "$repo/totals.txt"
    run --separate-stderr bash tidy.sh "$repo" main
    assert_failure 1
    [ "$stderr" = "tidy.sh: uncommitted changes in $repo" ]
    [ "$(git -C "$repo" rev-parse HEAD)" = "$before" ]
}

@test "an unknown base is an error" {
    review_round
    run --separate-stderr bash tidy.sh "$repo" develop
    assert_failure 1
    [ "$stderr" = "tidy.sh: unknown base develop" ]
}

@test "wrong arguments print the usage, a plain folder is an error" {
    run --separate-stderr bash tidy.sh "$repo"
    assert_failure 2
    [ "$stderr" = "usage: tidy.sh REPO BASE" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash tidy.sh "$BATS_TEST_TMPDIR/plain" main
    assert_failure 1
    [ "$stderr" = "tidy.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
