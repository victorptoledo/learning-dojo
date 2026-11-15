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

# change "NAME EMAIL" SUBJECT FILE... - appends a line to each file and commits as that author.
change() {
    local who="$1" subject="$2" file
    shift 2
    for file in "$@"; do
        mkdir -p "$(dirname "$repo/$file")"
        echo "$subject" >> "$repo/$file"
    done
    git -C "$repo" add -A
    GIT_AUTHOR_NAME="${who% *}" GIT_AUTHOR_EMAIL="${who##* }" \
        git -C "$repo" commit -q -m "$subject"
}

shop_history() {
    change "Ada ada@shop.test" "Initial commit" README.md
    change "Ada ada@shop.test" "Add cart" src/cart.sh
    change "Bob bob@shop.test" "Fix cart total" src/cart.sh
    change "Bob bob@shop.test" "Document the cart" README.md docs/cart.md
    change "Ada ada@shop.test" "Add tax rules" src/cart.sh src/tax.sh
}

@test "prints the whole report" {
    shop_history
    run bash gitstats.sh "$repo"
    assert_success
    assert_output "commits: 5
authors: 2
first: Initial commit
last: Add tax rules
top files:
3 src/cart.sh
2 README.md
1 docs/cart.md"
}

@test "only counts what is reachable from HEAD" {
    shop_history
    git -C "$repo" switch -q -c experiment
    change "Eve eve@shop.test" "Try a new cart" src/cart.sh
    change "Eve eve@shop.test" "Try again" src/cart.sh
    git -C "$repo" switch -q main
    run bash gitstats.sh "$repo"
    assert_line --index 0 "commits: 5"
    assert_line --index 1 "authors: 2"
    assert_line --index 3 "last: Add tax rules"
    assert_line --index 5 "3 src/cart.sh"
}

@test "authors are told apart by e-mail address" {
    change "Ada ada@shop.test" "One" a.txt
    change "Ada ada@home.test" "Two" a.txt
    change "Ada ada@shop.test" "Three" a.txt
    run bash gitstats.sh "$repo"
    assert_line --index 1 "authors: 2"
}

@test "breaks ties in alphabetical order and lists fewer files when there are fewer" {
    change "Ada ada@shop.test" "Start" zeta.txt alpha.txt
    run bash gitstats.sh "$repo"
    assert_output "commits: 1
authors: 1
first: Start
last: Start
top files:
1 alpha.txt
1 zeta.txt"
}

@test "merge commits count as commits but not as file changes" {
    shop_history
    git -C "$repo" switch -q -c feature HEAD~1
    change "Bob bob@shop.test" "Add coupons" src/coupon.sh
    git -C "$repo" switch -q main
    GIT_AUTHOR_NAME=Ada GIT_AUTHOR_EMAIL=ada@shop.test \
        git -C "$repo" merge -q --no-ff -m "Merge coupons" feature
    run bash gitstats.sh "$repo"
    assert_output "commits: 7
authors: 2
first: Initial commit
last: Merge coupons
top files:
3 src/cart.sh
2 README.md
1 docs/cart.md"
}

@test "a repository without commits prints zeros" {
    run bash gitstats.sh "$repo"
    assert_success
    assert_output "commits: 0
authors: 0
first: -
last: -
top files:"
}

@test "works with a path that contains spaces" {
    repo="$BATS_TEST_TMPDIR/my shop"
    git init -q "$repo"
    change "Ada ada@shop.test" "Initial commit" "read me.txt"
    run bash gitstats.sh "$repo"
    assert_success
    assert_line --index 0 "commits: 1"
    assert_line --index 5 "1 read me.txt"
}

@test "a folder that is not a repository is reported on stderr with exit 1" {
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr bash gitstats.sh "$BATS_TEST_TMPDIR/plain"
    assert_failure 1
    assert_output ""
    [ "$stderr" = "gitstats.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}

@test "without exactly one argument prints the usage and exits 2" {
    run --separate-stderr bash gitstats.sh
    assert_failure 2
    [ "$stderr" = "usage: gitstats.sh REPO" ]
    run --separate-stderr bash gitstats.sh "$repo" "$repo"
    assert_failure 2
    [ "$stderr" = "usage: gitstats.sh REPO" ]
}
