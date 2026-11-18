load /opt/dojo/facts

preflight() {
    cmd_out preflight | sed -n "s/^$1=//p"
}

@test "the server is PID 1, so it gets SIGTERM from docker stop" {
    run preflight pid
    assert_output "1"
}

@test "an exec-form health check runs the binary's own probe" {
    run image_healthcheck
    assert_output --regexp '^CMD .*-healthcheck$'
    run container_health
    assert_output "healthy"
}

@test "the version is a build argument stamped into the binary" {
    grep -q -i -E '^[[:space:]]*ARG[[:space:]]+VERSION=2\.3\.0[[:space:]]*$' Dockerfile \
        || fail "no 'ARG VERSION=2.3.0' line in the Dockerfile"
    run cmd_out version
    assert_output "inventory 2.3.0"
}

@test "logs are JSON lines on standard output" {
    first=$(container_logs | head -n 1)
    run jq -r '.msg + " " + .version' <<< "$first"
    assert_success
    assert_output "listening 2.3.0"
}

@test "the data folder is a volume, mounted in the running container" {
    run image_jq '.Config.Volumes // {} | has("/var/lib/inventory")'
    assert_output "true"
    run container_jq '.Mounts // [] | map(select(.Destination == "/var/lib/inventory")) | .[0].Type // "none"'
    assert_output "volume"
    run preflight data_writable
    assert_output "true"
}
