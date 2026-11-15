setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "counts the requests of the sample" {
    run bash logstats.sh sample.log
    assert_success
    assert_line --index 0 "requests: 8"
}
