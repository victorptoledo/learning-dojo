load /opt/dojo/facts

# True when .dockerignore has the pattern on a line of its own (also as dir/, /x or **/x).
ignored() {
    [ -f .dockerignore ] || return 1
    grep -v '^[[:space:]]*#' .dockerignore | sed 's/[[:space:]]*$//' \
        | grep -qxF -e "$1" -e "$1/" -e "/$1" -e "**/$1"
}

@test "every base image is pinned by digest" {
    stages=" "
    while read -r _ image rest; do
        name=$(sed -n -E 's/^.*[[:space:]][Aa][Ss][[:space:]]+([^[:space:]]+).*$/\1/p' <<< "$rest")
        case "$stages" in *" $image "*) image=stage ;; esac
        if [ "$image" != scratch ] && [ "$image" != stage ]; then
            [[ "$image" =~ @sha256:[0-9a-f]{64}$ ]] || fail "FROM $image is not pinned by digest"
        fi
        if [ -n "$name" ]; then
            stages="$stages$name "
        fi
    done < <(grep -i -E '^[[:space:]]*FROM[[:space:]]' Dockerfile)
}

@test "the OCI labels describe the release, with the binary's own version" {
    run image_label org.opencontainers.image.title
    assert_output "inventory"
    built=$(cmd_out version | awk '{print $2}')
    run image_label org.opencontainers.image.version
    assert_output "2.3.0"
    assert_output "$built"
    run image_label org.opencontainers.image.revision
    refute_output ""
    run image_label org.opencontainers.image.source
    assert_output --regexp '^https://'
    run image_label org.opencontainers.image.licenses
    refute_output ""
}

@test "no secret in the image configuration or its build steps" {
    run image_jq '.Config.Env // [] | map(select(split("=")[0] | test("TOKEN|SECRET|PASSWORD|PRIVATE"; "i"))) | join(",")'
    assert_output ""
    if image_history | grep -q -e 'REGISTRY_TOKEN' -e 'rgt_4f9c2a7e81d34b06'; then
        fail "the registry token appears in a build step"
    fi
}

@test "keys and env files stay out of the build context and the image" {
    [ -f .dockerignore ] || fail "there is no .dockerignore"
    ignored '.env*' || ignored .env.release || fail ".dockerignore does not list .env.release (or .env*)"
    ignored certs || ignored '*.key' || fail ".dockerignore does not list certs/ (or *.key)"
    found=$(image_files | grep -E '(\.key|\.pem|\.env\.release)$' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found in the image: $found"
}
