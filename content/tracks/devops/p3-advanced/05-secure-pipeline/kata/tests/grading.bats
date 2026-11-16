load /opt/dojo/devops

workflows=(pr.yml release.yml)

@test "both workflows set least-privilege permissions" {
    for name in "${workflows[@]}"; do
        run wf "$name" '.permissions | tojson'
        assert_output '{"contents":"read"}'
        policy "$name" explicit_permissions
    done
}

@test "every action is pinned to a commit SHA" {
    for name in "${workflows[@]}"; do
        policy "$name" pinned_actions
    done
}

@test "every job has a timeout" {
    for name in "${workflows[@]}"; do
        policy "$name" timeouts
    done
}

@test "no run script interpolates an expression" {
    for name in "${workflows[@]}"; do
        run wf "$name" '[.jobs[].steps[] | select((.run // "") | test("\\$\\{\\{")) | .name // .run] | join("; ")'
        assert_output ""
    done
}

@test "the pull request title reaches the shell only through env" {
    run wf pr.yml '[.jobs.test.steps[] | select(.env.PR_TITLE == "${{ github.event.pull_request.title }}") | .run] | first // ""'
    assert_output --partial '$PR_TITLE'
}

@test "nothing downloaded is piped into a shell" {
    run grep -nE '(curl|wget)[^|]*\|[[:space:]]*(sudo[[:space:]]+)?(ba|z|da)?sh' .github/workflows/pr.yml .github/workflows/release.yml
    assert_failure
}

@test "the pull request workflow gets no secrets" {
    run wf pr.yml 'tojson | test("secrets[.]")'
    assert_output "false"
}

@test "checkouts keep no token and never fetch the pull request head by hand" {
    for name in "${workflows[@]}"; do
        run wf "$name" '[.jobs[].steps[] | select((.uses // "") | startswith("actions/checkout@")) | (.with["persist-credentials"] | if . == null then true else . end)] | unique | tojson'
        assert_output '[false]'
    done
    run wf pr.yml '[.jobs[].steps[] | select((.uses // "") | startswith("actions/checkout@")) | .with.ref // empty] | length'
    assert_output "0"
}

@test "the release gets AWS credentials through OIDC, never from stored keys" {
    run wf release.yml '.jobs.publish.permissions | tojson'
    assert_output '{"contents":"read","id-token":"write"}'
    run wf release.yml '[.jobs.publish.steps[] | select((.uses // "") | startswith("aws-actions/configure-aws-credentials@")) | .with["role-to-assume"]] | first // ""'
    assert_output "arn:aws:iam::123456789012:role/release"
    run grep -nEi 'aws-access-key-id|aws-secret-access-key|secrets\.AWS' .github/workflows/release.yml
    assert_failure
}

@test "the release job runs in the release environment" {
    run wf release.yml '.jobs.publish.environment | if type == "object" then .name else . end'
    assert_output "release"
}

@test "the deploy token is passed through env and never printed" {
    run wf release.yml '[.jobs.publish.steps[] | select(.env.DEPLOY_TOKEN == "${{ secrets.DEPLOY_TOKEN }}") | .run] | first // ""'
    assert_output --partial "scripts/publish.sh"
    refute_output --partial "echo"
    run wf release.yml '[.jobs.publish.steps[].run // empty] | join("\n")'
    assert_output --partial "publish.sh"
    refute_output --partial "DEPLOY_TOKEN"
}
