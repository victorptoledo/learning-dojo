load /opt/dojo/facts

# True when .dockerignore has the pattern on a line of its own (also as dir/, /x or **/x).
ignored() {
    [ -f .dockerignore ] || return 1
    grep -v '^[[:space:]]*#' .dockerignore | sed 's/[[:space:]]*$//' \
        | grep -qxF -e "$1" -e "$1/" -e "/$1" -e "**/$1"
}

@test "a .dockerignore keeps notes, env files, .git and Markdown out of the context" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    ignored notes || fail ".dockerignore does not list notes/"
    ignored admin.env || ignored '*.env' || fail ".dockerignore does not list admin.env (or *.env)"
    ignored .git || fail ".dockerignore does not list .git"
    ignored '*.md' || fail ".dockerignore does not list *.md"
}

@test "admin.env and the notes are not in the image" {
    found=$(image_files | grep -E '(admin\.env|/notes/?|ideas\.md)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found in the image: $found"
    if image_history | grep -q 'board-admin-2026'; then
        fail "the admin password appears in a build step"
    fi
}

@test "go.mod is copied before the sources, so editing main.go reuses the module layer" {
    mod=$(grep -n -i -E '^[[:space:]]*COPY[[:space:]].*go\.mod' Dockerfile | head -n 1 | cut -d: -f1)
    src=$(grep -n -i -E '^[[:space:]]*COPY[[:space:]]' Dockerfile | grep -v 'go\.mod' \
        | grep -E '(\.go|[[:space:]]\.)[[:space:]]' | head -n 1 | cut -d: -f1)
    [ -n "$mod" ] || fail "no COPY line copies go.mod"
    [ -n "$src" ] || fail "no COPY line copies the .go files"
    [ "$mod" -lt "$src" ] || fail "go.mod (line $mod) is copied after the sources (line $src)"
}

@test "the binary is the exec-form ENTRYPOINT" {
    run image_jq '.Config.Entrypoint // [] | join(" ")'
    assert_output --regexp '(^|/)guestbook$'
}

@test "arguments given to docker run reach the binary" {
    run cmd_status version
    assert_output "0"
    run cmd_out version
    assert_output "guestbook 1.0.0"
}

@test "carries the OCI title and description labels" {
    run image_label org.opencontainers.image.title
    assert_output "guestbook"
    run image_label org.opencontainers.image.description
    refute_output ""
}
