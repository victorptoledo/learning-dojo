setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "prints the header first" {
    run bash sales.sh sample.csv
    assert_success
    assert_line --index 0 "region,orders,units,revenue"
}
