load /opt/dojo/devops

@test "it runs on pushes to main and on every pull request" {
    run wf ci.yml '"\(.on.push.branches // [] | join(",")) \(.on | has("pull_request"))"'
    assert_output "main true"
}

@test "the token is read-only by default" {
    run wf ci.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    policy ci.yml explicit_permissions
}

@test "a newer push cancels the run it makes obsolete" {
    run wf ci.yml '"\(.concurrency["cancel-in-progress"]) \(.concurrency.group // "" | test("github\\.ref"))"'
    assert_output "true true"
}

@test "every action is pinned to a commit SHA" {
    policy ci.yml pinned_actions
}

@test "every job has a timeout" {
    policy ci.yml timeouts
}

@test "lint checks formatting with gofmt and runs go vet" {
    run wf ci.yml '[.jobs.lint.steps[].run // empty] | join("\n")'
    assert_output --partial "gofmt -l"
    assert_output --partial "go vet ./..."
}

@test "test runs go test -race on Go 1.26 and 1.27 without stopping at the first failure" {
    run wf ci.yml '.jobs.test.strategy.matrix.go | map(tostring) | sort | join(",")'
    assert_output "1.26,1.27"
    run wf ci.yml '.jobs.test.strategy["fail-fast"]'
    assert_output "false"
    run wf ci.yml '[.jobs.test.steps[] | select((.uses // "") | startswith("actions/setup-go@")) | .with["go-version"]] | first // ""'
    assert_output '${{ matrix.go }}'
    run wf ci.yml '[.jobs.test.steps[].run // empty] | join("\n")'
    assert_output --partial "go test -race ./..."
}

@test "image waits for lint and test" {
    run wf ci.yml '.jobs.image.needs | if type == "array" then sort | join(",") else . end'
    assert_output "lint,test"
}

@test "only the image job may write packages" {
    run wf ci.yml '.jobs.image.permissions | tojson'
    assert_output '{"contents":"read","packages":"write"}'
    run wf ci.yml '[.jobs.lint.permissions, .jobs.test.permissions] | map(select(. != null)) | length'
    assert_output "0"
}

@test "the image is pushed only on push events, tagged with the commit SHA" {
    run wf ci.yml '[.jobs.image.steps[] | select((.uses // "") | startswith("docker/build-push-action@")) | .with] | first | "\(.push) \(.tags)"'
    assert_output '${{ github.event_name == '"'"'push'"'"' }} ghcr.io/${{ github.repository }}:${{ github.sha }}'
}

@test "the build reuses the GitHub Actions cache" {
    run wf ci.yml '[.jobs.image.steps[] | select((.uses // "") | startswith("docker/build-push-action@")) | .with["cache-from"]] | first // ""'
    assert_output "type=gha"
}

@test "checkouts do not keep the token in the git config" {
    run wf ci.yml '[.jobs[].steps[] | select((.uses // "") | startswith("actions/checkout@")) | (.with["persist-credentials"] | if . == null then true else . end)] | unique | tojson'
    assert_output '[false]'
}
