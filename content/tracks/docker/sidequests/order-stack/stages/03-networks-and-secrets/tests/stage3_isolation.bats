load /opt/dojo/facts

# The networks of service $1, sorted and joined with commas.
networks_of() {
    compose_jq ".services[\"$1\"].networks // {} | keys | join(\",\")"
}

@test "backend is an internal network and edge is not" {
    run compose_jq '.networks.backend.internal // false'
    assert_output "true"
    run compose_jq '.networks.edge.internal // false'
    assert_output "false"
}

@test "only proxy and api are on edge; everything else only on backend" {
    run networks_of proxy
    assert_output "edge"
    run networks_of api
    assert_output "backend,edge"
    for service in worker migrate db cache; do
        run networks_of "$service"
        assert_output "backend"
    done
}

@test "only proxy publishes a port, and only on 127.0.0.1" {
    run compose_jq '.services.proxy.ports // [] | map("\(.host_ip // "*"):\(.published):\(.target)") | join(",")'
    assert_output "127.0.0.1:8080:8080"
    run compose_jq '[.services | to_entries[] | select(.key != "proxy") | select((.value.ports // []) | length > 0) | .key] | join(",")'
    assert_output ""
}

@test "the database password is the db_password secret, read from secrets/db_password.txt" {
    run compose_jq '.secrets.db_password.file // "" | test("secrets/db_password\\.txt$")'
    assert_output "true"
    run compose_jq '[.services.db, .services.api, .services.worker, .services.migrate] | map(.secrets // [] | map(.source) | join(",")) | join(" ")'
    assert_output "db_password db_password db_password db_password"
    run compose_jq '.services.db.environment.POSTGRES_PASSWORD_FILE // ""'
    assert_output "/run/secrets/db_password"
    run compose_jq '[.services.api, .services.worker, .services.migrate] | map(.environment.DATABASE_PASSWORD_FILE // "") | join(" ")'
    assert_output "/run/secrets/db_password /run/secrets/db_password /run/secrets/db_password"
}

@test "no password is written in the compose file" {
    run compose_jq '[.services[] | .environment // {} | keys[] | select(test("PASSWORD$"))] | join(",")'
    assert_output ""
    if grep -q -e 'orders-db-5c81f2e7' -e 'orders-dev' "$COMPOSE_JSON"; then
        fail "a password still appears in the compose file"
    fi
}

@test "api, worker and migrate share config/orders.env" {
    grep -q -E '^[[:space:]]*env_file:' compose.yaml || fail "compose.yaml has no env_file"
    run compose_jq '[.services.api, .services.worker, .services.migrate] | map(.environment.LOG_LEVEL // "unset") | join(" ")'
    assert_output "info info info"
}

@test "secrets and settings stay out of the build context and the image" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    grep -q -x -E '/?(\*\*/)?secrets/?' .dockerignore || fail ".dockerignore does not list secrets/"
    found=$(image_files | grep -E '(db_password\.txt|orders\.env)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found in the image: $found"
}
