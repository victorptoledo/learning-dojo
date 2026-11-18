load /opt/dojo/facts

# The condition service $1 waits on $2 with, or "none".
waits_on() {
    compose_jq ".services[\"$1\"].depends_on[\"$2\"].condition // \"none\""
}

# The health check of service $1 as one line.
check_of() {
    compose_jq ".services[\"$1\"].healthcheck.test // [] | join(\" \")"
}

@test "the image has an exec-form health check that probes the api" {
    run image_healthcheck
    assert_output --regexp '^CMD .*orders healthcheck api$'
}

@test "the api container becomes healthy" {
    run container_health
    assert_output "healthy"
}

@test "db and cache are health-checked with their own tools" {
    run check_of db
    assert_output --partial "pg_isready"
    run check_of cache
    assert_output --regexp '^CMD(-SHELL)? redis-cli ping$'
}

@test "api and worker are health-checked by the orders binary, proxy on /healthz" {
    run check_of api
    assert_output --regexp '^CMD .*orders healthcheck api$'
    run check_of worker
    assert_output --regexp '^CMD .*orders healthcheck worker$'
    run check_of proxy
    assert_output --partial "/healthz"
}

@test "migrate runs the migrations once, after db is healthy" {
    run compose_jq '.services.migrate.image + " " + (.services.migrate.command // [] | join(" "))'
    assert_output "orders:2.1.0 migrate"
    run waits_on migrate db
    assert_output "service_healthy"
    run compose_jq '.services.migrate.restart // "no"'
    assert_output "no"
}

@test "api starts after healthy db and cache and finished migrations" {
    run waits_on api db
    assert_output "service_healthy"
    run waits_on api cache
    assert_output "service_healthy"
    run waits_on api migrate
    assert_output "service_completed_successfully"
}

@test "worker waits for the queue and the migrations, proxy for a healthy api" {
    run waits_on worker cache
    assert_output "service_healthy"
    run waits_on worker migrate
    assert_output "service_completed_successfully"
    run waits_on proxy api
    assert_output "service_healthy"
}

@test "the long-running services restart unless stopped" {
    run compose_jq '[.services.api, .services.worker, .services.db, .services.cache, .services.proxy] | map(.restart // "none") | join(" ")'
    assert_output "unless-stopped unless-stopped unless-stopped unless-stopped unless-stopped"
}
