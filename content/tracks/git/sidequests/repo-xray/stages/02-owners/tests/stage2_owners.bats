setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "owners: lines per author with their share and the bus factor" {
    lines src/cart.txt 1 6
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "cart"
    lines src/tax.txt 1 3
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-03 "tax"
    lines src/cart.txt 1 7
    commit_as "Carla Dias <carla@shop.example>" 2024-01-04 "one more line"
    run --separate-stderr gitx owners "$repo" src
    assert_success
    assert_output "$(printf '6\t60%%\tAna Souza\n3\t30%%\tBruno Lima\n1\t10%%\tCarla Dias\ntotal: 10 lines in 2 files\nbus factor: 1')"
}

@test "owners: the bus factor needs more than half of the lines" {
    lines a.txt 1 2
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    lines b.txt 1 2
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-03 "b"
    run gitx owners "$repo" .
    assert_success
    assert_output "$(printf '2\t50%%\tAna Souza\n2\t50%%\tBruno Lima\ntotal: 4 lines in 2 files\nbus factor: 2')"
    lines c.txt 1 3
    commit_as "Carla Dias <carla@shop.example>" 2024-01-04 "c"
    lines d.txt 1 1
    commit_as "Dan Reis <dan@shop.example>" 2024-01-05 "d"
    run gitx owners "$repo" .
    assert_success
    assert_line --index 4 "total: 8 lines in 4 files"
    assert_line --index 5 "bus factor: 2"
}

@test "owners: shares round to the nearest whole number, halves up" {
    lines a.txt 1 1
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    lines b.txt 1 7
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-03 "b"
    run gitx owners "$repo" .
    assert_success
    assert_output "$(printf '7\t88%%\tBruno Lima\n1\t13%%\tAna Souza\ntotal: 8 lines in 2 files\nbus factor: 1')"
    lines c.txt 1 1
    commit_as "Carla Dias <carla@shop.example>" 2024-01-04 "c"
    run gitx owners "$repo" .
    assert_line --index 0 "$(printf '7\t78%%\tBruno Lima')"
    assert_line --index 1 "$(printf '1\t11%%\tAna Souza')"
    assert_line --index 2 "$(printf '1\t11%%\tCarla Dias')"
}

@test "owners: only the files under PATH, one file or a folder" {
    lines src/billing/invoice.txt 1 4
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "invoice"
    lines "src/billing/my notes.txt" 1 2
    commit_as "Carla Dias <carla@shop.example>" 2024-01-03 "notes"
    lines src/search/index.txt 1 9
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-04 "search"
    run gitx owners "$repo" src/billing
    assert_success
    assert_output "$(printf '4\t67%%\tAna Souza\n2\t33%%\tCarla Dias\ntotal: 6 lines in 2 files\nbus factor: 1')"
    run gitx owners "$repo" "src/billing/my notes.txt"
    assert_success
    assert_output "$(printf '2\t100%%\tCarla Dias\ntotal: 2 lines in 1 files\nbus factor: 1')"
}

@test "owners: binary files are skipped and empty files have no lines" {
    lines app.txt 1 3
    printf 'PNG\0\1\2\0\n\n\n' > "$repo/logo.png"
    : > "$repo/empty.txt"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "app"
    run gitx owners "$repo" .
    assert_success
    assert_output "$(printf '3\t100%%\tAna Souza\ntotal: 3 lines in 2 files\nbus factor: 1')"
}

@test "owners: blames HEAD, not the working tree, and applies .mailmap" {
    lines a.txt 1 3
    commit_as "Ana <ana@home.example>" 2024-01-02 "a"
    put .mailmap "Ana Souza <ana@shop.example> <ana@home.example>\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "mailmap"
    lines a.txt 1 50
    run gitx owners "$repo" a.txt
    assert_success
    assert_output "$(printf '3\t100%%\tAna Souza\ntotal: 3 lines in 1 files\nbus factor: 1')"
}

@test "owners: commits listed in .git-blame-ignore-revs give their lines back" {
    put src/cart.txt "if x:\n  pay()\n  ship()\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "cart"
    put src/cart.txt "if x:\n    pay()\n    ship()\n"
    commit_as "Bot Formatter <bot@shop.example>" 2024-01-03 "style: four spaces"
    run gitx owners "$repo" src
    assert_success
    assert_line --index 0 "$(printf '2\t67%%\tBot Formatter')"
    style=$(git -C "$repo" rev-parse HEAD)
    put .git-blame-ignore-revs "# reformatting commits\n$style\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-04 "chore: ignore the formatting commit"
    run gitx owners "$repo" src
    assert_success
    assert_output "$(printf '3\t100%%\tAna Souza\ntotal: 3 lines in 1 files\nbus factor: 1')"
}

@test "owners: a path without files is an error" {
    lines a.txt 1 2
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    run --separate-stderr gitx owners "$repo" docs
    assert_failure 1
    [ "$stderr" = "gitx.sh: no files to blame under docs" ]
    printf 'PNG\0\1' > "$repo/logo.png"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "logo"
    run --separate-stderr gitx owners "$repo" logo.png
    assert_failure 1
    [ "$stderr" = "gitx.sh: no files to blame under logo.png" ]
    run --separate-stderr gitx owners "$repo"
    assert_failure 2
    run --separate-stderr gitx owners "$repo" a.txt extra
    assert_failure 2
}

@test "owners: works on a repository without commits and leaves it alone" {
    run --separate-stderr gitx owners "$repo" .
    assert_failure 1
    [ "$stderr" = "gitx.sh: no files to blame under ." ]
    lines a.txt 1 2
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    lines a.txt 1 9
    before=$(snapshot)
    run gitx owners "$repo" .
    assert_success
    [ "$(snapshot)" = "$before" ]
}
