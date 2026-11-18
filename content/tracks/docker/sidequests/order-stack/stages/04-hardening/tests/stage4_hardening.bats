load /opt/dojo/facts

@test "the image runs as 10001:10001, like the services that use it" {
    run image_user
    assert_output "10001:10001"
}

@test "api, worker and migrate run as 10001 on a read-only file system with a tmpfs /tmp" {
    for service in api worker migrate; do
        run compose_jq ".services.$service | \"\(.user) \(.read_only // false) \(.tmpfs // [] | map(select(test(\"^/tmp(:|$)\"))) | length)\""
        assert_output "10001:10001 true 1"
    done
}

@test "api, worker, migrate and proxy drop every capability" {
    for service in api worker migrate proxy; do
        run compose_jq ".services.$service.cap_drop // [] | join(\",\")"
        assert_output "ALL"
    done
}

@test "every service forbids new privileges" {
    run compose_jq '[.services | to_entries[] | select((.value.security_opt // []) | map(test("^no-new-privileges(:true)?$")) | any | not) | .key] | join(",")'
    assert_output ""
}

@test "proxy keeps a read-only file system too" {
    run compose_jq '.services.proxy | "\(.read_only // false) \(.tmpfs // [] | map(select(test("^/tmp(:|$)"))) | length)"'
    assert_output "true 1"
}

@test "every service has a memory limit, and api half a CPU and 256 MB" {
    run compose_jq '[.services | to_entries[] | select(.value.deploy.resources.limits.memory == null) | .key] | join(",")'
    assert_output ""
    run compose_jq '.services.api.deploy.resources.limits | "\(.cpus) \(.memory)"'
    assert_output "0.5 268435456"
}

@test "the long-running services rotate their logs" {
    for service in api worker db cache proxy; do
        run compose_jq ".services.$service.logging | \"\(.driver) \(.options[\"max-size\"]) \(.options[\"max-file\"])\""
        assert_output "json-file 10m 3"
    done
}

@test "two workers run side by side, so no service fixes its container name" {
    run compose_jq '.services.worker.deploy.replicas // 1'
    assert_output "2"
    run compose_jq '[.services[] | select(.container_name != null)] | length'
    assert_output "0"
}
