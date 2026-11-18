load /opt/dojo/facts

@test "uses at least two stages" {
    stages=$(grep -c -i -E '^[[:space:]]*FROM[[:space:]]' Dockerfile)
    [ "$stages" -ge 2 ] || fail "the Dockerfile has $stages FROM line(s)"
}

@test "ships no Go toolchain and no sources" {
    ! image_has_path /usr/local/go || fail "/usr/local/go is in the final image"
    found=$(image_files | grep -E '/(main\.go|go\.mod)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found source files: $found"
}

@test "the image is at most 30 MB" {
    size=$(image_size_mb)
    [ "$size" -le 30 ] || fail "the image is $size MB"
}

@test "keeps PORT and the port, and starts with an exec-form command" {
    run image_env PORT
    assert_output "8080"
    run image_ports
    assert_output "8080/tcp"
    run image_jq '((.Config.Entrypoint // []) + (.Config.Cmd // [])) | join(" ")'
    refute_output --partial "/bin/sh -c"
    assert_output --partial "shortener"
}

@test "answers ok on /health" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}

@test "shortens a link and redirects to it" {
    run curl --silent --show-error --fail --max-time 5 -X POST \
        --data-urlencode "url=https://example.com/docs/multi-stage" http://localhost:8080/links
    assert_success
    code=$output
    [ -n "$code" ] || fail "POST /links answered no code"
    run curl --silent --max-time 5 --output /dev/null \
        --write-out '%{http_code} %{redirect_url}' "http://localhost:8080/$code"
    assert_output "302 https://example.com/docs/multi-stage"
}
