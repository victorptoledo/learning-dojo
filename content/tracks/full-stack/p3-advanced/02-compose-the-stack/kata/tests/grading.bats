load /opt/dojo/facts

# The file as `docker compose --profile dev config` sees it: profiled services included.
dev_jq() {
    docker-compose --file compose.yaml --profile dev config --format json 2> /dev/null | jq -r "$1"
}

@test "every service runs a pinned release and the app images share one version" {
    run compose_jq '[.services[] | .image] | map(select(endswith(":latest") or (test(":[^/]+$") | not))) | length'
    assert_output "0"
    run dev_jq '[.services.web.image, .services.api.image, .services.migrate.image, .services.seed.image, .services.db.image] | join(" ")'
    assert_output "ghcr.io/dojo-example/shop-web:2.3.0 ghcr.io/dojo-example/shop-api:2.3.0 ghcr.io/dojo-example/shop-api:2.3.0 ghcr.io/dojo-example/shop-api:2.3.0 postgres:18-alpine"
}

@test "web is on frontend only, db on an internal backend only" {
    run compose_jq '.networks.backend.internal'
    assert_output "true"
    run compose_jq '.services.web.networks | keys | join(",")'
    assert_output "frontend"
    run compose_jq '.services.api.networks | keys | join(",")'
    assert_output "backend,frontend"
    run dev_jq '[.services.db, .services.migrate, .services.seed] | map(.networks | keys | join(",")) | join(" ")'
    assert_output "backend backend backend"
}

@test "web reaches the api by its service name" {
    run compose_jq '.services.web.environment.API_URL'
    assert_output "http://api:3000"
}

@test "the api is configured for one origin in production" {
    run compose_jq '.services.api.environment.CORS_ORIGIN + " " + .services.api.environment.NODE_ENV'
    assert_output "http://localhost:8080 production"
}

@test "api, migrate and seed get the same database settings" {
    for service in api migrate seed; do
        run dev_jq ".services.$service.environment | [.DATABASE_HOST, .DATABASE_NAME, .DATABASE_USER, .DATABASE_PASSWORD_FILE] | join(\" \")"
        assert_output "db shop shop /run/secrets/db_password"
        run dev_jq ".services.$service.environment.DATABASE_URL // \"none\""
        assert_output "none"
    done
}

@test "the password is a secret file used by every service that needs it" {
    run compose_jq '.secrets.db_password.file // "" | test("secrets/db_password\\.txt$")'
    assert_output "true"
    run dev_jq '[.services.db, .services.api, .services.migrate, .services.seed] | map(.secrets // [] | map(.source) | join(",")) | join(" ")'
    assert_output "db_password db_password db_password db_password"
    run compose_jq '.services.db.environment.POSTGRES_PASSWORD_FILE + " " + (.services.db.environment.POSTGRES_PASSWORD // "unset")'
    assert_output "/run/secrets/db_password unset"
    if grep -q 'shop-pass-2026' "$COMPOSE_JSON"; then
        fail "the password still appears in the compose file"
    fi
}

@test "migrations run once, in their own service, and the api only serves" {
    run compose_jq '.services.migrate.command | join(" ")'
    assert_output "node migrate.js"
    run compose_jq '.services.migrate.restart // "no"'
    assert_output "no"
    run compose_jq '.services.api.command | join(" ")'
    assert_output "node server.js"
}

@test "the start order follows health and completed migrations" {
    run compose_jq '.services.db.healthcheck.test | join(" ")'
    assert_output --partial "pg_isready"
    run compose_jq '.services.migrate.depends_on.db.condition'
    assert_output "service_healthy"
    run compose_jq '.services.api.depends_on | "\(.db.condition) \(.migrate.condition)"'
    assert_output "service_healthy service_completed_successfully"
    run compose_jq '.services.api.healthcheck.test | join(" ")'
    assert_output "CMD node healthcheck.js"
    run compose_jq '.services.web.depends_on.api.condition'
    assert_output "service_healthy"
}

@test "the database keeps its data in the db-data volume" {
    run compose_jq '.services.db.volumes | map(select(.type == "volume")) | map("\(.source):\(.target)") | join(",")'
    assert_output "db-data:/var/lib/postgresql"
    run compose_jq '.volumes | has("db-data")'
    assert_output "true"
}

@test "demo data only with the dev profile, after the migrations" {
    run compose_jq '.services | has("seed")'
    assert_output "false"
    run dev_jq '.services.seed.profiles // [] | join(",")'
    assert_output "dev"
    run dev_jq '.services.seed.command | join(" ")'
    assert_output "node seed.js"
    run dev_jq '.services.seed.depends_on.migrate.condition'
    assert_output "service_completed_successfully"
    run compose_jq '[.services.web, .services.api, .services.db] | map(.profiles // [] | length) | add'
    assert_output "0"
    run compose_jq '[.services.web.restart, .services.api.restart, .services.db.restart] | join(" ")'
    assert_output "unless-stopped unless-stopped unless-stopped"
}
