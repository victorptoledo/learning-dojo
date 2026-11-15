setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    origin="$BATS_TEST_TMPDIR/origin.git"
    repo="$BATS_TEST_TMPDIR/me"
    other="$BATS_TEST_TMPDIR/colleague"
    git init -q --bare "$origin"
    git clone -q "$origin" "$other" 2> /dev/null
    printf '%s\n' "# Shop" "discount: 10%" > "$other/README.md"
    git -C "$other" add README.md
    git -C "$other" commit -q -m "Initial commit"
    git -C "$other" push -q origin main
    git clone -q "$origin" "$repo"
}

# commit_in DIR FILE TEXT SUBJECT - writes TEXT into FILE and commits it.
commit_in() {
    printf '%s\n' "$3" > "$1/$2"
    git -C "$1" add "$2"
    git -C "$1" commit -q -m "$4"
}

# A pushed feature branch with two commits, and two new commits on the server's main.
diverged() {
    git -C "$repo" switch -q -c feature
    commit_in "$repo" search.txt "index" "Index products"
    commit_in "$repo" box.txt "box" "Add search box"
    git -C "$repo" push -q -u origin feature 2> /dev/null
    commit_in "$other" cart.txt "cart" "Add cart"
    commit_in "$other" tax.txt "tax" "Add tax rules"
    git -C "$other" push -q origin main
}

@test "rebases a pushed branch onto origin/main and reports it" {
    diverged
    run bash sync.sh "$repo" feature
    assert_success
    assert_output "feature: 2 commits on top of origin/main"
    run git -C "$repo" log --format=%s feature
    assert_output "Add search box
Index products
Add tax rules
Add cart
Initial commit"
}

@test "the history is linear with origin/main as an ancestor" {
    diverged
    bash sync.sh "$repo" feature
    git -C "$repo" merge-base --is-ancestor origin/main feature
    run git -C "$repo" rev-list --merges --count feature
    assert_output "0"
}

@test "pushes the rebased branch, leaves main on the server alone and checks the branch out" {
    diverged
    server_main=$(git -C "$other" rev-parse main)
    bash sync.sh "$repo" feature
    [ "$(git -C "$origin" rev-parse feature)" = "$(git -C "$repo" rev-parse feature)" ]
    [ "$(git -C "$origin" rev-parse main)" = "$server_main" ]
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "feature"
    assert_file_exist "$repo/cart.txt"
    assert_file_exist "$repo/box.txt"
}

@test "publishes a branch that was never pushed and sets its upstream" {
    git -C "$repo" switch -q -c fix/vat
    commit_in "$repo" vat.txt "vat" "Fix VAT"
    git -C "$repo" switch -q main
    commit_in "$other" cart.txt "cart" "Add cart"
    git -C "$other" push -q origin main
    run bash sync.sh "$repo" fix/vat
    assert_success
    assert_output "fix/vat: 1 commits on top of origin/main"
    [ "$(git -C "$origin" rev-parse fix/vat)" = "$(git -C "$repo" rev-parse fix/vat)" ]
    run git -C "$repo" rev-parse --abbrev-ref "fix/vat@{upstream}"
    assert_output "origin/fix/vat"
}

@test "a conflict is aborted and nothing changes" {
    git -C "$repo" switch -q -c feature
    printf '%s\n' "# Shop" "discount: 15%" > "$repo/README.md"
    git -C "$repo" commit -q -am "Raise the discount"
    git -C "$repo" push -q -u origin feature 2> /dev/null
    before=$(git -C "$repo" rev-parse feature)
    printf '%s\n' "# Shop" "discount: see pricing.md" > "$other/README.md"
    git -C "$other" commit -q -am "Move the discount"
    git -C "$other" push -q origin main
    run --separate-stderr bash sync.sh "$repo" feature
    assert_failure 1
    [ "$stderr" = "sync.sh: feature conflicts with origin/main" ]
    [ "$(git -C "$repo" rev-parse feature)" = "$before" ]
    [ "$(git -C "$origin" rev-parse feature)" = "$before" ]
    run git -C "$repo" rev-parse -q --verify REBASE_HEAD
    assert_failure
    run git -C "$repo" status --porcelain
    assert_output ""
}

@test "refuses when someone else pushed to the branch" {
    diverged
    git -C "$other" fetch -q origin
    git -C "$other" switch -q -c feature origin/feature
    commit_in "$other" review.txt "fix" "Apply review comments"
    git -C "$other" push -q origin feature
    theirs=$(git -C "$other" rev-parse feature)
    run --separate-stderr bash sync.sh "$repo" feature
    assert_failure 1
    [ "$stderr" = "sync.sh: origin/feature has commits that feature does not have" ]
    [ "$(git -C "$origin" rev-parse feature)" = "$theirs" ]
}

@test "refuses uncommitted changes to tracked files" {
    diverged
    before=$(git -C "$repo" rev-parse feature)
    echo "half done" >> "$repo/box.txt"
    run --separate-stderr bash sync.sh "$repo" feature
    assert_failure 1
    [ "$stderr" = "sync.sh: uncommitted changes in $repo" ]
    [ "$(git -C "$repo" rev-parse feature)" = "$before" ]
    run git -C "$repo" status --porcelain
    assert_output " M box.txt"
}

@test "untracked files do not block it" {
    diverged
    echo "notes" > "$repo/notes.txt"
    run bash sync.sh "$repo" feature
    assert_success
    assert_file_exist "$repo/notes.txt"
}

@test "an unknown branch is an error" {
    run --separate-stderr bash sync.sh "$repo" nope
    assert_failure 1
    [ "$stderr" = "sync.sh: no branch nope" ]
}

@test "wrong arguments print the usage and exit 2" {
    run --separate-stderr bash sync.sh "$repo"
    assert_failure 2
    [ "$stderr" = "usage: sync.sh REPO BRANCH" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash sync.sh "$BATS_TEST_TMPDIR/plain" main
    assert_failure 1
    [ "$stderr" = "sync.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
