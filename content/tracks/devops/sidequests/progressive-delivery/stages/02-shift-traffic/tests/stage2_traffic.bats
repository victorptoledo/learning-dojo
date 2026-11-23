bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    source "$BATS_TEST_DIRNAME/fakes.sh"
    cluster
}

# The first rule of the route as it is now (the testers' header rule).
header_rule() {
    jq -c '.spec.rules[0]' "$KUBE_STATE/httproute-checkout.json"
}

@test "run moves the traffic step by step and promotes the canary" {
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    assert_output --partial "step 1/3: weight 10"
    assert_output --partial "step 2/3: weight 50"
    assert_output --partial "step 3/3: weight 100"
    assert_line --index $((${#lines[@]} - 1)) "rollout: checkout promoted"
    run weights
    assert_output "90 10
50 50
0 100"
}

@test "the steps are printed in order" {
    run --separate-stderr rollout run "$repo/rollout.yaml"
    run grep -E '^step ' <<< "$output"
    assert_output "step 1/3: weight 10
step 2/3: weight 50
step 3/3: weight 100"
}

@test "each pause is a sleep of one interval per check" {
    rollout run "$repo/rollout.yaml" > /dev/null
    run cat "$SLEEP_LOG"
    assert_output "60
60
60
60"
}

@test "pauses in seconds and hours, and a step without a pause" {
    good_spec '[{weight: 20}, {weight: 40, pause: 90s}, {weight: 100}]' '{interval: 30s, query: q, max: 0.01, failure_limit: 2}'
    rollout run "$spec_file" > /dev/null
    run cat "$SLEEP_LOG"
    assert_output "30
30
30"
    good_spec '[{weight: 50, pause: 1h}, {weight: 100}]' '{interval: 30m, query: q, max: 0.01, failure_limit: 2}'
    : > "$SLEEP_LOG"
    # A later stage remembers finished rollouts in .rollout: start this one afresh.
    rm -rf "$BATS_TEST_TMPDIR/.rollout"
    rollout run "$spec_file" > /dev/null
    run cat "$SLEEP_LOG"
    assert_output "1800
1800"
}

@test "the testers' header rule is never touched" {
    before=$(header_rule)
    rollout run "$repo/rollout.yaml" > /dev/null
    [ "$(header_rule)" = "$before" ] || { echo "rule 0 changed: $(header_rule)"; return 1; }
}

@test "the backends are found by name, in any order and any rule" {
    jq '.spec.rules |= reverse | .spec.rules[0].backendRefs |= reverse' "$KUBE_STATE/httproute-checkout.json" > "$BATS_TEST_TMPDIR/r.json"
    mv "$BATS_TEST_TMPDIR/r.json" "$KUBE_STATE/httproute-checkout.json"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    run weights
    assert_output "90 10
50 50
0 100"
    run jq -c '.spec.rules[0].backendRefs | map({(.name): .weight}) | add' "$KUBE_STATE/httproute-checkout.json"
    assert_output '{"checkout-canary":100,"checkout-stable":0}'
}

@test "each step moves both weights in one patch" {
    rollout run "$repo/rollout.yaml" > /dev/null
    run jq -s '[.[] | select(index("patch"))] | length' "$KUBE_LOG"
    assert_output "3"
}

@test "the canary gets a replica before the first step, in the spec's namespace" {
    [ "$(replicas)" = 0 ]
    rollout run "$repo/rollout.yaml" > /dev/null
    [ "$(replicas)" = 1 ] || { echo "replicas: $(replicas)"; return 1; }
    run jq -r 'if (index("-n") // index("--namespace")) == null and (map(startswith("--namespace=")) | any | not) then "no namespace: \(.)" else empty end' "$KUBE_LOG"
    assert_output ""
    run jq -s -r 'map(.[index("scale") // index("patch") // 0]) | map(select(. == "scale" or . == "patch")) | first' "$KUBE_LOG"
    assert_output "scale"
}

@test "another namespace and other names come from the spec" {
    KUBE_NAMESPACE=payments STABLE=pay-v1 CANARY=pay-v2 cluster
    mv "$KUBE_STATE/httproute-checkout.json" "$KUBE_STATE/httproute-pay.json"
    spec <<'EOF'
service: pay
namespace: payments
route: pay
stable: pay-v1
canary: pay-v2
deployment: pay-v2
steps: [{weight: 25, pause: 1m}, {weight: 100}]
analysis: {interval: 1m, query: q, max: 0.01, failure_limit: 1}
EOF
    KUBE_NAMESPACE=payments STABLE=pay-v1 CANARY=pay-v2 run --separate-stderr rollout run "$spec_file"
    assert_success
    assert_line --index $((${#lines[@]} - 1)) "rollout: pay promoted"
    run weights
    assert_output "75 25
0 100"
}

@test "a route without both backends stops the rollout before any patch" {
    jq '.spec.rules[1].backendRefs |= map(select(.name != "checkout-canary"))' "$KUBE_STATE/httproute-checkout.json" > "$BATS_TEST_TMPDIR/r.json"
    mv "$BATS_TEST_TMPDIR/r.json" "$KUBE_STATE/httproute-checkout.json"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 1
    [ "$stderr" = "rollout: httproute checkout has no rule with both checkout-stable and checkout-canary" ] || { echo "stderr: $stderr"; return 1; }
    run weights
    assert_output ""
}

@test "a missing route, a failing patch or a failing scale stop the rollout" {
    mv "$KUBE_STATE/httproute-checkout.json" "$BATS_TEST_TMPDIR/away.json"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 1
    grep -qxF "rollout: cannot read httproute checkout" <<< "$stderr" || { echo "stderr: $stderr"; return 1; }
    mv "$BATS_TEST_TMPDIR/away.json" "$KUBE_STATE/httproute-checkout.json"
    KUBE_FAIL=patch run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 1
    grep -qxF "rollout: cannot patch httproute checkout" <<< "$stderr" || { echo "stderr: $stderr"; return 1; }
    KUBE_FAIL=scale run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 1
    grep -qxF "rollout: cannot scale deployment checkout-canary" <<< "$stderr" || { echo "stderr: $stderr"; return 1; }
}

@test "a bad spec never reaches the cluster" {
    good_spec '[{weight: 60}, {weight: 30}]'
    run --separate-stderr rollout run "$spec_file"
    assert_failure 1
    run cat "$KUBE_LOG"
    assert_output ""
}
