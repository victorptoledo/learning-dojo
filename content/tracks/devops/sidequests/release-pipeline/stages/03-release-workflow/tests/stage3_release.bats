bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    source "$BATS_TEST_DIRNAME/runner.sh"
}

targets=(linux-amd64 linux-arm64 darwin-arm64)

# build_target TAG TARGET: one matrix job of build, then its upload.
build_target() {
    local tag=$1 target=$2
    rm -rf "$repo/dist"
    run_job release.yml build "github.ref_name=$tag" "matrix.target=$target" || return 1
    upload release.yml build "matrix.target=$target"
}

# publish TAG: the publish job, after building every target with the same tag.
build_all() {
    local target
    for target in "${targets[@]}"; do
        build_target "$1" "$target" > /dev/null || { echo "build of $target failed"; return 1; }
    done
    rm -rf "$repo/dist"
}

publish() {
    run_job release.yml publish "github.ref_name=$1" "github.token=fake-token" \
        "secrets.GITHUB_TOKEN=fake-token" "github.sha=4f2a9c1e0b7d4f2a9c1e0b7d4f2a9c1e0b7d4f2a"
}

@test "only tags like v1.2.3 start a release" {
    run wf release.yml '.name'
    assert_output "release"
    run wf release.yml '.on | keys | join(",")'
    assert_output "push"
    run wf release.yml '.on.push | keys | join(",")'
    assert_output "tags"
    run wf release.yml '.on.push.tags | join(",")'
    assert_output "v*.*.*"
}

@test "the token reads by default, actions are pinned, every job has a timeout" {
    run wf release.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    policy release.yml explicit_permissions
    policy release.yml pinned_actions
    policy release.yml timeouts
    policy release.yml no_untrusted_in_run
    run wf release.yml '.defaults.run.shell'
    assert_output "bash"
}

@test "a release in progress is never cancelled" {
    run wf release.yml '.concurrency.group'
    assert_output --partial "github.ref"
    run wf release.yml '.concurrency["cancel-in-progress"] | tostring'
    assert_output "false"
}

@test "build is a matrix over the three targets and cannot write" {
    run wf release.yml '.jobs.build.strategy.matrix.target | join(",")'
    assert_output "linux-amd64,linux-arm64,darwin-arm64"
    run wf release.yml '.jobs.build.permissions // {} | [.[] | select(. == "write")] | length'
    assert_output "0"
    run step_index release.yml build '^actions/checkout@'
    assert_output "0"
}

@test "the tag and the target reach the build step through env" {
    run wf release.yml '[.jobs.build.steps[] | select((.run // "") | test("scripts/build\\.sh"))] | first | .env // {} | map_values(gsub("[[:space:]]"; "")) | [.[]] | sort | join(",")'
    assert_output '${{github.ref_name}},${{matrix.target}}'
}

@test "a build job builds its target with the commit time" {
    run build_target v1.2.0 linux-arm64
    assert_success
    run ls "$repo/dist"
    assert_output "greet-1.2.0-linux-arm64.tar.gz"
    run listing "$repo/dist/greet-1.2.0-linux-arm64.tar.gz"
    assert_line --index 0 "drwxr-xr-x 0/0 2023-11-14T22:15:23 greet-1.2.0/"
}

@test "each build job uploads dist as dist-TARGET, and fails when it is empty" {
    run wf release.yml '[.jobs.build.steps[] | select((.uses // "") | startswith("actions/upload-artifact@")) | .with] | first // {} | "\(.name | gsub("[[:space:]]"; "")) \(.path | sub("/$"; "")) \(.["if-no-files-found"])"'
    assert_output 'dist-${{matrix.target}} dist error'
    build_target v1.2.0 darwin-arm64 > /dev/null
    run ls "$FAKE_ARTIFACTS/dist-darwin-arm64"
    assert_output "greet-1.2.0-darwin-arm64.tar.gz"
}

@test "publish needs build, runs in the release environment and alone may write" {
    run wf release.yml '[.jobs.publish.needs] | flatten | join(",")'
    assert_output "build"
    run wf release.yml '.jobs.publish.environment | if type == "object" then .name else . end'
    assert_output "release"
    run wf release.yml '.jobs.publish.permissions | to_entries | sort_by(.key) | from_entries | tojson'
    assert_output '{"actions":"read","contents":"write"}'
}

@test "publish collects, describes and verifies before it publishes" {
    collect=$(step_index release.yml publish 'gh run download')
    manifest=$(step_index release.yml publish 'scripts/manifest\.sh dist')
    verify=$(step_index release.yml publish 'scripts/verify\.sh dist')
    create=$(step_index release.yml publish 'gh release create')
    echo "collect=$collect manifest=$manifest verify=$verify create=$create"
    [ "$collect" -ge 0 ] && [ "$manifest" -gt "$collect" ] && [ "$verify" -gt "$manifest" ] && [ "$create" -gt "$verify" ]
}

@test "a final tag publishes every archive, the checksums and the manifest" {
    build_all v1.2.0
    run publish v1.2.0
    assert_success
    run jq -r '.commit' "$repo/dist/manifest.json"
    assert_output "4f2a9c1e0b7d4f2a9c1e0b7d4f2a9c1e0b7d4f2a"
    run gh_calls "release create"
    [ "${#lines[@]}" -eq 1 ] || { echo "expected one gh release create, got: $output"; return 1; }
    call=${lines[0]}
    run jq -r '.[3:] | map(select(startswith("dist/") or startswith("./dist/"))) | map(sub("^.*/"; "")) | sort | join(" ")' <<< "$call"
    assert_output "SHA256SUMS greet-1.2.0-darwin-arm64.tar.gz greet-1.2.0-linux-amd64.tar.gz greet-1.2.0-linux-arm64.tar.gz manifest.json"
    run jq -r '.[2]' <<< "$call"
    assert_output "v1.2.0"
    run jq -r 'index("--verify-tag") != null, index("--prerelease") == null' <<< "$call"
    assert_output "true
true"
}

@test "a pre-release tag is published as a pre-release" {
    build_all v1.3.0-rc.1
    run publish v1.3.0-rc.1
    assert_success
    run gh_calls "release create"
    run jq -r 'index("--prerelease") != null' <<< "${lines[0]}"
    assert_output "true"
}

@test "archives of two versions stop the release before anything is published" {
    build_target v1.2.0 linux-amd64 > /dev/null
    build_target v1.2.0 linux-arm64 > /dev/null
    build_target v1.1.9 darwin-arm64 > /dev/null
    rm -rf "$repo/dist"
    run publish v1.2.0
    assert_failure
    run gh_calls "release create"
    assert_output ""
}

@test "the artifacts of this run are downloaded with the run's token" {
    run wf release.yml '[.jobs.publish.steps[] | select((.run // "") | test("gh run download"))] | first | .env.GH_TOKEN // "" | gsub("[[:space:]]"; "")'
    assert_output --regexp '^\$\{\{(github\.token|secrets\.GITHUB_TOKEN)\}\}$'
    build_all v1.2.0
    publish v1.2.0 > /dev/null
    run gh_calls "run download"
    run jq -r '.[2]' <<< "${lines[0]}"
    assert_output "4242"
}
