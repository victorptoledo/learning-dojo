# The image is built and started once, on its own (no Compose, a fresh /data volume);
# compose.yaml is only normalised with `docker compose config`, then read with compose_jq.
load /opt/dojo/facts

@test "the container keeps running and becomes healthy" {
    run container_status
    assert_output "running"
    run container_health
    assert_output "healthy"
}

@test "docker logs shows the server listening" {
    run container_logs
    assert_output --partial "listening on :8080"
}

@test "Compose runs two replicas of the catalog" {
    run compose_jq '.services.catalog.deploy.replicas'
    assert_output "2"
}
