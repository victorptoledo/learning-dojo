setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    msg="$BATS_TEST_TMPDIR/COMMIT_EDITMSG"
    repo="$BATS_TEST_TMPDIR/shop"
}

# check TEXT - runs the hook on a message file holding TEXT (printf format).
check() {
    # shellcheck disable=SC2059
    printf "$1" > "$msg"
    run --separate-stderr bash commit-msg.sh "$msg"
}

reason() {
    [ "$stderr" = "commit-msg: $1" ]
}

@test "accepts types, scopes, breaking marks and a body after a blank line" {
    for subject in "feat(cart): add coupon codes" "fix: round VAT per line" \
        "refactor(api-v2)!: rename the order endpoints" "chore(deps): bump yaml to 2.4" \
        "feat!: drop the v1 checkout API" "ci: cache the Maven repository"; do
        check "$subject\n"
        assert_success
    done
    check "fix(cart): round VAT per line\n\nThe total was rounded once per order,\nwhich lost a cent on large carts.\n"
    assert_success
}

@test "rejects free-form subjects and unknown types" {
    for subject in "Added coupons" "feature: add coupons" "Fix: round VAT" "fix round VAT" \
        "fix(Cart): round VAT" "fix:  round VAT" "fix:"; do
        check "$subject\n"
        assert_failure 1
        reason "subject must look like 'type(scope): description'"
    done
}

@test "allows 72 characters and rejects 73" {
    subject="fix: $(printf 'a%.0s' $(seq 1 67))"
    [ "${#subject}" -eq 72 ]
    check "$subject\n"
    assert_success
    check "${subject}b\n"
    assert_failure 1
    reason "subject is longer than 72 characters"
}

@test "rejects a body glued to the subject" {
    check "fix: round VAT per line\nThe total was wrong.\n"
    assert_failure 1
    reason "leave the second line empty"
}

@test "ignores comment lines and leading blank lines" {
    check "# Please enter the commit message\n\nfeat: add coupons\n# Lines starting with # are ignored\n"
    assert_success
    check "feat: add coupons\n# On branch main\n# Changes to be committed:\n"
    assert_success
}

@test "an empty or comment-only message is rejected" {
    check "\n\n"
    assert_failure 1
    reason "empty message"
    check "# only a comment\n"
    assert_failure 1
    reason "empty message"
}

@test "messages Git writes itself pass without the format check" {
    for subject in "Merge branch 'feature/search'" 'Revert "feat: add coupons"' \
        "fixup! feat: add coupons" "squash! fix: round VAT" "amend! feat: add coupons"; do
        check "$subject\n"
        assert_success
    done
}

@test "install.sh installs an executable hook into .git/hooks" {
    git init -q "$repo"
    run bash install.sh "$repo"
    assert_success
    assert_output "installed commit-msg hook"
    assert_file_executable "$repo/.git/hooks/commit-msg"
    cmp -s commit-msg.sh "$repo/.git/hooks/commit-msg"
}

@test "once installed, git commit refuses bad messages and accepts good ones" {
    git init -q "$repo"
    bash install.sh "$repo"
    echo "coupons" > "$repo/coupons.txt"
    git -C "$repo" add coupons.txt
    run git -C "$repo" commit -q -m "Added coupons"
    assert_failure
    assert_output --partial "subject must look like 'type(scope): description'"
    run git -C "$repo" log --oneline
    assert_failure
    run git -C "$repo" commit -q -m "feat: add coupons"
    assert_success
    run git -C "$repo" log --format=%s
    assert_output "feat: add coupons"
}

@test "install.sh follows core.hooksPath" {
    git init -q "$repo"
    git -C "$repo" config core.hooksPath .githooks
    run bash install.sh "$repo"
    assert_success
    assert_file_executable "$repo/.githooks/commit-msg"
    assert_file_not_exist "$repo/.git/hooks/commit-msg"
}

@test "install.sh keeps a different existing hook as commit-msg.bak" {
    git init -q "$repo"
    printf '%s\n' '#!/bin/sh' 'exit 0' > "$repo/.git/hooks/commit-msg"
    run bash install.sh "$repo"
    assert_success
    assert_output "saved the previous hook as commit-msg.bak
installed commit-msg hook"
    run cat "$repo/.git/hooks/commit-msg.bak"
    assert_output "#!/bin/sh
exit 0"
    run bash install.sh "$repo"
    assert_output "installed commit-msg hook"
}

@test "install.sh checks its arguments" {
    run --separate-stderr bash install.sh
    assert_failure 2
    [ "$stderr" = "usage: install.sh REPO" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash install.sh "$BATS_TEST_TMPDIR/plain"
    assert_failure 1
    [ "$stderr" = "install.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
