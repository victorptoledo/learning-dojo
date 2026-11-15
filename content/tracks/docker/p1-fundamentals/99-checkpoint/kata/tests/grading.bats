load /opt/dojo/facts

@test "greets dojo on /hello" {
    run http_get 8080 /hello
    assert_success
    assert_output "hello, dojo"
}

@test "the container keeps running and logs to stdout" {
    run container_status
    assert_output "running"
    run container_logs
    assert_output --partial "listening on :8080"
}

@test "builds with golang in /src" {
    image_has_path /usr/local/go || fail "no Go toolchain: build FROM golang:1.27-alpine"
    run image_jq '.Config.WorkingDir'
    assert_output "/src"
}

@test "copies go.mod before the sources" {
    mod=$(image_history | grep -n 'COPY .*go\.mod' | head -n 1 | cut -d: -f1)
    src=$(image_history | grep -n -E 'COPY .*(\*\.go|main\.go| \. )' | head -n 1 | cut -d: -f1)
    [ -n "$mod" ] || fail "no COPY step for go.mod"
    [ -n "$src" ] || fail "no COPY step for the .go files"
    [ "$mod" -lt "$src" ] || fail "go.mod is copied after the sources"
}

@test "installs the binary as /usr/local/bin/server" {
    image_has_path /usr/local/bin/server || fail "/usr/local/bin/server is not in the image"
}

@test "sets PORT and GREETING_NAME" {
    run image_env PORT
    assert_output "8080"
    run image_env GREETING_NAME
    assert_output "dojo"
}

@test "exposes 8080" {
    run image_ports
    assert_output "8080/tcp"
}

@test "starts the server with an exec-form CMD" {
    run image_jq '.Config.Cmd // [] | tojson'
    assert_output '["server"]'
}

@test "keeps README.md and tests out of the image" {
    ! image_has_path /src/README.md || fail "/src/README.md is in the image"
    ! image_has_path /src/tests || fail "/src/tests is in the image"
}
