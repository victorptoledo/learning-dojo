bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

windows="5m 30m 1h 2h 6h 1d 3d"

# Steady traffic for an hour: 1000 requests a minute to checkout, 1% of them 5xx (404s are
# not errors), 5% slower than 0.3 s; search fails half of its requests.
traffic='
      - series: http_requests_total{job="checkout",code="200",path="/pay"}
        values: 0+980x60
      - series: http_requests_total{job="checkout",code="404",path="/pay"}
        values: 0+10x60
      - series: http_requests_total{job="checkout",code="503",path="/pay"}
        values: 0+10x60
      - series: http_requests_total{job="search",code="500"}
        values: 0+500x60
      - series: http_requests_total{job="search",code="200"}
        values: 0+500x60
      - series: http_request_duration_seconds_bucket{job="checkout",le="0.1"}
        values: 0+800x60
      - series: http_request_duration_seconds_bucket{job="checkout",le="0.3"}
        values: 0+950x60
      - series: http_request_duration_seconds_bucket{job="checkout",le="+Inf"}
        values: 0+1000x60
      - series: http_request_duration_seconds_count{job="checkout"}
        values: 0+1000x60'

@test "generates a rule file that promtool accepts, and says so" {
    checkout_spec
    run --separate-stderr gen
    assert_success
    count=$(rules_json | jq '[.groups[].rules[]] | length')
    assert_line --index 0 "slogen: wrote rules/slos.rules.yml ($count rules)"
    run head -1 "$out/rules/slos.rules.yml"
    assert_output --regexp '^#.*[Gg]enerated'
    run promtool check rules "$out/rules/slos.rules.yml"
    assert_success
}

@test "one group per service, named slo-SERVICE" {
    spec <<'EOF'
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - { name: availability, objective: 99.9, window: 30d, sli: { type: availability, metric: http_requests_total } }
  - name: search-api
    job: search
    runbook: https://runbooks.example.com/search
    slos:
      - { name: availability, objective: 99.5, window: 28d, sli: { type: availability, metric: http_requests_total } }
EOF
    gen
    run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r '[.groups[].name | select(startswith(\"slo-\") and (endswith(\"-alerts\") | not))] | join(\",\")'"
    assert_output "slo-checkout,slo-search-api"
}

@test "every SLO records its error ratio over the seven windows and its budget" {
    checkout_spec
    gen
    for slo in availability latency; do
        run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r --arg slo $slo '[.groups[] | select(.name == \"slo-checkout\") | .rules[] | select(.record and .labels.slo == \$slo) | .record] | unique | join(\" \") + \" \"'"
        for window in $windows; do
            assert_output --partial "slo:sli_error:ratio_rate$window "
        done
        assert_output --partial "slo:error_budget:ratio"
    done
}

@test "every recording rule is labelled with exactly its service and SLO" {
    checkout_spec
    gen
    run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r '[.groups[].rules[] | select(.record) | .labels // {} | keys | join(\",\")] | unique | join(\" \")'"
    assert_output "service,slo"
}

@test "the availability error ratio counts the 5xx of its own job only" {
    checkout_spec
    gen
    cases=""
    for window in $windows; do
        cases+="
      - expr: round(slo:sli_error:ratio_rate$window{service=\"checkout\",slo=\"availability\"} * 1e6)
        eval_time: 60m
        exp_samples:
          - labels: '{service=\"checkout\", slo=\"availability\"}'
            value: 10000"
    done
    run promtest <<EOF
  - interval: 1m
    input_series:$traffic
    promql_expr_test:$cases
EOF
    assert_success
}

@test "the latency error ratio is the share slower than the threshold" {
    checkout_spec
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:$traffic
    promql_expr_test:
      - expr: round(slo:sli_error:ratio_rate5m{service="checkout",slo="latency"} * 1e6)
        eval_time: 60m
        exp_samples:
          - labels: '{service="checkout", slo="latency"}'
            value: 50000
      - expr: round(slo:sli_error:ratio_rate1d{service="checkout",slo="latency"} * 1e6)
        eval_time: 60m
        exp_samples:
          - labels: '{service="checkout", slo="latency"}'
            value: 50000
EOF
    assert_success
}

@test "the threshold picks the bucket" {
    spec <<'EOF'
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - { name: fast, objective: 90, window: 7d, sli: { type: latency, metric: http_request_duration_seconds, threshold: 0.1 } }
EOF
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:$traffic
    promql_expr_test:
      - expr: round(slo:sli_error:ratio_rate30m{service="checkout",slo="fast"} * 1e6)
        eval_time: 60m
        exp_samples:
          - labels: '{service="checkout", slo="fast"}'
            value: 200000
EOF
    assert_success
}

@test "the error budget is 1 - objective, exactly" {
    checkout_spec 99.95
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:$traffic
    promql_expr_test:
      - expr: slo:error_budget:ratio{service="checkout",slo="availability"}
        eval_time: 10m
        exp_samples:
          - labels: 'slo:error_budget:ratio{service="checkout", slo="availability"}'
            value: 0.0005
      - expr: slo:error_budget:ratio{service="checkout",slo="latency"}
        eval_time: 10m
        exp_samples:
          - labels: 'slo:error_budget:ratio{service="checkout", slo="latency"}'
            value: 0.01
EOF
    assert_success
}

@test "the same spec always gives the same file" {
    checkout_spec
    gen > /dev/null
    cp "$out/rules/slos.rules.yml" "$BATS_TEST_TMPDIR/first.yml"
    gen > /dev/null
    diff "$BATS_TEST_TMPDIR/first.yml" "$out/rules/slos.rules.yml"
}

@test "the committed rules are what slos.yaml generates today" {
    [ -f "$repo/rules/slos.rules.yml" ] || { echo "todo: generate and commit rules/slos.rules.yml"; return 1; }
    slogen generate "$repo/slos.yaml" "$out" > /dev/null
    diff "$out/rules/slos.rules.yml" "$repo/rules/slos.rules.yml" \
        || { echo "rules/slos.rules.yml is stale: run bash scripts/slogen.sh generate slos.yaml ."; return 1; }
}

@test "a bad SLO is named in the error, and nothing is written" {
    spec <<'EOF'
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - { name: availability, objective: 100, window: 30d, sli: { type: availability, metric: http_requests_total } }
EOF
    run --separate-stderr gen
    assert_failure 1
    [ "$stderr" = "slogen: checkout/availability: objective must be a number between 0 and 100" ] || { echo "stderr: $stderr"; return 1; }
    [ ! -e "$out/rules" ] || { echo "a bad spec still wrote $out/rules"; return 1; }
}

@test "every problem of the spec is reported, one per line" {
    spec <<'EOF'
services:
  - name: checkout
    runbook: http://runbooks.example.com/checkout
    slos:
      - { name: availability, objective: 99.9, window: 90d, sli: { type: availability, metric: http_requests_total } }
      - { name: latency, objective: 99, window: 30d, sli: { type: latency, metric: http_request_duration_seconds } }
      - { name: Errors, objective: high, window: 7d, sli: { type: saturation, metric: cpu } }
  - name: search
    job: search
    runbook: https://runbooks.example.com/search
    slos: []
EOF
    run --separate-stderr gen
    assert_failure 1
    expected=(
        "slogen: checkout: no job"
        "slogen: checkout: runbook must be an https:// URL"
        "slogen: checkout/availability: window must be 7d, 28d or 30d"
        "slogen: checkout/latency: a latency sli needs a threshold in seconds"
        "slogen: checkout/Errors: the slo name must match [a-z][a-z0-9-]*"
        "slogen: checkout/Errors: objective must be a number between 0 and 100"
        "slogen: checkout/Errors: sli.type must be availability or latency"
        "slogen: search: no slos"
    )
    for line in "${expected[@]}"; do
        grep -qxF -- "$line" <<< "$stderr" || { echo "missing: $line"; echo "stderr: $stderr"; return 1; }
    done
    [ "$(wc -l <<< "$stderr")" -eq "${#expected[@]}" ] || { echo "stderr: $stderr"; return 1; }
}

@test "no services, no file or a wrong command line fail" {
    echo "services: []" | spec
    run --separate-stderr gen
    assert_failure 1
    [ "$stderr" = "slogen: no services" ] || { echo "stderr: $stderr"; return 1; }
    run --separate-stderr slogen generate "$BATS_TEST_TMPDIR/missing.yaml" "$out"
    assert_failure 1
    run --separate-stderr slogen generate "$spec_file"
    assert_failure 2
    run --separate-stderr slogen publish "$spec_file" "$out"
    assert_failure 2
}
