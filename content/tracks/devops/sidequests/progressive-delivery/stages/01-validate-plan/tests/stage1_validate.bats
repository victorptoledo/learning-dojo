bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# problems: runs validate on the test spec; fails unless it exits 1.
problems() {
    run --separate-stderr rollout validate "$spec_file"
    [ "$status" -eq 1 ] || { echo "validate exited $status: $output $stderr"; return 1; }
}

# has LINE: the last validate printed LINE on stderr.
has() {
    grep -qxF -- "$1" <<< "$stderr" || { echo "missing: $1"; echo "stderr: $stderr"; return 1; }
}

@test "the repository's rollout.yaml is valid" {
    run --separate-stderr rollout validate "$repo/rollout.yaml"
    assert_success
    assert_output "rollout: $repo/rollout.yaml is valid"
}

@test "plan prints every step, its pause and its checks" {
    run --separate-stderr rollout plan "$repo/rollout.yaml"
    assert_success
    assert_output "checkout: 3 steps, analysis every 60s (max error ratio 0.01, 2 failures roll back)
step 1/3: weight 10, pause 120s, 2 checks
step 2/3: weight 50, pause 120s, 2 checks
step 3/3: weight 100"
}

@test "plan counts pauses in seconds and checks in intervals" {
    good_spec '[{weight: 5, pause: 30s}, {weight: 25, pause: 1h}, {weight: 100}]' \
        '{interval: 30s, query: q, max: 0.05, failure_limit: 3}'
    run --separate-stderr rollout plan "$spec_file"
    assert_success
    assert_output "checkout: 3 steps, analysis every 30s (max error ratio 0.05, 3 failures roll back)
step 1/3: weight 5, pause 30s, 1 check
step 2/3: weight 25, pause 3600s, 120 checks
step 3/3: weight 100"
}

@test "a single step straight to 100 is a valid rollout" {
    good_spec '[{weight: 100}]'
    run --separate-stderr rollout validate "$spec_file"
    assert_success
}

@test "the names are required and must be Kubernetes names" {
    spec <<'EOF'
service: Checkout
route: checkout_route
stable: checkout-stable-
canary: checkout-canary
deployment: checkout-canary-very-long-name-that-goes-on-and-on-for-more-than-63-chars
steps: [{weight: 100}]
analysis: {interval: 1m, query: q, max: 0.01, failure_limit: 2}
EOF
    problems
    has "rollout: service: must be a Kubernetes name"
    has "rollout: namespace: is required"
    has "rollout: route: must be a Kubernetes name"
    has "rollout: stable: must be a Kubernetes name"
    has "rollout: deployment: must be a Kubernetes name"
    [ "$(wc -l <<< "$stderr")" -eq 5 ] || { echo "stderr: $stderr"; return 1; }
}

@test "weights go up from 1 to 100 and end at 100" {
    good_spec '[{weight: 0}, {weight: 30}, {weight: 30}, {weight: 20.5}, {weight: 150}, {weight: 90}]'
    problems
    has "rollout: steps[0].weight: must be an integer from 1 to 100"
    has "rollout: steps[2].weight: must be greater than the previous step"
    has "rollout: steps[3].weight: must be an integer from 1 to 100"
    has "rollout: steps[4].weight: must be an integer from 1 to 100"
    has "rollout: steps: the last step must reach weight 100"
}

@test "a step going down is refused" {
    good_spec '[{weight: 50}, {weight: 20}, {weight: 100}]'
    problems
    [ "$stderr" = "rollout: steps[1].weight: must be greater than the previous step" ] || { echo "stderr: $stderr"; return 1; }
}

@test "pauses are durations and whole numbers of checks" {
    good_spec '[{weight: 10, pause: 2 minutes}, {weight: 20, pause: 90s}, {weight: 50, pause: 1h}, {weight: 100}]'
    problems
    has "rollout: steps[0].pause: must be a duration like 30s, 5m or 1h"
    has "rollout: steps[1].pause: must be a multiple of analysis.interval"
    [ "$(wc -l <<< "$stderr")" -eq 2 ] || { echo "stderr: $stderr"; return 1; }
}

@test "the analysis is checked field by field" {
    good_spec '[{weight: 100}]' '{interval: 0s, max: 2, failure_limit: 0, inconclusive_limit: 1.5}'
    problems
    has "rollout: analysis.interval: must be a duration like 30s, 5m or 1h"
    has "rollout: analysis.query: is required"
    has "rollout: analysis.max: must be a number from 0 to 1"
    has "rollout: analysis.failure_limit: must be an integer of at least 1"
    has "rollout: analysis.inconclusive_limit: must be an integer of at least 1"
}

@test "steps and analysis are required" {
    spec <<'EOF'
service: checkout
namespace: shop
route: checkout
stable: checkout-stable
canary: checkout-canary
deployment: checkout-canary
EOF
    problems
    has "rollout: steps: at least one step is required"
    has "rollout: analysis: is required"
}

@test "plan refuses a bad spec, and prints nothing on stdout" {
    good_spec '[{weight: 10}]'
    run --separate-stderr rollout plan "$spec_file"
    assert_failure 1
    assert_output ""
    has "rollout: steps: the last step must reach weight 100"
}

@test "a missing file fails, and a wrong command line exits 2" {
    run --separate-stderr rollout validate "$BATS_TEST_TMPDIR/nope.yaml"
    assert_failure 1
    good_spec
    run --separate-stderr rollout validate
    assert_failure 2
    run --separate-stderr rollout deploy "$spec_file"
    assert_failure 2
    run --separate-stderr rollout validate "$spec_file" extra
    assert_failure 2
}
