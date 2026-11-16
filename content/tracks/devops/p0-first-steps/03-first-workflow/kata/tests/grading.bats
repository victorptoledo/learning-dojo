load /opt/dojo/devops

@test "it runs on pushes to main" {
    run wf ci.yml '.on.push.branches // [] | join(",")'
    assert_output "main"
}

@test "it runs on every pull request" {
    run wf ci.yml '.on | has("pull_request")'
    assert_output "true"
    run wf ci.yml '.on.pull_request.branches // [] | length'
    assert_output "0"
}

@test "the GITHUB_TOKEN can only read the repository" {
    run wf ci.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
}

@test "the test job runs on ubuntu-24.04" {
    run wf ci.yml '.jobs.test["runs-on"]'
    assert_output "ubuntu-24.04"
}

@test "the first step checks out the code" {
    run step_index ci.yml test '^actions/checkout@'
    assert_output "0"
}

@test "Go is set up from go.mod after the checkout" {
    run wf ci.yml '[.jobs.test.steps[] | select((.uses // "") | startswith("actions/setup-go@")) | .with["go-version-file"]] | first // "none"'
    assert_output "go.mod"
    checkout=$(step_index ci.yml test '^actions/checkout@')
    setup=$(step_index ci.yml test '^actions/setup-go@')
    [ "$setup" -gt "$checkout" ] || { echo "todo: set up Go after the checkout"; return 1; }
}

@test "go vet and go test run after Go is set up" {
    setup=$(step_index ci.yml test '^actions/setup-go@')
    vet=$(step_index ci.yml test 'go vet \./\.\.\.')
    test=$(step_index ci.yml test 'go test \./\.\.\.')
    [ "$vet" -gt "$setup" ] || { echo "todo: add a step that runs go vet ./... after setup-go"; return 1; }
    [ "$test" -gt "$setup" ] || { echo "todo: add a step that runs go test ./... after setup-go"; return 1; }
}
