setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    repo="$BATS_TEST_TMPDIR/projects/shop"
}

@test "creates the repository with one commit and reports it" {
    run bash newrepo.sh "$repo"
    assert_success
    assert_output "created $repo on branch main"
    run git -C "$repo" log --format=%s
    assert_output "Initial commit"
}

@test "the branch is main" {
    bash newrepo.sh "$repo"
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "main"
}

@test "uses main even when init.defaultBranch says otherwise" {
    git config --global init.defaultBranch trunk
    bash newrepo.sh "$repo"
    run git -C "$repo" branch --format='%(refname:short)'
    assert_output "main"
}

@test "the README is named after the last part of DIR" {
    bash newrepo.sh "$repo"
    run git -C "$repo" show HEAD:README.md
    assert_output "# shop"
}

@test "commits exactly README.md and .gitignore" {
    bash newrepo.sh "$repo"
    run git -C "$repo" ls-files
    assert_output ".gitignore
README.md"
    run git -C "$repo" show HEAD:.gitignore
    assert_output "*.log
.env"
}

@test "ignores log files and .env and leaves a clean working tree" {
    bash newrepo.sh "$repo"
    touch "$repo/app.log" "$repo/.env"
    run git -C "$repo" check-ignore app.log .env
    assert_success
    run git -C "$repo" status --porcelain
    assert_output ""
}

@test "keeps an existing README and leaves other files untracked" {
    mkdir -p "$repo"
    echo "Shop service" > "$repo/README.md"
    echo "draft" > "$repo/notes.txt"
    bash newrepo.sh "$repo"
    run git -C "$repo" show HEAD:README.md
    assert_output "Shop service"
    run git -C "$repo" status --porcelain
    assert_output "?? notes.txt"
}

@test "commits with the configured identity" {
    bash newrepo.sh "$repo"
    run git -C "$repo" log -1 --format='%an <%ae>'
    assert_output "Dojo Tester <tester@dojo.test>"
}

@test "refuses a directory that is already a repository" {
    git init -q "$repo"
    run --separate-stderr bash newrepo.sh "$repo"
    assert_failure 1
    assert_output ""
    [ "$stderr" = "newrepo.sh: $repo is already a git repository" ]
    run git -C "$repo" rev-parse --verify -q HEAD
    assert_failure
}

@test "without exactly one argument prints the usage and exits 2" {
    run --separate-stderr bash newrepo.sh
    assert_failure 2
    [ "$stderr" = "usage: newrepo.sh DIR" ]
    run --separate-stderr bash newrepo.sh a b
    assert_failure 2
    [ "$stderr" = "usage: newrepo.sh DIR" ]
}
