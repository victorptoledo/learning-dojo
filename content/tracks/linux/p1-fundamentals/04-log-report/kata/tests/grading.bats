setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "prints the whole report for the sample" {
    run bash logstats.sh sample.log
    assert_success
    assert_output "requests: 8
visitors: 4
errors: 3
top paths:
3 /index.html
2 /about.html
1 /api/orders"
}

@test "breaks ties in alphabetical order of the path" {
    cat > "$BATS_TEST_TMPDIR/ties.log" <<'EOF'
1.1.1.1 - - [14/Nov/2026:10:00:01 +0000] "GET /zebra HTTP/1.1" 200 1
1.1.1.1 - - [14/Nov/2026:10:00:02 +0000] "GET /alpha HTTP/1.1" 200 1
1.1.1.1 - - [14/Nov/2026:10:00:03 +0000] "GET /mango HTTP/1.1" 200 1
1.1.1.1 - - [14/Nov/2026:10:00:04 +0000] "GET /beta HTTP/1.1" 200 1
EOF
    run bash logstats.sh "$BATS_TEST_TMPDIR/ties.log"
    assert_success
    assert_output "requests: 4
visitors: 1
errors: 0
top paths:
1 /alpha
1 /beta
1 /mango"
}

@test "lists fewer paths when there are fewer than three" {
    printf '%s\n' '2.2.2.2 - - [14/Nov/2026:10:00:01 +0000] "GET /only HTTP/1.1" 503 1' > "$BATS_TEST_TMPDIR/one.log"
    run bash logstats.sh "$BATS_TEST_TMPDIR/one.log"
    assert_success
    assert_output "requests: 1
visitors: 1
errors: 1
top paths:
1 /only"
}

@test "an empty log gives zeros" {
    : > "$BATS_TEST_TMPDIR/empty.log"
    run bash logstats.sh "$BATS_TEST_TMPDIR/empty.log"
    assert_success
    assert_output "requests: 0
visitors: 0
errors: 0
top paths:"
}

@test "a status of 304 or 200 is not an error, 401 and 500 are" {
    printf '%s\n' \
        '1.1.1.1 - - [x +0000] "GET /a HTTP/1.1" 304 0' \
        '1.1.1.1 - - [x +0000] "GET /a HTTP/1.1" 401 0' \
        '1.1.1.1 - - [x +0000] "GET /a HTTP/1.1" 500 0' \
        '1.1.1.1 - - [x +0000] "GET /a HTTP/1.1" 200 0' > "$BATS_TEST_TMPDIR/status.log"
    run bash logstats.sh "$BATS_TEST_TMPDIR/status.log"
    assert_line --index 2 "errors: 2"
}

@test "without an argument prints the usage on stderr and exits 2" {
    run --separate-stderr bash logstats.sh
    assert_failure 2
    assert_output ""
    [ "$stderr" = "usage: logstats.sh FILE" ]
}

@test "with two arguments prints the usage and exits 2" {
    run --separate-stderr bash logstats.sh sample.log sample.log
    assert_failure 2
    [ "$stderr" = "usage: logstats.sh FILE" ]
}

@test "a missing file is reported on stderr with exit 1" {
    run --separate-stderr bash logstats.sh nope.log
    assert_failure 1
    assert_output ""
    [ "$stderr" = "logstats.sh: cannot read nope.log" ]
}
