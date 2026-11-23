bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    source "$BATS_TEST_DIRNAME/runner.sh"
    store="$repo/deploy"
}

# release VERSION [EPOCH]: builds VERSION (all targets) into store/releases/VERSION.
release() {
    local version=$1
    SDE=${2:-$SDE} build "$version" > /dev/null
    GIT_COMMIT=abc123 bash "$repo/scripts/manifest.sh" "$repo/dist" > /dev/null
    rm -rf "$store/releases/$version"
    mkdir -p "$store/releases"
    mv "$repo/dist" "$store/releases/$version"
}

promote() {
    (cd "$BATS_TEST_TMPDIR" && timeout 30 bash "$repo/scripts/promote.sh" "$@" 3>&-)
}

digest_of() {
    echo "sha256:$(sha "$store/releases/$1/manifest.json")"
}

channel() {
    jq -c '[.version, .digest, .previous]' "$store/channels/$1.json"
}

@test "staging gets a verified release, recorded by its digest" {
    release 1.2.0
    run --separate-stderr promote "$store" 1.2.0 staging
    assert_success
    assert_output "promote: staging now runs 1.2.0 ($(digest_of 1.2.0))"
    run channel staging
    assert_output "[\"1.2.0\",\"$(digest_of 1.2.0)\",null]"
    run cat "$store/channels/history.log"
    assert_output "staging 1.2.0 $(digest_of 1.2.0)"
}

@test "production takes only what staging runs" {
    release 1.2.0
    run --separate-stderr promote "$store" 1.2.0 production
    assert_failure 1
    assert_output "promote: 1.2.0 is not in staging"
    [ ! -e "$store/channels/production.json" ]
    promote "$store" 1.2.0 staging > /dev/null
    run --separate-stderr promote "$store" 1.2.0 production
    assert_success
    run channel production
    assert_output "[\"1.2.0\",\"$(digest_of 1.2.0)\",null]"
}

@test "a rebuild of the same version is not what staging tested" {
    release 1.2.0
    promote "$store" 1.2.0 staging > /dev/null
    # Rebuilt later from the same tag, but with another timestamp: other bytes.
    release 1.2.0 1700009999
    run --separate-stderr promote "$store" 1.2.0 production
    assert_failure 1
    assert_output "promote: 1.2.0 changed since it reached staging"
    [ ! -e "$store/channels/production.json" ]
}

@test "a release that does not verify goes nowhere" {
    release 1.2.0
    printf 'x' >> "$store/releases/1.2.0/greet-1.2.0-linux-amd64.tar.gz"
    run --separate-stderr promote "$store" 1.2.0 staging
    assert_failure 1
    assert_output --partial "verify: greet-1.2.0-linux-amd64.tar.gz: checksum mismatch"
    assert_line "promote: 1.2.0 does not verify"
    [ ! -e "$store/channels/staging.json" ]
    run --separate-stderr promote "$store" 9.9.9 staging
    assert_failure 1
    assert_output --partial "promote: no release 9.9.9"
}

@test "a pre-release reaches staging but never production" {
    release 1.3.0-rc.1
    run promote "$store" 1.3.0-rc.1 staging
    assert_success
    run --separate-stderr promote "$store" 1.3.0-rc.1 production
    assert_failure 1
    assert_output "promote: pre-releases never reach production"
}

@test "promoting what already runs changes nothing" {
    release 1.2.0
    promote "$store" 1.2.0 staging > /dev/null
    before=$(cat "$store/channels/staging.json" "$store/channels/history.log")
    run --separate-stderr promote "$store" 1.2.0 staging
    assert_success
    assert_output "promote: staging already runs 1.2.0"
    [ "$before" = "$(cat "$store/channels/staging.json" "$store/channels/history.log")" ]
}

@test "the previous version is kept, and versions compare by semver" {
    release 1.9.0
    release 1.10.0
    promote "$store" 1.9.0 staging > /dev/null
    run promote "$store" 1.10.0 staging
    assert_success
    run channel staging
    assert_output "[\"1.10.0\",\"$(digest_of 1.10.0)\",\"1.9.0\"]"
    run --separate-stderr promote "$store" 1.9.0 staging
    assert_failure 1
    assert_output "promote: staging runs 1.10.0, newer than 1.9.0 (use --allow-downgrade)"
    run channel staging
    assert_output --partial '"1.10.0"'
}

@test "a rollback needs --allow-downgrade" {
    release 1.9.0
    release 1.10.0
    promote "$store" 1.9.0 staging > /dev/null
    promote "$store" 1.10.0 staging > /dev/null
    run promote "$store" 1.9.0 staging --allow-downgrade
    assert_success
    run channel staging
    assert_output "[\"1.9.0\",\"$(digest_of 1.9.0)\",\"1.10.0\"]"
    run wc -l < "$store/channels/history.log"
    assert_output "3"
}

@test "pre-releases order before their release, numerically" {
    release 1.3.0-rc.2
    release 1.3.0-rc.10
    release 1.3.0
    promote "$store" 1.3.0-rc.2 staging > /dev/null
    run promote "$store" 1.3.0-rc.10 staging
    assert_success
    run promote "$store" 1.3.0 staging
    assert_success
    run --separate-stderr promote "$store" 1.3.0-rc.10 staging
    assert_failure 1
    assert_output --partial "newer than 1.3.0-rc.10"
}

@test "the channel is replaced whole, with nothing temporary left" {
    release 1.2.0
    release 1.2.1
    promote "$store" 1.2.0 staging > /dev/null
    promote "$store" 1.2.1 staging > /dev/null
    run ls -A "$store/channels"
    assert_output "history.log
staging.json"
}

@test "wrong arguments print the usage and exit 2" {
    release 1.2.0
    run --separate-stderr promote "$store" 1.2.0 qa
    assert_failure 2
    run --separate-stderr promote "$store" 1.2.0
    assert_failure 2
    run --separate-stderr promote "$store" 1.2.0 staging --force
    assert_failure 2
}

@test "promote is started by hand with a version and a choice of environment" {
    run wf promote.yml '.name'
    assert_output "promote"
    run wf promote.yml '.on | keys | join(",")'
    assert_output "workflow_dispatch"
    run wf promote.yml '.on.workflow_dispatch.inputs.version | "\(.required) \(.type)"'
    assert_output "true string"
    run wf promote.yml '.on.workflow_dispatch.inputs.environment | "\(.required) \(.type) \(.options | join(","))"'
    assert_output "true choice staging,production"
}

@test "one promotion per environment, waiting for its approval, allowed to push" {
    run wf promote.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    run wf promote.yml '.concurrency.group'
    assert_output --partial "inputs.environment"
    run wf promote.yml '.concurrency["cancel-in-progress"] | tostring'
    assert_output "false"
    run wf promote.yml '.jobs.promote.environment | if type == "object" then .name else . end | gsub("[[:space:]]"; "")'
    assert_output '${{inputs.environment}}'
    run wf promote.yml '.jobs.promote.permissions | tojson'
    assert_output '{"contents":"write"}'
    policy promote.yml pinned_actions
    policy promote.yml timeouts
    policy promote.yml no_untrusted_in_run
}

# serve VERSION: puts the release on the fake gh, as v VERSION.
serve() {
    release "$1"
    mkdir -p "$FAKE_RELEASES/v$1"
    mv "$store/releases/$1/"* "$FAKE_RELEASES/v$1/"
    rm -rf "$store/releases"
}

dispatch() {
    run_job promote.yml promote "inputs.version=$1" "inputs.environment=$2" \
        "github.token=fake-token" "secrets.GITHUB_TOKEN=fake-token"
}

@test "the workflow downloads, promotes and pushes the new channel" {
    serve 1.2.0
    run dispatch 1.2.0 staging
    assert_success
    run jq -r '.version' "$store/channels/staging.json"
    assert_output "1.2.0"
    run gh_calls "release download"
    run jq -r '.[2]' <<< "${lines[0]}"
    assert_output "v1.2.0"
    run jq -r 'join(" ")' "$GIT_LOG"
    assert_line "add deploy/channels"
    assert_line --regexp '^commit '
    assert_line "push"
}

@test "the workflow refuses production for a release staging never ran" {
    serve 1.2.0
    run dispatch 1.2.0 production
    assert_failure
    run grep -cE '^\["(commit|push)"' "$GIT_LOG"
    assert_output "0"
}

@test "the workflow does not commit when nothing changed" {
    serve 1.2.0
    dispatch 1.2.0 staging > /dev/null
    rm -rf "$store/releases"
    : > "$GIT_LOG"
    FAKE_GIT_DIFF_STATUS=0 run dispatch 1.2.0 staging
    assert_success
    run grep -cE '^\["(commit|push)"' "$GIT_LOG"
    assert_output "0"
}
