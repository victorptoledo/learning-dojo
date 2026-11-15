load /opt/dojo/facts

@test "multi-stage and at most 30 MB" {
    stages=$(grep -c -i -E '^[[:space:]]*FROM[[:space:]]' Dockerfile)
    [ "$stages" -ge 2 ] || fail "the Dockerfile has $stages FROM line(s)"
    size=$(image_size_mb)
    [ "$size" -le 30 ] || fail "the image is $size MB"
}

@test "has no shell and runs as non-root" {
    ! image_has_path /bin/sh || fail "/bin/sh is in the final image"
    user=$(image_user)
    case "${user%%:*}" in
        "" | root | 0) fail "the image runs as root (Config.User is '${user}')" ;;
    esac
}

@test "the health check runs the server's own probe in exec form" {
    run image_healthcheck
    assert_output "CMD /app/server -healthcheck"
    run image_jq '.Config.Healthcheck.Interval'
    assert_output "5000000000"
}

@test "carries the title and version labels" {
    run image_label org.opencontainers.image.title
    assert_output "greeter"
    run image_label org.opencontainers.image.version
    assert_output "1.0.0"
}

@test "keeps PORT, the port and the entrypoint" {
    run image_env PORT
    assert_output "8080"
    run image_ports
    assert_output "8080/tcp"
    run image_jq '.Config.Entrypoint // [] | tojson'
    assert_output '["/app/server"]'
}

@test "web is built from this folder" {
    run compose_jq '.services.web.build.context // "" | test("/kata$")'
    assert_output "true"
}

@test "GREETING_NAME defaults to compose" {
    run compose_jq '.services.web.environment.GREETING_NAME'
    assert_output "compose"
}

@test "web waits for a healthy cache and restarts unless stopped" {
    run compose_jq '.services.web.depends_on.cache.condition'
    assert_output "service_healthy"
    run compose_jq '.services.web.restart'
    assert_output "unless-stopped"
}

@test "cache runs a pinned redis image" {
    run compose_jq '.services.cache.image'
    assert_output "redis:8.2-alpine"
}

@test "cache is health-checked with redis-cli ping" {
    run compose_jq '.services.cache.healthcheck.test | join(" ")'
    assert_output --partial "redis-cli ping"
}

@test "cache keeps its data in the cache-data volume" {
    run compose_jq '.services.cache.volumes // [] | map(select(.type == "volume" and .source == "cache-data" and .target == "/data")) | length'
    assert_output "1"
    run compose_jq '.volumes | has("cache-data")'
    assert_output "true"
}
