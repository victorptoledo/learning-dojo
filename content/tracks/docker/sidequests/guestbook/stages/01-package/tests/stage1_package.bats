load /opt/dojo/facts

@test "builds from a golang alpine image with a version tag" {
    from=$(grep -i -m 1 -E '^[[:space:]]*FROM[[:space:]]' Dockerfile | awk '{print $2}')
    case "$from" in
        golang:*-alpine | golang:*-alpine[0-9]*) ;;
        *) fail "the first FROM is '$from', not golang:<version>-alpine" ;;
    esac
}

@test "installs the binary as /usr/local/bin/guestbook" {
    image_has_path /usr/local/bin/guestbook || fail "/usr/local/bin/guestbook is not in the image"
}

@test "sets PORT and BOARD_TITLE and exposes 8080" {
    run image_env PORT
    assert_output "8080"
    run image_env BOARD_TITLE
    assert_output "Dojo board"
    run image_ports
    assert_output "8080/tcp"
}

@test "starts the server with an exec-form command" {
    run image_jq '((.Config.Entrypoint // []) + (.Config.Cmd // [])) | join(" ")'
    refute_output --partial "/bin/sh -c"
    assert_output --partial "guestbook"
}

@test "the container keeps running and logs to stdout" {
    run container_status
    assert_output "running"
    run container_logs
    assert_output --partial "listening on :8080"
}

@test "answers ok on /health" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}

@test "shows the board title on /" {
    run http_get 8080 /
    assert_success
    assert_line --index 0 "Dojo board"
}
