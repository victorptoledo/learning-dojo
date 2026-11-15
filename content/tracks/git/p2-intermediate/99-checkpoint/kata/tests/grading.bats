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

# commit_in DIR FILE TEXT SUBJECT
commit_in() {
    printf '%s\n' "$3" > "$1/$2"
    git -C "$1" add "$2"
    git -C "$1" commit -q -m "$4"
}

# A pushed branch with two commits and a fixup, while a colleague pushes to main.
reviewed_branch() {
    git -C "$repo" switch -q -c feature/export
    commit_in "$repo" export.txt "export v1" "Add export"
    commit_in "$repo" header.txt "header" "Add CSV header"
    printf '%s\n' "export v2" > "$repo/export.txt"
    git -C "$repo" commit -q -am "fixup! Add export"
    git -C "$repo" push -q -u origin feature/export 2> /dev/null
    commit_in "$other" cart.txt "cart" "Add cart"
    git -C "$other" push -q origin main
}

@test "lands the branch with its fixups folded" {
    reviewed_branch
    run bash land.sh "$repo" feature/export
    assert_success
    assert_output "landed feature/export: 2 commits on main"
    run git -C "$origin" log --format=%s main
    assert_output "Add CSV header
Add export
Add cart
Initial commit"
}

@test "the server's main is linear and has the folded content" {
    reviewed_branch
    bash land.sh "$repo" feature/export
    run git -C "$origin" rev-list --merges --count main
    assert_output "0"
    run git -C "$origin" show "main~1:export.txt"
    assert_output "export v2"
}

@test "local main matches the server and is checked out" {
    reviewed_branch
    bash land.sh "$repo" feature/export
    [ "$(git -C "$repo" rev-parse main)" = "$(git -C "$origin" rev-parse main)" ]
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "main"
    assert_file_exist "$repo/cart.txt"
}

@test "deletes the branch locally and on the server" {
    reviewed_branch
    bash land.sh "$repo" feature/export
    run git -C "$repo" show-ref --verify -q refs/heads/feature/export
    assert_failure
    run git -C "$origin" show-ref --verify -q refs/heads/feature/export
    assert_failure
}

@test "lands a branch that was never pushed" {
    git -C "$repo" switch -q -c fix/vat
    commit_in "$repo" vat.txt "vat" "Fix VAT"
    run bash land.sh "$repo" fix/vat
    assert_success
    assert_output "landed fix/vat: 1 commits on main"
    run git -C "$origin" log -1 --format=%s main
    assert_output "Fix VAT"
}

@test "a conflict is aborted and nothing is pushed" {
    git -C "$repo" switch -q -c feature/discount
    printf '%s\n' "# Shop" "discount: 15%" > "$repo/README.md"
    git -C "$repo" commit -q -am "Raise the discount"
    printf '%s\n' "# Shop" "discount: see pricing.md" > "$other/README.md"
    git -C "$other" commit -q -am "Move the discount"
    git -C "$other" push -q origin main
    server_main=$(git -C "$origin" rev-parse main)
    before=$(git -C "$repo" rev-parse feature/discount)
    run --separate-stderr bash land.sh "$repo" feature/discount
    assert_failure 1
    [ "$stderr" = "land.sh: feature/discount conflicts with origin/main" ]
    [ "$(git -C "$origin" rev-parse main)" = "$server_main" ]
    [ "$(git -C "$repo" rev-parse feature/discount)" = "$before" ]
    run git -C "$repo" rev-parse -q --verify REBASE_HEAD
    assert_failure
}

@test "refuses when someone else pushed to the branch" {
    reviewed_branch
    git -C "$other" fetch -q origin
    git -C "$other" switch -q -c feature/export origin/feature/export
    commit_in "$other" review.txt "fix" "Apply review comments"
    git -C "$other" push -q origin feature/export
    server_main=$(git -C "$origin" rev-parse main)
    run --separate-stderr bash land.sh "$repo" feature/export
    assert_failure 1
    [ "$stderr" = "land.sh: origin/feature/export has commits that feature/export does not have" ]
    [ "$(git -C "$origin" rev-parse main)" = "$server_main" ]
}

@test "refuses when local main has unpushed commits" {
    reviewed_branch
    git -C "$repo" switch -q main
    commit_in "$repo" local.txt "local" "Local experiment"
    git -C "$repo" switch -q feature/export
    server_main=$(git -C "$origin" rev-parse main)
    run --separate-stderr bash land.sh "$repo" feature/export
    assert_failure 1
    [ "$stderr" = "land.sh: local main has commits that origin/main does not have" ]
    [ "$(git -C "$origin" rev-parse main)" = "$server_main" ]
}

@test "refuses uncommitted changes and a branch with nothing to land" {
    reviewed_branch
    echo "draft" >> "$repo/header.txt"
    run --separate-stderr bash land.sh "$repo" feature/export
    assert_failure 1
    [ "$stderr" = "land.sh: uncommitted changes in $repo" ]
    git -C "$repo" checkout -q -- header.txt
    git -C "$repo" branch empty origin/main
    run --separate-stderr bash land.sh "$repo" empty
    assert_failure 1
    [ "$stderr" = "land.sh: empty has nothing to land" ]
}

@test "unknown branches, main itself and wrong arguments are refused" {
    run --separate-stderr bash land.sh "$repo" nope
    assert_failure 1
    [ "$stderr" = "land.sh: no branch to land: nope" ]
    run --separate-stderr bash land.sh "$repo" main
    assert_failure 1
    [ "$stderr" = "land.sh: no branch to land: main" ]
    run --separate-stderr bash land.sh "$repo"
    assert_failure 2
    [ "$stderr" = "usage: land.sh REPO BRANCH" ]
}
