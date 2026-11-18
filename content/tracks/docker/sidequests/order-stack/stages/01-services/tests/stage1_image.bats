load /opt/dojo/facts

@test "multi-stage, with no Go toolchain and no sources in the image" {
    stages=$(grep -c -i -E '^[[:space:]]*FROM[[:space:]]' Dockerfile)
    [ "$stages" -ge 2 ] || fail "the Dockerfile has $stages FROM line(s)"
    ! image_has_path /usr/local/go || fail "/usr/local/go is in the final image"
    found=$(image_files | grep -E '/(main\.go|go\.mod)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found source files: $found"
}

@test "go build keeps its compiler cache in a BuildKit cache mount" {
    grep -i -E '^[[:space:]]*RUN[[:space:]].*--mount=type=cache' Dockerfile | grep -q 'go-build' \
        || fail "no 'RUN --mount=type=cache,target=/root/.cache/go-build' in the Dockerfile"
}

@test "the binary is the ENTRYPOINT and api the default command" {
    run image_jq '.Config.Entrypoint // [] | join(" ")'
    assert_output --regexp '(^|/)orders$'
    run image_jq '.Config.Cmd // [] | join(" ")'
    assert_output "api"
}

@test "the same image plays the other roles" {
    run cmd_status plan
    assert_output "0"
    run cmd_out plan
    assert_line --index 0 "001_create_orders"
    run cmd_out version
    assert_output "orders 2.1.0"
}

@test "the api answers /health, and /ready waits for its database" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
    run http_status 8080 /ready
    assert_output "503"
}
