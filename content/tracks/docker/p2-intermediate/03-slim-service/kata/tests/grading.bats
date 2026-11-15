load /opt/dojo/facts

@test "uses at least two stages" {
    stages=$(grep -c -i -E '^[[:space:]]*FROM[[:space:]]' Dockerfile)
    [ "$stages" -ge 2 ] || fail "the Dockerfile has $stages FROM line(s)"
}

@test "ships no Go toolchain" {
    ! image_has_path /usr/local/go || fail "/usr/local/go is in the final image"
}

@test "ships no source code" {
    if image_files | grep -E -q '/(main\.go|go\.mod)$'; then
        fail "found source files: $(image_files | grep -E '/(main\.go|go\.mod)$' | head -n 3 | tr '\n' ' ')"
    fi
}

@test "runs as a non-root user" {
    user=$(image_user)
    case "${user%%:*}" in
        "" | root | 0) fail "the image runs as root (Config.User is '${user}')" ;;
    esac
}

@test "keeps the configuration and the port" {
    run image_env PORT
    assert_output "8080"
    run image_env GREETING_NAME
    assert_output "dojo"
    run image_ports
    assert_output "8080/tcp"
}

@test "starts with an exec-form command" {
    run image_jq '((.Config.Entrypoint // []) + (.Config.Cmd // [])) | join(" ")'
    refute_output --partial "/bin/sh -c"
    refute_output ""
}

@test "answers on /health" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}

@test "greets dojo on /hello" {
    run http_get 8080 /hello
    assert_success
    assert_output "hello, dojo"
}
