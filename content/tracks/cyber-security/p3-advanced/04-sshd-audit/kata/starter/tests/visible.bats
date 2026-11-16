setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "the first PermitRootLogin of the example wins" {
    run bash audit.sh sshd_config.example
    assert_line --index 0 "HIGH PermitRootLogin yes: root may log in with a password"
}

@test "the example ends with the summary and exits 1" {
    run bash audit.sh sshd_config.example
    assert_failure 1
    assert_line --index 4 "findings: 4 (high: 2)"
}
