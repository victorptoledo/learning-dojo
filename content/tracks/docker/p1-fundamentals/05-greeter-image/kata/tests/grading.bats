load /opt/dojo/facts

@test "greets every name given to docker run" {
    run cmd_out two
    assert_output "hello, Ada!
hello, Grace!"
    run cmd_status two
    assert_output "0"
}

@test "greet.sh is the exec-form entrypoint" {
    run image_jq '.Config.Entrypoint // [] | tojson'
    assert_output '["/app/greet.sh"]'
}

@test "world is the default argument" {
    run image_jq '.Config.Cmd // [] | tojson'
    assert_output '["world"]'
}

@test "GREETING defaults to hello" {
    run image_env GREETING
    assert_output "hello"
}

@test "works in /app" {
    run image_jq '.Config.WorkingDir'
    assert_output "/app"
}

@test "secrets.env is not in the image" {
    if image_files | grep -q 'secrets\.env$'; then
        fail "secrets.env was copied into the image"
    fi
}

@test "notes and tests are not in the image" {
    ! image_has_path /app/notes || fail "/app/notes is in the image"
    ! image_has_path /app/tests || fail "/app/tests is in the image"
}

@test "a .dockerignore keeps them out of the build context" {
    [ -f .dockerignore ] || fail "there is no .dockerignore next to the Dockerfile"
    grep -q 'secrets\.env' .dockerignore || fail ".dockerignore does not list secrets.env"
}
