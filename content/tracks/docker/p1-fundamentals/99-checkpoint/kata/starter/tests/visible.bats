# While your container runs, the tests share its network: localhost:8080 is your service.
load /opt/dojo/facts

@test "answers ok on /health" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}
