# Shared by the tests of every stage (sourced by their setup).
load /opt/dojo/devops

# A copy of the learner's repository, without the grading tests.
repo="$BATS_TEST_TMPDIR/repo"
mkdir -p "$repo"
cp -r "$KATA/." "$repo/"
rm -rf "$repo/tests" "$repo/.rollout"

spec_file="$BATS_TEST_TMPDIR/rollout.yaml"

# rollout ARGS...: the copy's controller, from the test folder, with a time limit.
rollout() {
    (cd "$BATS_TEST_TMPDIR" && timeout 60 bash "$repo/rollout.sh" "$@" 3>&-)
}

# spec: the rollout file of the test (rollout.yaml in the test folder), from stdin.
spec() {
    cat > "$spec_file"
}

# good_spec [STEPS] [ANALYSIS]: a valid spec, with other steps or analysis if given (YAML
# flow lists or maps).
good_spec() {
    local steps='[{weight: 10, pause: 2m}, {weight: 50, pause: 2m}, {weight: 100}]'
    local analysis="{interval: 1m, query: 'canary_error_ratio', max: 0.01, failure_limit: 2, inconclusive_limit: 3}"
    [ -z "${1:-}" ] || steps=$1
    [ -z "${2:-}" ] || analysis=$2
    spec <<EOF
service: checkout
namespace: shop
route: checkout
stable: checkout-stable
canary: checkout-canary
deployment: checkout-canary
steps: $steps
analysis: $analysis
EOF
}
