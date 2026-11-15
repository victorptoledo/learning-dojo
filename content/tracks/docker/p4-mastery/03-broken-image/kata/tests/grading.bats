load /opt/dojo/facts

@test "the container becomes healthy" {
    run container_health
    assert_output "healthy"
}

@test "logs go to stdout, where docker logs can read them" {
    run container_logs
    assert_output --partial "listening on :8080"
}

@test "the server could write to its data directory" {
    run container_logs
    refute_output --partial "cannot write to the data directory"
    run image_env DATA_DIR
    assert_output "/data"
    run image_jq '.Config.Volumes // {} | has("/data")'
    assert_output "true"
}

@test "answers on /health" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}

@test "the server is PID 1: exec-form CMD" {
    run image_jq '.Config.Cmd // [] | tojson'
    assert_output '["server"]'
}

@test "the health check probes the port the server listens on" {
    run image_healthcheck
    refute_output --partial ":8000"
    refute_output ""
}

@test "still runs as the app user" {
    run image_user
    assert_output "app"
}
