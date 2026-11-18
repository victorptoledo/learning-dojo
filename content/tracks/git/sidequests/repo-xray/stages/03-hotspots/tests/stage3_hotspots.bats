setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "hotspots: files by commits, then churn, then path" {
    lines cart.txt 1 3
    lines tax.txt 1 3
    lines readme.txt 1 1
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "start"
    lines cart.txt 1 10
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "cart"
    lines tax.txt 1 4
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-04 "tax"
    lines cart.txt 2 10
    lines tax.txt 2 4
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-05 "trim"
    run --separate-stderr gitx hotspots "$repo"
    assert_success
    assert_output "$(printf '3\t11\tcart.txt\n3\t5\ttax.txt\n1\t1\treadme.txt')"
}

@test "hotspots: equal counts and churn are sorted by path" {
    put b.txt "1\n"
    put a.txt "1\n"
    put "my file.txt" "1\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "start"
    run gitx hotspots "$repo"
    assert_success
    assert_output "$(printf '1\t1\ta.txt\n1\t1\tb.txt\n1\t1\tmy file.txt')"
}

@test "hotspots: older commits count under the name of today" {
    lines src/pay.txt 1 20
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "pay"
    lines src/pay.txt 1 21
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "one more"
    mkdir -p "$repo/billing"
    git -C "$repo" mv src/pay.txt billing/payment.txt
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-04 "move"
    git -C "$repo" mv billing/payment.txt "billing/pay ment.txt"
    lines "billing/pay ment.txt" 1 22
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-05 "rename and edit"
    lines other.txt 1 2
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-06 "other"
    run gitx hotspots "$repo"
    assert_success
    assert_output "$(printf '4\t22\tbilling/pay ment.txt\n1\t2\tother.txt')"
}

@test "hotspots: a name that comes back later is another file" {
    lines old.txt 1 20
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "old"
    lines old.txt 1 21
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "edit"
    git -C "$repo" mv old.txt new.txt
    commit_as "Ana Souza <ana@shop.example>" 2024-01-04 "rename"
    put old.txt "something else\n"
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-05 "a new old.txt"
    run gitx hotspots "$repo"
    assert_success
    assert_output "$(printf '3\t21\tnew.txt\n1\t1\told.txt')"
}

@test "hotspots: deleted files and merge commits are left out" {
    lines keep.txt 1 2
    lines gone.txt 1 5
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "start"
    lines gone.txt 1 9
    commit_as "Ana Souza <ana@shop.example>" 2024-01-03 "grow"
    git -C "$repo" switch -q -c feature
    lines keep.txt 1 3
    commit_as "Bruno Lima <bruno@shop.example>" 2024-01-04 "feature"
    git -C "$repo" switch -q main
    git -C "$repo" rm -q gone.txt
    commit_as "Ana Souza <ana@shop.example>" 2024-01-05 "remove"
    git -C "$repo" merge -q --no-ff --no-edit feature
    run gitx hotspots "$repo"
    assert_success
    assert_output "$(printf '2\t3\tkeep.txt')"
}

@test "hotspots: --since keeps recent commits and --top limits the list" {
    for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
        put "f$i.txt" "$i\n"
    done
    commit_as "Ana Souza <ana@shop.example>" 2023-06-01 "old"
    put f1.txt "1\n2\n"
    put f2.txt "2\n3\n4\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-03-01 "new"
    put f2.txt "4\n"
    commit_as "Ana Souza <ana@shop.example>" 2024-03-02 "newer"
    run gitx hotspots "$repo"
    assert_success
    assert_line --index 0 "$(printf '3\t5\tf2.txt')"
    assert_line --index 1 "$(printf '2\t2\tf1.txt')"
    assert_line --index 2 "$(printf '1\t1\tf10.txt')"
    assert_line --index 9 "$(printf '1\t1\tf7.txt')"
    [ "${#lines[@]}" -eq 10 ]
    run gitx hotspots "$repo" --since 2024-01-01
    assert_success
    assert_output "$(printf '2\t4\tf2.txt\n1\t1\tf1.txt')"
    run gitx hotspots "$repo" --top 1 --since 2024-01-01
    assert_success
    assert_output "$(printf '2\t4\tf2.txt')"
    run gitx hotspots "$repo" --since 2024-01-01 --top 12
    assert_success
    assert_output "$(printf '2\t4\tf2.txt\n1\t1\tf1.txt')"
}

@test "hotspots: empty repositories, bad options and a repository left alone" {
    run --separate-stderr gitx hotspots "$repo"
    assert_success
    assert_output ""
    lines a.txt 1 2
    commit_as "Ana Souza <ana@shop.example>" 2024-01-02 "a"
    put wip.txt "x\n"
    before=$(snapshot)
    run gitx hotspots "$repo" --top 5
    assert_success
    [ "$(snapshot)" = "$before" ]
    usage="usage: gitx.sh authors|owners|hotspots REPO [ARGS]"
    for args in "--top 0" "--top x" "--top" "--since" "--everything" "extra"; do
        # shellcheck disable=SC2086
        run --separate-stderr gitx hotspots "$repo" $args
        assert_failure 2
        [ "$stderr" = "$usage" ]
    done
}
