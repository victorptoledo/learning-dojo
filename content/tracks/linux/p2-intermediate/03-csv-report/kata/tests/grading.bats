setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "totals the sample per region" {
    run bash sales.sh sample.csv
    assert_success
    assert_output "region,orders,units,revenue
East,1,1,25.00
North,3,6,228.50
South,2,6,318.50
TOTAL,6,13,572.00"
}

@test "-m keeps one month" {
    run bash sales.sh -m 2026-10 sample.csv
    assert_success
    assert_output "region,orders,units,revenue
North,1,2,50.00
South,1,4,38.00
TOTAL,2,6,88.00"
}

@test "-r keeps one region and combines with -m" {
    run bash sales.sh -r North -m 2026-11 sample.csv
    assert_success
    assert_output "region,orders,units,revenue
North,2,4,178.50
TOTAL,2,4,178.50"
}

@test "no matching records prints a zero total" {
    run bash sales.sh -r West sample.csv
    assert_success
    assert_output "region,orders,units,revenue
TOTAL,0,0,0.00"
}

@test "invalid records are reported on stderr and skipped" {
    cat > "$BATS_TEST_TMPDIR/bad.csv" <<'EOF'
date,region,product,quantity,unit_price
2026-11-01,North,cable,2,3.5
2026-11-01,North,cable,two,3.50
2026-11-01,North,cable,0,3.50

2026-11-01,North,cable,1,3.505
2026-11-01,North,cable,1
2026-11-01,South,cable,1,4
EOF
    run --separate-stderr bash sales.sh "$BATS_TEST_TMPDIR/bad.csv"
    assert_success
    assert_output "region,orders,units,revenue
North,1,2,7.00
South,1,1,4.00
TOTAL,2,3,11.00"
    [ "$stderr" = "sales.sh: line 3: invalid record
sales.sh: line 4: invalid record
sales.sh: line 6: invalid record
sales.sh: line 7: invalid record" ]
}

@test "accepts Windows line ends" {
    printf 'date,region,product,quantity,unit_price\r\n2026-11-01,East,pen,3,1.25\r\n' > "$BATS_TEST_TMPDIR/crlf.csv"
    run --separate-stderr bash sales.sh "$BATS_TEST_TMPDIR/crlf.csv"
    assert_success
    assert_output "region,orders,units,revenue
East,1,3,3.75
TOTAL,1,3,3.75"
    [ -z "$stderr" ]
}

@test "regions are sorted in byte order" {
    printf 'date,region,product,quantity,unit_price\n2026-11-01,beta,x,1,1\n2026-11-01,Alpha,x,1,1\n2026-11-01,Zulu,x,1,1\n' > "$BATS_TEST_TMPDIR/order.csv"
    run bash sales.sh "$BATS_TEST_TMPDIR/order.csv"
    assert_success
    assert_line --index 1 "Alpha,1,1,1.00"
    assert_line --index 2 "Zulu,1,1,1.00"
    assert_line --index 3 "beta,1,1,1.00"
}

@test "rejects an invalid month" {
    run --separate-stderr bash sales.sh -m 2026-13 sample.csv
    assert_failure 2
    [ "$stderr" = "sales.sh: invalid month: 2026-13" ]
}

@test "prints the usage for an unknown option or a missing file operand" {
    run --separate-stderr bash sales.sh -x sample.csv
    assert_failure 2
    [ "$stderr" = "usage: sales.sh [-m YYYY-MM] [-r REGION] FILE" ]
    run --separate-stderr bash sales.sh -m 2026-11
    assert_failure 2
    [ "$stderr" = "usage: sales.sh [-m YYYY-MM] [-r REGION] FILE" ]
}

@test "reports a file it cannot read" {
    run --separate-stderr bash sales.sh missing.csv
    assert_failure 1
    [ "$stderr" = "sales.sh: cannot read missing.csv" ]
}
