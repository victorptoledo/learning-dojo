load /opt/dojo/facts

@test "server -version prints 1.4.0" {
    run cmd_out version
    assert_output "1.4.0"
}
