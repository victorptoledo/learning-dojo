bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# avail_spec OBJECTIVE WINDOW: one availability SLO for checkout.
avail_spec() {
    spec <<EOF
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - name: availability
        objective: $1
        window: $2
        sli:
          type: availability
          metric: http_requests_total
EOF
}

# steady ERRORS GOOD: an hour of traffic, per minute.
steady() {
    printf '      - series: http_requests_total{job="checkout",code="500"}\n        values: 0+%sx60\n' "$1"
    printf '      - series: http_requests_total{job="checkout",code="200"}\n        values: 0+%sx60\n' "$2"
}

# alert SEVERITY WINDOW: one expected firing alert.
alert() {
    cat <<EOF
          - exp_labels: { service: checkout, slo: availability, severity: $1, long_window: $2 }
            exp_annotations:
              summary: checkout is burning the availability error budget
              runbook_url: https://runbooks.example.com/checkout
EOF
}

# expect TIME [SEVERITY WINDOW]...: the alerts firing at TIME, as an alert_rule_test.
expect() {
    local time=$1
    shift
    printf '    alert_rule_test:\n      - eval_time: %s\n        alertname: SLOErrorBudgetBurn\n' "$time"
    if [ "$#" -eq 0 ]; then
        printf '        exp_alerts: []\n'
        return
    fi
    printf '        exp_alerts:\n'
    while [ "$#" -gt 0 ]; do
        alert "$1" "$2"
        shift 2
    done
}

@test "four alerts per SLO, named SLOErrorBudgetBurn, in slo-SERVICE-alerts" {
    checkout_spec
    gen
    run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r '.groups[] | select(.name == \"slo-checkout-alerts\") | [.rules[] | .alert] | \"\(length) \(unique | join(\",\"))\"'"
    assert_output "8 SLOErrorBudgetBurn"
    run promtool check rules "$out/rules/slos.rules.yml"
    assert_success
}

@test "the alerts are built on the recorded series, not on raw metrics" {
    checkout_spec
    gen
    run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r '.groups[].rules[] | select(.alert) | .expr' | grep -c 'http_request'"
    assert_output "0"
    run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r '[.groups[].rules[] | select(.alert) | .for] | join(\",\")'"
    assert_output "2m,15m,15m,15m,2m,15m,15m,15m"
}

@test "at 2% errors, a 99.9% SLO over 30 days pages and opens tickets" {
    avail_spec 99.9 30d
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:
$(steady 20 980)
$(expect 60m page 1h page 6h ticket 1d ticket 3d)
EOF
    assert_success
}

@test "at 0.5% errors, the same SLO only opens tickets" {
    avail_spec 99.9 30d
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:
$(steady 5 995)
$(expect 60m ticket 1d ticket 3d)
EOF
    assert_success
}

@test "within the budget, nothing fires" {
    avail_spec 99.9 30d
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:
$(steady 1 1999)
$(expect 60m)
EOF
    assert_success
}

@test "a looser objective needs a faster burn" {
    avail_spec 99.5 30d
    gen
    # 1% errors with a 0.5% budget: a burn rate of 2, above only the 3d threshold (1).
    run promtest <<EOF
  - interval: 1m
    input_series:
$(steady 10 990)
$(expect 60m ticket 3d)
EOF
    assert_success
}

@test "a shorter window makes the same errors more urgent" {
    avail_spec 99.5 7d
    gen
    # Same traffic, 7-day window: thresholds 3.36, 1.4, 0.7 and 0.2333 times the budget.
    run promtest <<EOF
  - interval: 1m
    input_series:
$(steady 10 990)
$(expect 60m page 6h ticket 1d ticket 3d)
EOF
    assert_success
}

@test "a 28-day window scales the same way" {
    avail_spec 99 28d
    gen
    # 1% budget, 28 days: factors 13.44, 5.6, 2.8 and 0.9333; 7% errors is a burn rate of 7.
    run promtest <<EOF
  - interval: 1m
    input_series:
$(steady 70 930)
$(expect 60m page 6h ticket 1d ticket 3d)
EOF
    assert_success
}

@test "a page waits for its for, then fires while the burst lasts" {
    avail_spec 99.9 30d
    gen
    # 3% errors for 20 minutes, then none: at 12 minutes only the 1h page (for 2m) fires.
    run promtest <<EOF
  - interval: 1m
    input_series:
      - series: http_requests_total{job="checkout",code="500"}
        values: 0+30x20 600+0x40
      - series: http_requests_total{job="checkout",code="200"}
        values: 0+970x60
$(expect 12m page 1h)
EOF
    assert_success
}

@test "when the burst stops, the pages stop and the tickets stay" {
    avail_spec 99.9 30d
    gen
    run promtest <<EOF
  - interval: 1m
    input_series:
      - series: http_requests_total{job="checkout",code="500"}
        values: 0+30x20 600+0x40
      - series: http_requests_total{job="checkout",code="200"}
        values: 0+970x60
$(expect 60m ticket 1d ticket 3d)
EOF
    assert_success
}

@test "every SLO of every service gets its own alerts" {
    spec <<'EOF'
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - { name: availability, objective: 99.9, window: 30d, sli: { type: availability, metric: http_requests_total } }
  - name: search
    job: search
    runbook: https://runbooks.example.com/search
    slos:
      - { name: availability, objective: 99.9, window: 30d, sli: { type: availability, metric: http_requests_total } }
EOF
    gen
    # Only search fails: only search alerts, with its own runbook.
    run promtest <<'EOF'
  - interval: 1m
    input_series:
      - series: http_requests_total{job="search",code="500"}
        values: 0+20x60
      - series: http_requests_total{job="search",code="200"}
        values: 0+980x60
      - series: http_requests_total{job="checkout",code="200"}
        values: 0+1000x60
    alert_rule_test:
      - eval_time: 60m
        alertname: SLOErrorBudgetBurn
        exp_alerts:
          - exp_labels: { service: search, slo: availability, severity: page, long_window: 1h }
            exp_annotations: { summary: search is burning the availability error budget, runbook_url: https://runbooks.example.com/search }
          - exp_labels: { service: search, slo: availability, severity: page, long_window: 6h }
            exp_annotations: { summary: search is burning the availability error budget, runbook_url: https://runbooks.example.com/search }
          - exp_labels: { service: search, slo: availability, severity: ticket, long_window: 1d }
            exp_annotations: { summary: search is burning the availability error budget, runbook_url: https://runbooks.example.com/search }
          - exp_labels: { service: search, slo: availability, severity: ticket, long_window: 3d }
            exp_annotations: { summary: search is burning the availability error budget, runbook_url: https://runbooks.example.com/search }
EOF
    assert_success
}

@test "the committed rules include the alerts" {
    run bash -c "yq -o=json '.' '$repo/rules/slos.rules.yml' | jq '[.groups[].rules[] | select(.alert)] | length'"
    assert_output "12"
}
