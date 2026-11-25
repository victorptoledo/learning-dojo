load /opt/dojo/facts

# The Dockerfile with comments dropped and continuation lines joined: one instruction per line.
instructions() {
    grep -v -E '^[[:space:]]*#' Dockerfile | sed -e ':a' -e '/\\[[:space:]]*$/N; s/\\[[:space:]]*\n/ /; ta'
}

# The RUN instruction that compiles the service.
build_step() { instructions | grep -E '^[[:space:]]*RUN[[:space:]].*go build' | head -n 1; }

# True when .dockerignore has the pattern on a line of its own (also as dir/, /x or **/x).
ignored() {
    [ -f .dockerignore ] || return 1
    grep -v '^[[:space:]]*#' .dockerignore | sed 's/[[:space:]]*$//' \
        | grep -qxF -e "$1" -e "$1/" -e "/$1" -e "**/$1"
}

@test "every base image is pinned by digest" {
    stages=" "
    count=0
    while read -r _ image rest; do
        name=$(sed -n -E 's/^.*[[:space:]][Aa][Ss][[:space:]]+([^[:space:]]+).*$/\1/p' <<< "$rest")
        case "$stages" in *" $image "*) image=stage ;; esac
        if [ "$image" != scratch ] && [ "$image" != stage ]; then
            [[ "$image" =~ @sha256:[0-9a-f]{64}$ ]] || fail "FROM $image is not pinned by digest"
            count=$((count + 1))
        fi
        if [ -n "$name" ]; then
            stages="$stages$name "
        fi
    done < <(instructions | grep -i -E '^[[:space:]]*FROM[[:space:]]' | sed -E 's/--platform=[^[:space:]]+[[:space:]]+//')
    [ "$count" -ge 1 ] || fail "no FROM line names an image"
}

@test "go.mod is copied before the code and the compiler cache is a cache mount" {
    if instructions | grep -q -i -E '^[[:space:]]*COPY[[:space:]]+(--[^[:space:]]+[[:space:]]+)*\.[[:space:]]+\.'; then
        fail "the Dockerfile still has COPY . ."
    fi
    mod=$(instructions | grep -n -E '^[[:space:]]*COPY[[:space:]].*go\.mod' | head -n 1 | cut -d: -f1)
    code=$(instructions | grep -n -E '^[[:space:]]*COPY[[:space:]].*\.go([[:space:]]|$)' | head -n 1 | cut -d: -f1)
    [ -n "$mod" ] || fail "go.mod is never copied on its own"
    [ -n "$code" ] || fail "the .go files are never copied"
    [ "$mod" -lt "$code" ] || fail "go.mod is copied after the code"
    step=$(build_step)
    [ -n "$step" ] || fail "no RUN step runs go build"
    grep -q -E -- '--mount=[^[:space:]]*type=cache' <<< "$step" || fail "go build has no cache mount"
    grep -q -E -- '--mount=[^[:space:]]*target=/root/\.cache/go-build' <<< "$step" \
        || fail "go build has no cache mount on /root/.cache/go-build"
}

@test "the Git credentials come from a build secret named netrc" {
    step=$(build_step)
    [ -n "$step" ] || fail "no RUN step runs go build"
    grep -q -E -- '--mount=[^[:space:]]*type=secret' <<< "$step" || fail "go build has no secret mount"
    grep -q -E -- '--mount=[^[:space:]]*id=netrc' <<< "$step" || fail "the secret mount is not the secret netrc"
    if instructions | grep -q -E '^[[:space:]]*(ARG|ENV)[[:space:]].*GIT_TOKEN'; then
        fail "GIT_TOKEN is still a build argument or a variable"
    fi
}

@test "no credential in the image history, configuration or files" {
    if image_history | grep -q -e 'GIT_TOKEN' -e 'dojo-fake-git' -e 'password'; then
        fail "a build step still carries the Git credentials"
    fi
    run image_jq '.Config.Env // [] | map(select(split("=")[0] | test("TOKEN|SECRET|PASSWORD"; "i"))) | join(",")'
    assert_output ""
    found=$(image_files | grep -E '(^|/)(\.netrc|\.env[^/]*|secrets/?|api_token[^/]*)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found in the image: $found"
    if image_has_prefix src/; then
        fail "the build folder /src is in the final image"
    fi
}

@test ".dockerignore keeps Git, env files and secrets out of the context" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    ignored .git || fail ".dockerignore does not list .git"
    ignored .env || ignored '.env*' || fail ".dockerignore does not list .env"
    ignored secrets || fail ".dockerignore does not list secrets/"
}

@test "the OCI labels describe the release, with the binary's own version" {
    run image_label org.opencontainers.image.title
    assert_output "quotes"
    run image_label org.opencontainers.image.version
    assert_output "3.1.0"
    assert_output "$(cmd_out version)"
    run image_label org.opencontainers.image.revision
    refute_output ""
    run image_label org.opencontainers.image.source
    assert_output --regexp '^https://'
}

@test "runs as the numeric user 10001, small, and serves quotes" {
    run image_user
    assert_output --regexp '^10001(:10001)?$'
    size=$(image_size_mb)
    [ "$size" -le 30 ] || fail "the image is $size MB"
    run container_logs
    assert_output --partial '"uid":10001'
    run http_get 8080 /quote
    assert_success
    refute_output ""
}

@test "the health check runs the server's own probe in exec form" {
    run image_healthcheck
    assert_output "CMD /app/quotes -healthcheck"
}

@test "Compose builds with VERSION and the netrc build secret" {
    run compose_jq '.services.quotes.build.args.VERSION'
    assert_output "3.1.0"
    run compose_jq '.services.quotes.build.secrets // [] | map(.source) | join(",")'
    assert_output "netrc"
    run compose_jq '.secrets.netrc.file // "" | test("/secrets/netrc$")'
    assert_output "true"
    run compose_jq '.services.quotes.build.args // {} | has("GIT_TOKEN")'
    assert_output "false"
}

@test "the API token is a runtime secret and no token is in the compose file" {
    run compose_jq '.services.quotes.secrets // [] | map(.source) | join(",")'
    assert_output "api_token"
    run compose_jq '.secrets.api_token.file // "" | test("/secrets/api_token\\.txt$")'
    assert_output "true"
    run compose_jq '.services.quotes.environment // {} | keys | map(select(test("TOKEN"))) | join(",")'
    assert_output ""
    if grep -q -e 'dojo-fake-git' -e 'dojo-fake-api' "$COMPOSE_JSON"; then
        fail "a token still appears in the normalised compose file"
    fi
}

@test "quotes runs as 10001 with a read-only file system and a tmpfs /tmp" {
    run compose_jq '.services.quotes.user'
    assert_output "10001:10001"
    run compose_jq '.services.quotes.read_only'
    assert_output "true"
    run compose_jq '.services.quotes.tmpfs // [] | join(",")'
    assert_output --regexp '(^|,)/tmp(:|,|$)'
}

@test "quotes drops every capability and new privileges, with limits" {
    run compose_jq '.services.quotes.cap_drop // [] | join(",")'
    assert_output "ALL"
    run compose_jq '.services.quotes.security_opt // [] | map(select(test("^no-new-privileges(:true)?$"))) | length'
    assert_output "1"
    run compose_jq '.services.quotes.deploy.resources.limits.memory'
    assert_output "134217728"
    run compose_jq '.services.quotes.deploy.resources.limits.cpus'
    assert_output "0.5"
    run compose_jq '.services.quotes.deploy.resources.limits.pids // .services.quotes.pids_limit'
    assert_output "100"
}

@test "quotes is published on 127.0.0.1 only" {
    run compose_jq '.services.quotes.ports // [] | map("\(.host_ip // "*"):\(.published):\(.target)") | join(",")'
    assert_output "127.0.0.1:8080:8080"
}
