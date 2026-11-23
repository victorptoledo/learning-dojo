load /opt/dojo/devops

jobs=(lint test)

@test "the workflow is named ci and runs on pushes to main and pull requests" {
    run wf ci.yml '.name'
    assert_output "ci"
    run wf ci.yml '.on.push.branches // [] | join(",")'
    assert_output "main"
    run wf ci.yml '.on | has("pull_request")'
    assert_output "true"
    run wf ci.yml '.on | keys | sort | join(",")'
    assert_output "pull_request,push"
}

@test "the token can only read, at the top and in every job" {
    run wf ci.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    run wf ci.yml '[.jobs[] | select(has("permissions"))] | length'
    assert_output "0"
    policy ci.yml explicit_permissions
}

@test "actions are pinned and nothing untrusted reaches a run script" {
    policy ci.yml pinned_actions
    policy ci.yml no_untrusted_in_run
}

@test "a new run cancels the older one for pull requests only" {
    run wf ci.yml '.concurrency.group'
    assert_output --partial "github.ref"
    run wf ci.yml '.concurrency["cancel-in-progress"] | tostring'
    assert_output --partial "github.event_name"
    assert_output --partial "pull_request"
}

@test "run steps use bash by default" {
    run wf ci.yml '.defaults.run.shell'
    assert_output "bash"
}

@test "lint and test run on ubuntu-24.04 within 10 minutes, test after lint" {
    policy ci.yml timeouts
    run wf ci.yml '.jobs.lint["runs-on"]'
    assert_output "ubuntu-24.04"
    # A later stage turns test into a matrix over runners.
    run wf ci.yml '.jobs.test["runs-on"]'
    assert_output --regexp '^(ubuntu-24\.04|\$\{\{ *matrix\.os *\}\})$'
    for job in "${jobs[@]}"; do
        run wf ci.yml '.jobs[$job]["timeout-minutes"] | . != null and . <= 10' --arg job "$job"
        assert_output "true"
    done
    run wf ci.yml '[.jobs.test.needs] | flatten | join(",")'
    assert_output "lint"
}

@test "both jobs check out the code first" {
    for job in "${jobs[@]}"; do
        run step_index ci.yml "$job" '^actions/checkout@'
        assert_output "0"
    done
}

@test "the tools folder is cached with a key from tools.lock" {
    for job in "${jobs[@]}"; do
        # A later stage adds ${{ runner.arch }} after the OS.
        run wf ci.yml '[.jobs[$job].steps[] | select((.uses // "") | startswith("actions/cache@")) | .with] | first // {} | "\(.path)|\(.key | gsub("[[:space:]]"; ""))|\(.["restore-keys"] // "" | gsub("[[:space:]]"; ""))" | gsub("-\\$\\{\\{runner.arch\\}\\}"; "")' --arg job "$job"
        assert_output "~/.cache/toolbox|toolbox-\${{runner.os}}-\${{hashFiles('tools.lock')}}|toolbox-\${{runner.os}}-"
    done
}

@test "the tools are installed after the cache and put on the PATH" {
    for job in "${jobs[@]}"; do
        cache=$(step_index ci.yml "$job" '^actions/cache@')
        install=$(step_index ci.yml "$job" 'scripts/install-tools\.sh')
        path=$(step_index ci.yml "$job" 'GITHUB_PATH')
        echo "$job: cache=$cache install=$install path=$path"
        [ "$cache" -ge 0 ] && [ "$install" -gt "$cache" ] && [ "$path" -ge "$install" ]
    done
}

@test "lint runs ci.sh lint and test runs ci.sh test, once the tools are there" {
    path=$(step_index ci.yml lint 'GITHUB_PATH')
    lint=$(step_index ci.yml lint 'scripts/ci\.sh lint')
    echo "lint: path=$path ci=$lint"
    [ "$path" -ge 0 ] && [ "$lint" -gt "$path" ]
    path=$(step_index ci.yml test 'GITHUB_PATH')
    test=$(step_index ci.yml test 'scripts/ci\.sh test')
    echo "test: path=$path ci=$test"
    [ "$path" -ge 0 ] && [ "$test" -gt "$path" ]
}
