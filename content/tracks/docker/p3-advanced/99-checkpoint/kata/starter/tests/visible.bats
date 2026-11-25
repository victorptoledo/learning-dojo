# The image is built (with no build secret, like a CI job that forgot it) and started once;
# compose.yaml is only normalised with `docker compose config`, then read with compose_jq.
load /opt/dojo/facts

@test "the container becomes healthy" {
    run container_health
    assert_output "healthy"
}

@test "-version prints the release version" {
    run cmd_out version
    assert_output "3.1.0"
}

@test "Compose tags the image with the release, never latest" {
    run compose_jq '.services.quotes.image'
    assert_output "ghcr.io/dojo-example/quotes:3.1.0"
}
