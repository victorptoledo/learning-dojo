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
    test_cmd='grep -q "total: 41.50" receipt.txt'
}

# history N BAD [BROKEN...] - N commits "Change 1".."Change N"; from commit BAD on the total
# is wrong; the commits listed in BROKEN contain a file named "broken".
history() {
    local total=$1 bad=$2 n
    shift 2
    for n in $(seq 1 "$total"); do
        if [ "$n" -lt "$bad" ]; then
            echo "total: 41.50" > "$repo/receipt.txt"
        else
            echo "total: 40.00" > "$repo/receipt.txt"
        fi
        rm -f "$repo/broken"
        case " $* " in *" $n "*) touch "$repo/broken" ;; esac
        echo "change $n" > "$repo/notes.txt"
        git -C "$repo" add -A
        git -C "$repo" commit -q -m "Change $n"
        if [ "$n" -eq 1 ]; then git -C "$repo" tag v1.0.0; fi
    done
}

hash_of() {
    git -C "$repo" log --format=%H --grep="^$1\$"
}

@test "prints the full hash and subject of the first bad commit" {
    history 20 13
    run bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_success
    assert_output "$(hash_of "Change 13") Change 13"
}

@test "leaves no bisect in progress, the branch checked out and a clean tree" {
    history 20 13
    bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    run git -C "$repo" bisect log
    assert_failure
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "main"
    run git -C "$repo" status --porcelain
    assert_output ""
    [ "$(git -C "$repo" rev-parse HEAD)" = "$(hash_of "Change 20")" ]
}

@test "commits the command cannot test are skipped" {
    history 20 13 5 6 17
    run bash hunt.sh "$repo" v1.0.0 "[ -e broken ] && exit 125; $test_cmd"
    assert_success
    assert_output "$(hash_of "Change 13") Change 13"
}

@test "finds a culprit right after GOOD and one at HEAD" {
    history 9 2
    run bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_output "$(hash_of "Change 2") Change 2"
    rm -rf "$repo"
    git init -q "$repo"
    history 9 9
    run bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_output "$(hash_of "Change 9") Change 9"
}

@test "returns to the branch it started on" {
    history 12 7
    git -C "$repo" switch -q -c release/1.x
    run bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_success
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "release/1.x"
}

@test "refuses when HEAD passes the test" {
    history 6 99
    run --separate-stderr bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_failure 1
    assert_output ""
    [ "$stderr" = "hunt.sh: HEAD passes the test, nothing to hunt" ]
}

@test "refuses when GOOD fails too, and goes back to the branch" {
    history 6 1
    run --separate-stderr bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_failure 1
    [ "$stderr" = "hunt.sh: v1.0.0 fails the test too" ]
    run git -C "$repo" symbolic-ref --short HEAD
    assert_output "main"
    run git -C "$repo" bisect log
    assert_failure
}

@test "refuses an unknown commit" {
    history 6 3
    run --separate-stderr bash hunt.sh "$repo" v9.9.9 "$test_cmd"
    assert_failure 1
    [ "$stderr" = "hunt.sh: unknown commit v9.9.9" ]
}

@test "refuses uncommitted changes" {
    history 6 3
    echo "local" >> "$repo/notes.txt"
    run --separate-stderr bash hunt.sh "$repo" v1.0.0 "$test_cmd"
    assert_failure 1
    [ "$stderr" = "hunt.sh: uncommitted changes in $repo" ]
    run git -C "$repo" status --porcelain
    assert_output " M notes.txt"
}

@test "wrong arguments print the usage, a plain folder is an error" {
    run --separate-stderr bash hunt.sh "$repo" HEAD
    assert_failure 2
    [ "$stderr" = "usage: hunt.sh REPO GOOD CMD" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash hunt.sh "$BATS_TEST_TMPDIR/plain" HEAD true
    assert_failure 1
    [ "$stderr" = "hunt.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
