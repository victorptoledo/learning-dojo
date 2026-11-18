load /opt/dojo/facts

# True when .dockerignore has the pattern on a line of its own (also as dir/, /x or **/x).
ignored() {
    [ -f .dockerignore ] || return 1
    grep -v '^[[:space:]]*#' .dockerignore | sed 's/[[:space:]]*$//' \
        | grep -qxF -e "$1" -e "$1/" -e "/$1" -e "**/$1"
}

@test "runs as a numeric non-root user" {
    user=$(image_user)
    [[ "$user" =~ ^[0-9]+(:[0-9]+)?$ ]] \
        || fail "Config.User is '$user'; use a numeric UID (and GID), so a cluster can check it is not root"
    [ "${user%%:*}" -ne 0 ] || fail "UID 0 is root"
}

@test "the container really runs as that user" {
    run container_jq '.Config.User'
    refute_output ""
    refute_output "root"
    run container_status
    assert_output "running"
}

@test "a .dockerignore keeps deploy, docs, .git and Markdown out of the context" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    ignored deploy || ignored '*.env' || fail ".dockerignore does not list deploy/ (or *.env)"
    ignored docs || ignored '*.md' || fail ".dockerignore does not list docs/ (or *.md)"
    ignored .git || fail ".dockerignore does not list .git"
    ignored '*.md' || fail ".dockerignore does not list *.md"
}

@test "the build stage copies go.mod before the sources" {
    mod=$(grep -n -i -E '^[[:space:]]*COPY[[:space:]].*go\.mod' Dockerfile | head -n 1 | cut -d: -f1)
    src=$(grep -n -i -E '^[[:space:]]*COPY[[:space:]]' Dockerfile | grep -v -E 'go\.mod|--from' \
        | head -n 1 | cut -d: -f1)
    [ -n "$mod" ] || fail "no COPY line copies go.mod"
    [ -n "$src" ] || fail "no COPY line copies the sources"
    [ "$mod" -lt "$src" ] || fail "go.mod (line $mod) is copied after the sources (line $src)"
}

@test "the production token is in no layer of the image" {
    if image_history | grep -q 'prod-admin-7f3e9a1c'; then
        fail "the token appears in a build step"
    fi
    found=$(image_files | grep -E '(production\.env|design\.md)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found in the image: $found"
}
