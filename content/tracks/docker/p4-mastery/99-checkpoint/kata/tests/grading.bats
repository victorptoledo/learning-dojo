load /opt/dojo/facts

# Seconds in a Compose duration such as 30s, 1m or 1m30s; empty when it cannot be read.
seconds() {
    local value=$1 total=0
    [[ "$value" =~ ^([0-9]+h)?([0-9]+m)?([0-9]+s)?$ ]] && [ -n "$value" ] || return 0
    [[ "$value" =~ ([0-9]+)h ]] && total=$((total + BASH_REMATCH[1] * 3600))
    [[ "$value" =~ ([0-9]+)m ]] && total=$((total + BASH_REMATCH[1] * 60))
    [[ "$value" =~ ([0-9]+)s ]] && total=$((total + BASH_REMATCH[1]))
    echo "$total"
}

@test "the server is PID 1 and runs as the non-root user 10001" {
    run http_get 8080 /debug/process
    assert_success
    pid=$(jq -r '.pid' <<< "$output")
    uid=$(jq -r '.uid' <<< "$output")
    [ "$pid" = 1 ] || fail "the server is PID $pid: something else (a shell?) is PID 1 and gets the SIGTERM"
    [ "$uid" = 10001 ] || fail "the server runs as uid $uid"
    run image_user
    refute_output ""
    refute_output "root"
    refute_output "0"
}

@test "every log line on stdout is JSON, and the schema is migrated before the server listens" {
    run container_logs
    refute_output ""
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        jq -e . > /dev/null 2>&1 <<< "$line" || fail "not a JSON log line: $line"
    done < <(container_logs)
    run bash -c "jq -r '.msg' < '$FACTS/logs.txt' | grep -n -E '^(schema migrated|schema up to date|listening on :8080)' | cut -d: -f2- | head -n 2 | paste -sd '|'"
    assert_output --regexp '^schema (migrated|up to date)\|listening on :8080$'
    run image_env LOG_FORMAT
    assert_output "json"
}

@test "the catalog can write its schema to the /data volume" {
    run container_logs
    refute_output --partial "cannot migrate"
    refute_output --partial "not migrated"
    run image_env DATA_DIR
    assert_output "/data"
    run image_jq '.Config.Volumes // {} | has("/data")'
    assert_output "true"
}

@test "exec-form ENTRYPOINT script and CMD serve, so arguments reach the catalog" {
    run image_jq '.Config.Entrypoint // [] | length'
    assert_output "1"
    run image_jq '.Config.Entrypoint[0] | test("docker-entrypoint\\.sh$")'
    assert_output "true"
    run image_jq '.Config.Cmd // [] | tojson'
    assert_output '["serve"]'
    run cmd_out version
    assert_output "2.0.0"
}

@test "the health check runs catalog healthcheck in exec form, with a start period" {
    run image_jq '.Config.Healthcheck.Test[0] // ""'
    assert_output "CMD"
    run image_healthcheck
    assert_output --regexp '^CMD [^ ]*catalog healthcheck$'
    run image_jq '(.Config.Healthcheck.StartPeriod // 0) > 0'
    assert_output "true"
    run image_jq '(.Config.Healthcheck.Interval // 30000000000) <= 10000000000'
    assert_output "true"
}

@test "serves the catalog and stays small" {
    run http_get 8080 /items
    assert_success
    assert_output --partial '"sku":"KB-01"'
    size=$(image_size_mb)
    [ "$size" -le 30 ] || fail "the image is $size MB"
}

@test "both services use the release image, never latest" {
    run compose_jq '.services.catalog.image + " " + .services.migrate.image'
    assert_output "ghcr.io/dojo-example/catalog:2.0.0 ghcr.io/dojo-example/catalog:2.0.0"
}

@test "a one-shot migrate service runs first, and the replicas do not migrate" {
    run compose_jq '.services.migrate.command // [] | join(" ")'
    assert_output "migrate"
    run compose_jq '.services.migrate.restart // "no"'
    assert_output "no"
    run compose_jq '.services.catalog.depends_on.migrate.condition'
    assert_output "service_completed_successfully"
    run compose_jq '.services.catalog.environment.MIGRATE_ON_START'
    assert_output "false"
}

@test "rolling updates start the new task first and roll back on failure" {
    run compose_jq '.services.catalog.deploy.update_config | "\(.parallelism) \(.order) \(.failure_action)"'
    assert_output "1 start-first rollback"
}

@test "no fixed host port, so two replicas can run on one host" {
    run compose_jq '.services.catalog.ports // [] | map(select((.published // "") != "")) | length'
    assert_output "0"
}

@test "30 seconds to drain on stop, restarts unless stopped" {
    grace=$(seconds "$(compose_jq '.services.catalog.stop_grace_period // ""')")
    [ -n "$grace" ] || fail "catalog has no stop_grace_period"
    [ "$grace" -ge 30 ] || fail "stop_grace_period is only $grace s, and the drain takes up to 25 s"
    run compose_jq '.services.catalog.restart'
    assert_output "unless-stopped"
}

@test "logs rotate: json-file, 10m, 5 files" {
    run compose_jq '.services.catalog.logging | "\(.driver) \(.options["max-size"]) \(.options["max-file"])"'
    assert_output "json-file 10m 5"
}

@test "both services share the named volume catalog-data at /data" {
    run compose_jq '[.services.migrate, .services.catalog] | map(.volumes // [] | map(select(.type == "volume" and .source == "catalog-data" and .target == "/data")) | length) | join(" ")'
    assert_output "1 1"
    run compose_jq '.volumes | has("catalog-data")'
    assert_output "true"
}
