load /opt/dojo/facts

@test "the health check runs the binary's own probe in exec form" {
    run image_healthcheck
    assert_output --regexp '^CMD .*-healthcheck$'
}

@test "the container becomes healthy" {
    run container_health
    assert_output "healthy"
}

@test "the version is a build argument that defaults to 1.0.0" {
    grep -q -i -E '^[[:space:]]*ARG[[:space:]]+VERSION=1\.0\.0[[:space:]]*$' Dockerfile \
        || fail "no 'ARG VERSION=1.0.0' line in the Dockerfile"
}

@test "the binary reports the version it was built with" {
    run cmd_status version
    assert_output "0"
    run cmd_out version
    assert_output "shortener 1.0.0"
    run container_logs
    assert_output --partial '"version":"1.0.0"'
}

@test "carries the OCI title, version and source labels" {
    run image_label org.opencontainers.image.title
    assert_output "shortener"
    run image_label org.opencontainers.image.version
    assert_output "1.0.0"
    run image_label org.opencontainers.image.source
    assert_output --regexp '^https://'
}
