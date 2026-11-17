load /opt/dojo/devops

@test "the workflow runs on pushes to main and on every pull request" {
    run wf ci.yml '.on.push.branches // [] | join(",")'
    assert_output "main"
    run wf ci.yml '.on | has("pull_request")'
    assert_output "true"
}

@test "the token can only read the repository" {
    run wf ci.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
}

@test "the test job runs on ubuntu-24.04 with a timeout" {
    run wf ci.yml '.jobs.test["runs-on"]'
    assert_output "ubuntu-24.04"
    run wf ci.yml '.jobs.test["timeout-minutes"] // "none" | tostring'
    refute_output "none"
}

@test "the first step checks out the code" {
    run step_index ci.yml test '^actions/checkout@'
    assert_output "0"
}

@test "formatting, clippy with -D warnings and the tests run after the checkout" {
    checkout=$(step_index ci.yml test '^actions/checkout@')
    fmt=$(step_index ci.yml test 'cargo fmt.*--check')
    clippy=$(step_index ci.yml test 'cargo clippy.*-D warnings')
    test=$(step_index ci.yml test 'cargo test')
    [ "$fmt" -gt "$checkout" ] || { echo "todo: run cargo fmt --check after the checkout"; return 1; }
    [ "$clippy" -gt "$checkout" ] || { echo "todo: run cargo clippy with -D warnings after the checkout"; return 1; }
    [ "$test" -gt "$checkout" ] || { echo "todo: run cargo test after the checkout"; return 1; }
}

@test "the alert rule CacheHitRatioLow is a ticket after 10 minutes" {
    run rule alerts.rules.yml CacheHitRatioLow '.labels.severity'
    assert_output "ticket"
    run rule alerts.rules.yml CacheHitRatioLow '.for'
    assert_output "10m"
}

@test "the alert is computed from the rates of both counters" {
    run rule alerts.rules.yml CacheHitRatioLow '.expr'
    assert_output --partial "cache_hits_total"
    assert_output --partial "cache_misses_total"
    assert_output --partial "rate("
}
