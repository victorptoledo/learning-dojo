# This kata has no Dockerfile: compose.yaml is normalised with `docker compose config`
# and read with compose_jq.
load /opt/dojo/facts

@test "no service is privileged" {
    run compose_jq '[.services[] | select(.privileged == true)] | length'
    assert_output "0"
}
