load /opt/dojo/facts

@test "the stack has the five services" {
    run compose_jq '["api", "cache", "db", "proxy", "worker"] - (.services | keys) | join(",")'
    assert_output ""
}

@test "every image has a version tag" {
    run compose_jq '[.services[] | .image // "" | select(test(":[^/]+$") | not)] | length'
    assert_output "0"
    run compose_jq '[.services[] | .image // "" | select(endswith(":latest"))] | length'
    assert_output "0"
}

@test "api is built from this folder and tagged orders:2.1.0" {
    run compose_jq '.services.api.build.context // ""'
    refute_output ""
    run compose_jq '.services.api.build.dockerfile // "Dockerfile"'
    assert_output "Dockerfile"
    run compose_jq '.services.api.image'
    assert_output "orders:2.1.0"
}

@test "worker runs the same image with the worker command" {
    run compose_jq '.services.worker.image'
    assert_output "orders:2.1.0"
    run compose_jq '.services.worker.command // [] | join(" ")'
    assert_output "worker"
    run compose_jq '.services.worker.ports // [] | length'
    assert_output "0"
}

@test "api and worker find db and cache by service name" {
    run compose_jq '[.services.api, .services.worker] | map(.environment.DATABASE_HOST + " " + .environment.REDIS_ADDR) | join(",")'
    assert_output "db cache:6379,db cache:6379"
}

@test "db is postgres:18-alpine with the orders database on the db-data volume" {
    run compose_jq '.services.db.image'
    assert_output "postgres:18-alpine"
    run compose_jq '.services.db.environment.POSTGRES_DB + " " + .services.db.environment.POSTGRES_USER'
    assert_output "orders orders"
    run compose_jq '.services.db.volumes // [] | map(select(.type == "volume" and .target == "/var/lib/postgresql")) | .[0].source // ""'
    assert_output "db-data"
}

@test "cache is redis with append-only persistence on the cache-data volume" {
    run compose_jq '.services.cache.image'
    assert_output --regexp '^redis:[0-9]'
    run compose_jq '.services.cache.command // [] | join(" ")'
    assert_output --partial "--appendonly yes"
    run compose_jq '.services.cache.volumes // [] | map(select(.type == "volume" and .target == "/data")) | .[0].source // ""'
    assert_output "cache-data"
}

@test "proxy mounts nginx.conf read-only and publishes 8080" {
    run compose_jq '.services.proxy.image'
    assert_output "nginxinc/nginx-unprivileged:1.29-alpine"
    run compose_jq '.services.proxy.volumes // [] | map(select(.target == "/etc/nginx/conf.d/default.conf")) | .[0] | "\(.type) \(.source | test("proxy/nginx\\.conf$")) \(.read_only // false)"'
    assert_output "bind true true"
    run compose_jq '.services.proxy.ports // [] | map("\(.published):\(.target)") | join(",")'
    assert_output "8080:8080"
}
