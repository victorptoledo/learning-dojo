load /opt/dojo/facts

TOKEN=dojo-fake-token-7f3a91

@test "the token appears in no build step" {
    if image_history | grep -q "$TOKEN"; then
        fail "docker history shows the token: $(image_history | grep "$TOKEN" | head -n 1)"
    fi
}

@test "no secret files in the image" {
    found=$(image_files | grep -E '(/\.env$|/secrets/|/api_token)' | grep -v '^/run/secrets/$' || true)
    [ -z "$found" ] || fail "secret files in the image: $(echo "$found" | head -n 3 | tr '\n' ' ')"
}

@test "no source folder in the final image" {
    ! image_has_path /src || fail "/src is in the final image"
}

@test ".dockerignore lists .env and secrets" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    grep -q -E '^/?\.env$' .dockerignore || fail ".dockerignore does not list .env"
    grep -q -E '^/?secrets/?$' .dockerignore || fail ".dockerignore does not list secrets/"
}

@test "runs as non-root and starts without a baked-in token" {
    user=$(image_user)
    case "${user%%:*}" in
        "" | root | 0) fail "the image runs as root (Config.User is '${user}')" ;;
    esac
    run container_status
    assert_output "running"
    run container_logs
    assert_output --partial '"status":"missing"'
}

@test "api gets the api_token secret" {
    run compose_jq '.services.api.secrets // [] | map(.source) | join(",")'
    assert_output "api_token"
}

@test "the secret comes from secrets/api_token.txt" {
    run compose_jq '.secrets.api_token.file // "" | test("secrets/api_token\\.txt$")'
    assert_output "true"
}

@test "the token appears nowhere in the compose file" {
    compose_jq '.' > /dev/null
    if grep -q "$TOKEN" "$COMPOSE_JSON"; then
        fail "docker compose config shows the token (environment or env_file)"
    fi
}

@test "api has no API_TOKEN environment variable" {
    run compose_jq '.services.api.environment // {} | has("API_TOKEN")'
    assert_output "false"
}
