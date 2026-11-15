setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "joins every argument into the name" {
    run bash greet.sh Ada Lovelace
    assert_success
    assert_output "Welcome to the dojo, Ada Lovelace!"
}

@test "greets a stranger without arguments" {
    run bash greet.sh
    assert_success
    assert_output "Welcome to the dojo, stranger!"
}

@test "an empty name counts as no name" {
    run bash greet.sh ""
    assert_success
    assert_output "Welcome to the dojo, stranger!"
}

@test "shouts with --shout" {
    run bash greet.sh --shout Ada
    assert_success
    assert_output "WELCOME TO THE DOJO, ADA!"
}

@test "shouts at a stranger" {
    run bash greet.sh --shout
    assert_success
    assert_output "WELCOME TO THE DOJO, STRANGER!"
}
