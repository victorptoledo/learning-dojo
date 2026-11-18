setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "authors: counts commits and lines per author, most commits first" {
    lines cart.txt 1 10
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "feat: cart"
    lines cart.txt 1 12
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "feat: two more lines"
    lines tax.txt 1 5
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-04 "feat: tax"
    lines cart.txt 3 12
    commit_as "Ana Souza <ana@shop.example>" 2024-01-05 "refactor: drop two lines"
    run --separate-stderr gitx authors "$repo"
    assert_success
    assert_output "$(printf '3\t+12\t-2\tAna Souza\n1\t+5\t-0\tBruno Lima')"
}

@test "authors: ties are sorted by name in byte order" {
    put a.txt "a\n"
    commit_as "carla <carla@shop.example>" 2024-01-02 "c"
    put b.txt "b\n"
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-03 "b"
    put c.txt "c\nc\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-04 "a"
    run gitx authors "$repo"
    assert_success
    assert_output "$(printf '1\t+2\t-0\tAna Souza\n1\t+1\t-0\tBruno Lima\n1\t+1\t-0\tcarla')"
}

@test "authors: merge commits count for nothing" {
    lines base.txt 1 3
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "base"
    git -C "$repo" switch -q -c feature
    lines feature.txt 1 4
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-03 "feature"
    git -C "$repo" switch -q main
    lines main.txt 1 2
    commit_as "Ana Souza <ana@shop.example>" 2024-01-04 "main work"
    GIT_AUTHOR_NAME="Carla Dias" GIT_AUTHOR_EMAIL=carla@shop.example \
        GIT_COMMITTER_NAME="Carla Dias" GIT_COMMITTER_EMAIL=carla@shop.example \
        git -C "$repo" merge -q --no-ff --no-edit feature
    run gitx authors "$repo"
    assert_success
    assert_output "$(printf '2\t+5\t-0\tAna Souza\n1\t+4\t-0\tBruno Lima')"
}

@test "authors: binary files count as zero lines" {
    printf 'PNG\0\1\2\3\0binary' > "$repo/logo.png"
    put readme.md "# Shop\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "logo"
    run gitx authors "$repo"
    assert_success
    assert_output "$(printf '1\t+1\t-0\tAna Souza')"
}

@test "authors: .mailmap folds the names and e-mails of one person" {
    put a.txt "1\n"
    commit_as "Ana <ana@home.example>" 2024-01-02 "a"
    put b.txt "1\n2\n"
    commit_as "ana.souza <ana@work.example>" 2024-01-03 "b"
    put c.txt "1\n"
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-04 "c"
    put .mailmap "Ana Souza <ana@shop.example> <ana@home.example>\nAna Souza <ana@shop.example> <ana@work.example>\n"
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-05 "chore: mailmap"
    run gitx authors "$repo"
    assert_success
    assert_output "$(printf '2\t+3\t-0\tAna Souza\n2\t+3\t-0\tBruno Lima')"
}

@test "authors: only the current branch counts" {
    put a.txt "1\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    git -C "$repo" switch -q -c experiment
    put b.txt "1\n"
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-03 "b"
    git -C "$repo" switch -q main
    run gitx authors "$repo"
    assert_success
    assert_output "$(printf '1\t+1\t-0\tAna Souza')"
}

@test "authors: an empty repository prints nothing" {
    run --separate-stderr gitx authors "$repo"
    assert_success
    assert_output ""
}

@test "authors: does not change the repository" {
    put a.txt "1\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    put wip.txt "not committed\n"
    before=$(snapshot)
    run gitx authors "$repo"
    assert_success
    [ "$(snapshot)" = "$before" ]
}

@test "gitx rejects bad arguments" {
    usage="usage: gitx.sh authors|owners|hotspots REPO [ARGS]"
    run --separate-stderr gitx
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr gitx frobnicate "$repo"
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr gitx authors
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr gitx authors "$repo" extra
    assert_failure 2
    [ "$stderr" = "$usage" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr gitx authors "$BATS_TEST_TMPDIR/plain"
    assert_failure 2
    [ "$stderr" = "gitx.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
    run --separate-stderr gitx authors "$BATS_TEST_TMPDIR/missing"
    assert_failure 2
    [ "$stderr" = "gitx.sh: not a git repository: $BATS_TEST_TMPDIR/missing" ]
}
