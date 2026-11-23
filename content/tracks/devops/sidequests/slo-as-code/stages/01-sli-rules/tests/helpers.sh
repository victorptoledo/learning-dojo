# Shared by the tests of every stage (sourced by their setup).
load /opt/dojo/devops

# A copy of the learner's repository, without the grading tests.
repo="$BATS_TEST_TMPDIR/repo"
mkdir -p "$repo"
cp -r "$KATA/." "$repo/"
rm -rf "$repo/tests"

out="$BATS_TEST_TMPDIR/out"
spec_file="$BATS_TEST_TMPDIR/slos.yaml"

# slogen ARGS...: the copy's generator, from an unrelated directory, with a time limit.
slogen() {
    (cd "$BATS_TEST_TMPDIR" && timeout 60 bash "$repo/scripts/slogen.sh" "$@" 3>&-)
}

# spec: the SLO file the next `gen` reads, from stdin.
spec() {
    cat > "$spec_file"
}

# gen: generates $spec_file into $out.
gen() {
    slogen generate "$spec_file" "$out"
}

# One service, two SLOs: the usual test spec. OBJECTIVE and WINDOW change the first one.
checkout_spec() {
    spec <<EOF
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - name: availability
        objective: ${1:-99.9}
        window: ${2:-30d}
        sli:
          type: availability
          metric: http_requests_total
      - name: latency
        objective: 99
        window: 30d
        sli:
          type: latency
          metric: http_request_duration_seconds
          threshold: 0.3
EOF
}

# promtest: runs `promtool test rules` on the generated rules with the test cases on stdin
# (the YAML under `tests:`), and prints promtool's report when it fails.
promtest() {
    {
        printf 'rule_files: [%s]\nevaluation_interval: 1m\ntests:\n' "${RULE_FILES:-rules/slos.rules.yml}"
        cat
    } > "$out/case.test.yml"
    (cd "$out" && promtool test rules case.test.yml)
}

# rules_json: the generated rules as JSON.
rules_json() {
    yq -o=json -I=0 '.' "$out/rules/slos.rules.yml"
}
