setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "reports the suspect source of the sample" {
    run bash bruteforce.sh sample.log
    assert_line --index 0 "SUSPECT 203.0.113.7 failures=6 users=admin,oracle,root"
}

@test "raises an alert and exits 1 when a suspect source logs in" {
    run bash bruteforce.sh sample.log
    assert_failure 1
    assert_line --index 1 "ALERT 203.0.113.7 accepted user=deploy after 6 failures"
}
