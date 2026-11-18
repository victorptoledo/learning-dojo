load /opt/dojo/facts

# True when .dockerignore has the pattern on a line of its own (also as dir/, /x or **/x).
ignored() {
    [ -f .dockerignore ] || return 1
    grep -v '^[[:space:]]*#' .dockerignore | sed 's/[[:space:]]*$//' \
        | grep -qxF -e "$1" -e "$1/" -e "/$1" -e "**/$1"
}

# Line number of the first COPY that matches $1.
copy_line() {
    grep -n -i -E '^[[:space:]]*COPY[[:space:]]' Dockerfile | grep -v -e '--from' | grep -E "$1" \
        | head -n 1 | cut -d: -f1
}

@test "a .dockerignore keeps .git, docs, scripts, testdata and Markdown out of the context" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    for pattern in .git docs scripts testdata '*.md'; do
        ignored "$pattern" || fail ".dockerignore does not list $pattern"
    done
}

@test "the build copies what it compiles, never the whole folder" {
    if grep -i -E '^[[:space:]]*(COPY|ADD)[[:space:]]+(--[^[:space:]]+[[:space:]]+)*\.[[:space:]]' Dockerfile; then
        fail "a COPY copies the whole build context"
    fi
}

@test "go.mod is copied before cmd/ and internal/" {
    mod=$(copy_line 'go\.mod')
    cmd=$(copy_line 'cmd')
    internal=$(copy_line 'internal')
    [ -n "$mod" ] && [ -n "$cmd" ] && [ -n "$internal" ] \
        || fail "copy go.mod, cmd/ and internal/ with COPY lines of their own"
    [ "$mod" -lt "$cmd" ] && [ "$mod" -lt "$internal" ] \
        || fail "go.mod (line $mod) must come before cmd/ (line $cmd) and internal/ (line $internal)"
}

@test "go build keeps its compiler cache in a cache mount" {
    grep -i -E '^[[:space:]]*RUN[[:space:]].*--mount=type=cache' Dockerfile | grep -q 'go-build' \
        || fail "no 'RUN --mount=type=cache,target=/root/.cache/go-build' in the Dockerfile"
}

@test "no docs, scripts or test data in the image" {
    found=$(image_files | grep -E '(runbook\.md|load-test\.sh|testdata/?)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found in the image: $found"
}
