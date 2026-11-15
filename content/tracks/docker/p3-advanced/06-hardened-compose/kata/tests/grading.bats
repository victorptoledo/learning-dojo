load /opt/dojo/facts

@test "every image is pinned to a version tag" {
    run compose_jq '[.services[].image | select(test(":[^/]+$") | not)] + [.services[].image | select(endswith(":latest"))] | join(" ")'
    assert_output ""
    run compose_jq '.services.api.image + " " + .services.db.image'
    assert_output "ghcr.io/dojo-example/orders-api:1.4.0 postgres:18-alpine"
}

@test "nothing mounts the Docker socket" {
    run compose_jq '[.services[].volumes // [] | .[] | select(.source // "" | test("docker\\.sock"))] | length'
    assert_output "0"
}

@test "api runs as 10001 with a read-only file system and a tmpfs /tmp" {
    run compose_jq '.services.api.user'
    assert_output "10001:10001"
    run compose_jq '.services.api.read_only'
    assert_output "true"
    run compose_jq '.services.api.tmpfs // [] | join(",")'
    assert_output --regexp '(^|,)/tmp(:|,|$)'
}

@test "api drops every capability and new privileges" {
    run compose_jq '.services.api.cap_drop // [] | join(",")'
    assert_output "ALL"
    run compose_jq '.services.api.security_opt // [] | map(select(test("^no-new-privileges(:true)?$"))) | length'
    assert_output "1"
}

@test "api has memory, CPU and process limits" {
    run compose_jq '.services.api.deploy.resources.limits.memory'
    assert_output "268435456"
    run compose_jq '.services.api.deploy.resources.limits.cpus'
    assert_output "0.5"
    run compose_jq '.services.api.deploy.resources.limits.pids // .services.api.pids_limit'
    assert_output "200"
}

@test "api is published on 127.0.0.1 only and db is not published" {
    run compose_jq '.services.api.ports // [] | map("\(.host_ip // "*"):\(.published):\(.target)") | join(",")'
    assert_output "127.0.0.1:8080:8080"
    run compose_jq '.services.db.ports // [] | length'
    assert_output "0"
}

@test "db lives on an internal backend network, api on frontend and backend" {
    run compose_jq '.networks.backend.internal'
    assert_output "true"
    run compose_jq '.services.db.networks | keys | join(",")'
    assert_output "backend"
    run compose_jq '.services.api.networks | keys | join(",")'
    assert_output "backend,frontend"
}

@test "the password is a secret file, never an environment value" {
    run compose_jq '.secrets.db_password.file // "" | test("secrets/db_password\\.txt$")'
    assert_output "true"
    run compose_jq '[.services.api, .services.db] | map(.secrets // [] | map(.source) | join(",")) | join(" ")'
    assert_output "db_password db_password"
    run compose_jq '.services.db.environment.POSTGRES_PASSWORD_FILE + " " + .services.api.environment.DATABASE_PASSWORD_FILE'
    assert_output "/run/secrets/db_password /run/secrets/db_password"
    if grep -q 'orders-pass-123' "$COMPOSE_JSON"; then
        fail "the password still appears in the compose file"
    fi
}

@test "api waits for a healthy db, and both are health-checked" {
    run compose_jq '.services.api.depends_on.db.condition'
    assert_output "service_healthy"
    run compose_jq '.services.db.healthcheck.test | join(" ")'
    assert_output --partial "pg_isready"
    run compose_jq '.services.api.healthcheck.test | length > 0'
    assert_output "true"
}

@test "both restart unless stopped and api logs rotate" {
    run compose_jq '.services.api.restart + " " + .services.db.restart'
    assert_output "unless-stopped unless-stopped"
    run compose_jq '.services.api.logging.driver + " " + .services.api.logging.options["max-size"] + " " + .services.api.logging.options["max-file"]'
    assert_output "json-file 10m 3"
}
