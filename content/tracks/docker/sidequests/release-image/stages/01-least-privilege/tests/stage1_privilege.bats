load /opt/dojo/facts

# The value of key $1 in the -preflight report.
preflight() {
    cmd_out preflight | sed -n "s/^$1=//p"
}

@test "the binary takes arguments: -preflight runs" {
    run cmd_status preflight
    assert_output "0"
    run preflight uid
    refute_output ""
}

@test "the image runs as 10001:10001" {
    run image_user
    assert_output "10001:10001"
    run preflight uid
    assert_output "10001"
    run preflight gid
    assert_output "10001"
}

@test "the process cannot rewrite its own binary" {
    run preflight exe_writable
    assert_output "false"
}

@test "the process can write its data directory, /var/lib/inventory" {
    run image_env DATA_DIR
    assert_output "/var/lib/inventory"
    run preflight data_writable
    assert_output "true"
}

@test "the server keeps running and answers /health" {
    run container_status
    assert_output "running"
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}

@test "a stock change is saved" {
    run curl --silent --show-error --max-time 5 -X PUT --data "qty=12" \
        --output /dev/null --write-out '%{http_code}' http://localhost:8080/items/BOLT-M6
    assert_output "200"
    run http_get 8080 /items
    assert_output --partial '{"sku":"BOLT-M6","qty":12}'
}
