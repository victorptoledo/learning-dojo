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

commit() {
    git -C "$repo" commit -q --allow-empty -m "$1"
}

shop_graph() {
    commit "Initial"
    git -C "$repo" branch fix/vat
    commit "Add cart"
    git -C "$repo" switch -q fix/vat
    commit "Fix VAT"
    git -C "$repo" switch -q main
    git -C "$repo" merge -q --no-ff -m "Merge fix/vat" fix/vat
    git -C "$repo" switch -q -c feature/search
    commit "Index products"
    commit "Search box"
    git -C "$repo" switch -q main
    commit "Add tax rules"
    git -C "$repo" branch docs
}

@test "prints the report for every branch" {
    shop_graph
    run bash branches.sh "$repo"
    assert_success
    assert_output "docs ahead 0 behind 0 merged
feature/search ahead 2 behind 1 open
fix/vat ahead 0 behind 3 merged
total: 3 branches, 2 merged"
}

@test "compares against the BASE given" {
    shop_graph
    git -C "$repo" branch develop feature/search
    run bash branches.sh "$repo" develop
    assert_success
    assert_output "docs ahead 1 behind 2 open
feature/search ahead 0 behind 0 merged
fix/vat ahead 0 behind 4 merged
main ahead 1 behind 2 open
total: 4 branches, 2 merged"
}

@test "works from another checked-out branch and changes nothing" {
    shop_graph
    git -C "$repo" switch -q feature/search
    before=$(git -C "$repo" for-each-ref)
    run bash branches.sh "$repo"
    assert_success
    assert_line --index 1 "feature/search ahead 2 behind 1 open"
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "feature/search"
    [ "$(git -C "$repo" for-each-ref)" = "$before" ]
}

@test "a tag named like a branch does not confuse it" {
    shop_graph
    git -C "$repo" tag docs "main~1"
    git -C "$repo" tag fix/vat main
    run bash branches.sh "$repo"
    assert_success
    assert_line --index 0 "docs ahead 0 behind 0 merged"
    assert_line --index 2 "fix/vat ahead 0 behind 3 merged"
}

@test "a repository with only the base branch has no branches to report" {
    commit "Initial"
    run bash branches.sh "$repo"
    assert_success
    assert_output "total: 0 branches, 0 merged"
}

@test "a missing base branch is an error" {
    commit "Initial"
    run --separate-stderr bash branches.sh "$repo" develop
    assert_failure 1
    assert_output ""
    [ "$stderr" = "branches.sh: no branch develop" ]
}

@test "a folder that is not a repository is an error" {
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash branches.sh "$BATS_TEST_TMPDIR/plain"
    assert_failure 1
    [ "$stderr" = "branches.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}

@test "a branch that diverged a lot is counted on both sides" {
    commit "Initial"
    git -C "$repo" switch -q -c long-running
    for n in 1 2 3 4; do commit "Feature step $n"; done
    git -C "$repo" switch -q main
    for n in 1 2 3 4 5 6; do commit "Main step $n"; done
    run bash branches.sh "$repo"
    assert_output "long-running ahead 4 behind 6 open
total: 1 branches, 0 merged"
}

@test "wrong number of arguments prints the usage and exits 2" {
    run --separate-stderr bash branches.sh
    assert_failure 2
    [ "$stderr" = "usage: branches.sh REPO [BASE]" ]
    run --separate-stderr bash branches.sh "$repo" main extra
    assert_failure 2
    [ "$stderr" = "usage: branches.sh REPO [BASE]" ]
}
