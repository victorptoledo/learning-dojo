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
}

# commit SUBJECT - adds a file named after the subject and commits it.
commit() {
    echo "$1" > "$repo/$(echo "$1" | tr ' ' '-').txt"
    git -C "$repo" add -A
    git -C "$repo" commit -q -m "$1"
}

tip_of() {
    git -C "$repo" rev-parse "$1"
}

shop() {
    commit "Initial commit"
    commit "Add cart"
    commit "Add tax rules"
    commit "Add coupons"
}

@test "undoes a hard reset and brings the files back" {
    shop
    tip=$(tip_of main)
    git -C "$repo" reset -q --hard HEAD~3
    run bash rescue.sh "$repo" main
    assert_success
    assert_output "restored main to $tip Add coupons"
    assert_file_exist "$repo/Add-coupons.txt"
    run git -C "$repo" status --porcelain
    assert_output ""
}

@test "undoes git branch -f on a branch that is not checked out" {
    shop
    git -C "$repo" branch feature
    git -C "$repo" switch -q feature
    commit "Add search"
    tip=$(tip_of feature)
    git -C "$repo" switch -q main
    git -C "$repo" branch -f feature HEAD~2
    run bash rescue.sh "$repo" feature
    assert_success
    assert_output "restored feature to $tip Add search"
    [ "$(tip_of feature)" = "$tip" ]
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "main"
    assert_file_not_exist "$repo/Add-search.txt"
}

@test "undoes a finished rebase" {
    commit "Initial commit"
    git -C "$repo" switch -q -c feature
    commit "Add search"
    commit "Add search box"
    before=$(tip_of feature)
    git -C "$repo" switch -q main
    commit "Add cart"
    git -C "$repo" switch -q feature
    git -C "$repo" rebase -q main
    [ "$(tip_of feature)" != "$before" ]
    run bash rescue.sh "$repo" feature
    assert_success
    assert_output "restored feature to $before Add search box"
    assert_file_not_exist "$repo/Add-cart.txt"
}

@test "only undoes the most recent mistake" {
    shop
    tip=$(tip_of main)
    git -C "$repo" reset -q --hard HEAD~1
    middle=$(tip_of main)
    commit "Add gift cards"
    gift=$(tip_of main)
    git -C "$repo" reset -q --hard "$middle"
    run bash rescue.sh "$repo" main
    assert_success
    assert_output "restored main to $gift Add gift cards"
    [ "$(tip_of main)" != "$tip" ]
}

@test "running it twice gives the same result as once" {
    shop
    tip=$(tip_of main)
    git -C "$repo" reset -q --hard HEAD~2
    bash rescue.sh "$repo" main
    run bash rescue.sh "$repo" main
    assert_success
    assert_output "restored main to $tip Add coupons"
    [ "$(tip_of main)" = "$tip" ]
}

@test "restores a deleted branch at its last tip" {
    shop
    git -C "$repo" switch -q -c feature/search
    commit "Index products"
    git -C "$repo" switch -q main
    git -C "$repo" switch -q feature/search
    commit "Add search box"
    tip=$(tip_of feature/search)
    git -C "$repo" switch -q main
    git -C "$repo" branch -q -D feature/search
    run bash rescue.sh "$repo" feature/search
    assert_success
    assert_output "restored feature/search to $tip Add search box"
    [ "$(tip_of feature/search)" = "$tip" ]
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "main"
}

@test "a deleted branch is not confused with one whose name starts the same" {
    shop
    git -C "$repo" switch -q -c fix
    commit "Fix VAT"
    fix_tip=$(tip_of fix)
    git -C "$repo" switch -q main
    git -C "$repo" switch -q -c fix-docs
    commit "Fix docs"
    git -C "$repo" switch -q main
    git -C "$repo" branch -q -D fix fix-docs
    run bash rescue.sh "$repo" fix
    assert_success
    assert_output "restored fix to $fix_tip Fix VAT"
}

@test "nothing to undo on a branch without resets or rebases" {
    shop
    run --separate-stderr bash rescue.sh "$repo" main
    assert_failure 1
    assert_output ""
    [ "$stderr" = "rescue.sh: nothing to undo on main" ]
}

@test "no trace of a branch that was never checked out" {
    shop
    run --separate-stderr bash rescue.sh "$repo" ghost
    assert_failure 1
    [ "$stderr" = "rescue.sh: no trace of ghost in the reflog" ]
    run git -C "$repo" show-ref --verify -q refs/heads/ghost
    assert_failure
}

@test "refuses to move a checked-out branch over uncommitted changes" {
    shop
    before=$(git -C "$repo" reset -q --hard HEAD~1 && tip_of main)
    echo "draft" >> "$repo/Add-cart.txt"
    run --separate-stderr bash rescue.sh "$repo" main
    assert_failure 1
    [ "$stderr" = "rescue.sh: uncommitted changes in $repo" ]
    [ "$(tip_of main)" = "$before" ]
    run git -C "$repo" status --porcelain
    assert_output " M Add-cart.txt"
}

@test "wrong arguments print the usage, a plain folder is an error" {
    run --separate-stderr bash rescue.sh "$repo"
    assert_failure 2
    [ "$stderr" = "usage: rescue.sh REPO BRANCH" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash rescue.sh "$BATS_TEST_TMPDIR/plain" main
    assert_failure 1
    [ "$stderr" = "rescue.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
