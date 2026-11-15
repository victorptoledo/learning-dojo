load /opt/dojo/facts

# The build stage: from its FROM line to the next FROM.
build_stage() {
    awk 'toupper($1) == "FROM" { n++ } n == 1' Dockerfile
}

@test "copies go.mod before the sources" {
    mod=$(build_stage | grep -n -i -E '^[[:space:]]*COPY .*go\.mod' | head -n 1 | cut -d: -f1)
    src=$(build_stage | grep -n -i -E '^[[:space:]]*COPY .*(\*\.go|main\.go)' | head -n 1 | cut -d: -f1)
    [ -n "$mod" ] || fail "no COPY of go.mod in the build stage"
    [ -n "$src" ] || fail "no COPY of the .go files in the build stage"
    [ "$mod" -lt "$src" ] || fail "go.mod is copied after the sources"
}

@test "no COPY . . in the build stage" {
    if build_stage | grep -q -i -E '^[[:space:]]*COPY[[:space:]]+(--[^ ]+[[:space:]]+)*\.[[:space:]]'; then
        fail "the build stage copies the whole context"
    fi
}

@test "go build uses a cache mount for the compiler cache" {
    tr -d '\\\n' < Dockerfile | grep -q -E 'RUN[[:space:]]+--mount=type=cache,(id=[^,]+,)?target=/root/\.cache/go-build.*go build' \
        || fail "no RUN --mount=type=cache,target=/root/.cache/go-build ... go build"
}

@test ".dockerignore keeps docs, tests, .git and Markdown out" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    for entry in .git docs tests '*.md'; do
        grep -q -x -F -e "$entry" -e "$entry/" -e "/$entry" -e "**/$entry" .dockerignore \
            || fail ".dockerignore does not list $entry"
    done
}

@test "the image has the version label" {
    run image_label org.opencontainers.image.version
    assert_output "1.4.0"
}

@test "-version exits with status 0" {
    run cmd_status version
    assert_output "0"
}

@test "small and non-root" {
    size=$(image_size_mb)
    [ "$size" -le 30 ] || fail "the image is $size MB"
    user=$(image_user)
    case "${user%%:*}" in
        "" | root | 0) fail "the image runs as root (Config.User is '${user}')" ;;
    esac
}

@test "answers on /health" {
    run http_get 8080 /health
    assert_success
    assert_output "ok"
}
