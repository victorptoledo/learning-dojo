bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    source "$BATS_TEST_DIRNAME/fakes.sh"
    cluster
}

queries() {
    wc -l < "$PROMQ_LOG"
}

last_line() {
    printf '%s\n' "${lines[${#lines[@]} - 1]}"
}

@test "a healthy canary is measured after every interval and promoted" {
    answers 0.001
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    [ "$(last_line)" = "rollout: checkout promoted" ]
    [ "$(queries)" -eq 4 ] || { echo "promq ran $(queries) times"; return 1; }
    run sort -u "$PROMQ_LOG"
    assert_output 'sum(rate(http_requests_total{service="checkout-canary",code=~"5.."}[1m])) / sum(rate(http_requests_total{service="checkout-canary"}[1m]))'
}

@test "each check prints its result" {
    answers 0.001 0.02 0.003 NaN
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    run grep '^  check ' <<< "$output"
    assert_output "  check 1/2: 0.001 ok
  check 2/2: 0.02 failed
  check 1/2: 0.003 ok
  check 2/2: no data"
}

@test "two failed checks in a row roll the canary back" {
    answers 0.002 0.03 0.04
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 3
    [ "$(last_line)" = "rollout: checkout rolled back at step 2: error ratio 0.04 above 0.01" ] || { echo "$output"; return 1; }
    run weights
    assert_output "90 10
50 50
100 0"
    [ "$(replicas)" = 0 ] || { echo "replicas: $(replicas)"; return 1; }
    [ "$(queries)" -eq 3 ] || { echo "promq ran $(queries) times"; return 1; }
}

@test "the rollback does not wait for the rest of the pause" {
    good_spec '[{weight: 10, pause: 10m}, {weight: 100}]' '{interval: 1m, query: q, max: 0.01, failure_limit: 1}'
    answers 0.5
    run --separate-stderr rollout run "$spec_file"
    assert_failure 3
    [ "$(last_line)" = "rollout: checkout rolled back at step 1: error ratio 0.5 above 0.01" ]
    run cat "$SLEEP_LOG"
    assert_output "60"
    run weights
    assert_output "90 10
100 0"
}

@test "a single bad check between good ones does not roll back" {
    answers 0.02 0.001 0.02 0.001
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    run weights
    assert_line --index 2 "0 100"
}

@test "the maximum itself is still good, and numbers may use an exponent" {
    answers 0.01 1e-4 0.01 9E-3
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    [ "$(last_line)" = "rollout: checkout promoted" ]
}

@test "three answers without data in a row roll back" {
    answers NaN empty error
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 3
    [ "$(last_line)" = "rollout: checkout rolled back at step 2: no data from the analysis" ] || { echo "$output"; return 1; }
    [ "$(replicas)" = 0 ]
    run weights
    assert_line --index 2 "100 0"
}

@test "the inconclusive limit comes from the spec" {
    good_spec '' '{interval: 1m, query: q, max: 0.01, failure_limit: 2, inconclusive_limit: 1}'
    answers "not a number"
    run --separate-stderr rollout run "$spec_file"
    assert_failure 3
    [ "$(last_line)" = "rollout: checkout rolled back at step 1: no data from the analysis" ]
    [ "$(queries)" -eq 1 ]
}

@test "without data, a failure still counts towards the limit" {
    answers 0.05 NaN 0.05
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 3
    [ "$(last_line)" = "rollout: checkout rolled back at step 2: error ratio 0.05 above 0.01" ] || { echo "$output"; return 1; }
}

@test "a failed check ends a run of inconclusive ones" {
    answers NaN NaN 0.05 NaN
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    [ "$(last_line)" = "rollout: checkout promoted" ] || { echo "$output"; return 1; }
}

@test "a good check ends a run of failures" {
    good_spec '[{weight: 10, pause: 4m}, {weight: 100}]' '{interval: 1m, query: q, max: 0.01, failure_limit: 2}'
    answers 0.05 0.001 0.05 0.001
    run --separate-stderr rollout run "$spec_file"
    assert_success
}

@test "the rollback leaves the testers' header rule alone" {
    before=$(jq -c '.spec.rules[0]' "$KUBE_STATE/httproute-checkout.json")
    answers 0.9
    run rollout run "$repo/rollout.yaml"
    assert_failure 3
    [ "$(jq -c '.spec.rules[0]' "$KUBE_STATE/httproute-checkout.json")" = "$before" ]
}
