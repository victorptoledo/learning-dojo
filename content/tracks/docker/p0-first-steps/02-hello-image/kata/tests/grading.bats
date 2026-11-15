load /opt/dojo/facts

@test "the container exits with status 0" {
    run cmd_status default
    assert_output "0"
}

@test "starts from alpine" {
    image_has_path /etc/alpine-release || fail "no /etc/alpine-release: start FROM alpine:3.24"
}

@test "keeps the file at /srv/hello.txt" {
    image_has_path /srv/hello.txt || fail "/srv/hello.txt is not in the image"
}

@test "works in /srv" {
    run image_jq '.Config.WorkingDir'
    assert_output "/srv"
}

@test "carries the title label and uses an exec-form CMD" {
    run image_label org.opencontainers.image.title
    assert_output "hello-dojo"
    run image_jq '.Config.Cmd | join(" ")'
    refute_output --partial "/bin/sh -c"
}
