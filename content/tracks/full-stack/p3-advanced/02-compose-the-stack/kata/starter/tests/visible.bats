# This kata has no Dockerfile: compose.yaml is normalised with `docker compose config`
# and read with compose_jq.
load /opt/dojo/facts

@test "only web is published, on 127.0.0.1:8080" {
    run compose_jq '[.services | to_entries[] | select((.value.ports // []) | length > 0) | .key] | join(",")'
    assert_output "web"
    run compose_jq '.services.web.ports | map("\(.host_ip // "*"):\(.published):\(.target)") | join(",")'
    assert_output "127.0.0.1:8080:8080"
}
