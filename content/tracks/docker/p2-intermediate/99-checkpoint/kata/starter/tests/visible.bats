# The image is built and started once; compose.yaml is only normalised with
# `docker compose config`, then read with compose_jq.
load /opt/dojo/facts

@test "the container becomes healthy" {
    run container_health
    assert_output "healthy"
}

@test "web publishes 8080" {
    run compose_jq '.services.web.ports // [] | map("\(.published):\(.target)") | join(",")'
    assert_output "8080:8080"
}
