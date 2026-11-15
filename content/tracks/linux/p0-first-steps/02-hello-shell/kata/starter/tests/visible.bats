setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "greets by name" {
    run bash greet.sh Ada
    assert_success
    assert_output "Welcome to the dojo, Ada!"
}
