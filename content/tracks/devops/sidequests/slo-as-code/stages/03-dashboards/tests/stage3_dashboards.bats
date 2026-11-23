bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

board() {
    jq -r "$2" "$out/dashboards/$1.json"
}

@test "one dashboard per service, and a removed service loses its dashboard" {
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
      - { name: availability, objective: 99.5, window: 7d, sli: { type: availability, metric: http_requests_total } }
EOF
    run --separate-stderr gen
    assert_success
    assert_line "slogen: wrote dashboards/checkout.json"
    assert_line "slogen: wrote dashboards/search.json"
    touch "$out/dashboards/README.md"
    checkout_spec
    gen > /dev/null
    run ls "$out/dashboards"
    assert_output "README.md
checkout.json"
}

@test "the dashboard is named after the service and not editable" {
    checkout_spec
    gen > /dev/null
    run board checkout '"\(.uid)|\(.title)|\(.tags | join(","))|\(.schemaVersion)|\(.editable)"'
    assert_output "slo-checkout|checkout SLOs|slo,generated|39|false"
}

@test "every panel and query uses the datasource variable" {
    checkout_spec
    gen > /dev/null
    run board checkout '.templating.list | map({name, type, query}) | tojson'
    assert_output '[{"name":"datasource","type":"datasource","query":"prometheus"}]'
    run board checkout '[.panels[] | .datasource, .targets[].datasource | tojson] | unique | join(" ")'
    assert_output '{"type":"prometheus","uid":"${datasource}"}'
}

@test "three panels per SLO: a stat, a gauge and a burn-rate graph" {
    checkout_spec
    gen > /dev/null
    run board checkout '[.panels[] | "\(.type):\(.title)"] | join("|")'
    assert_output "stat:availability: SLI over 30d|gauge:availability: error budget left|timeseries:availability: burn rate|stat:latency: SLI over 30d|gauge:latency: error budget left|timeseries:latency: burn rate"
    run board checkout '[.panels[].id] | (length == (unique | length))'
    assert_output "true"
    run board checkout '[.panels[] | select(.type == "timeseries") | [.targets[] | "\(.refId)=\(.legendFormat)"] | join(",")] | unique | join(" ")'
    assert_output "A=1h,B=6h"
}

@test "the panels fit the grid, one row per SLO, without overlapping" {
    checkout_spec
    gen > /dev/null
    run board checkout '[.panels[].gridPos | select(.x < 0 or .w <= 0 or .h <= 0 or .x + .w > 24)] | length'
    assert_output "0"
    run board checkout '[.panels[].gridPos] as $g | [range(0; $g | length) as $i | range($i + 1; $g | length) as $j
        | select($g[$i].x < $g[$j].x + $g[$j].w and $g[$j].x < $g[$i].x + $g[$i].w
                 and $g[$i].y < $g[$j].y + $g[$j].h and $g[$j].y < $g[$i].y + $g[$i].h)
        | "panels \($i) and \($j) overlap"] | join("\n")'
    assert_output ""
    run board checkout '[.panels[0:3][].gridPos.y] + [.panels[3:6][].gridPos.y] | . as $y | ($y[0:3] | unique | length) == 1 and ($y[3:6] | unique | length) == 1 and $y[3] > $y[0]'
    assert_output "true"
}

@test "the rules record the error ratio over the SLO's own window" {
    spec <<'EOF'
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - { name: weekly, objective: 99, window: 7d, sli: { type: availability, metric: http_requests_total } }
      - { name: monthly, objective: 99, window: 28d, sli: { type: availability, metric: http_requests_total } }
EOF
    gen > /dev/null
    run bash -c "yq -o=json '.' '$out/rules/slos.rules.yml' | jq -r '[.groups[].rules[] | select(.record) | \"\(.labels.slo):\(.record)\"] | map(select(endswith(\"7d\") or endswith(\"28d\"))) | join(\" \")'"
    assert_output "weekly:slo:sli_error:ratio_rate7d monthly:slo:sli_error:ratio_rate28d"
}

@test "the queries read only the recorded slo: series" {
    checkout_spec
    gen > /dev/null
    run bash -c "jq -r '.panels[].targets[].expr' '$out/dashboards/checkout.json' | grep -oE '[a-zA-Z_:][a-zA-Z0-9_:]*\\{' | sort -u | grep -v '^slo:'"
    assert_output ""
    run bash -c "jq -r '.panels[].targets[].expr' '$out/dashboards/checkout.json' | grep -c 'http_request'"
    assert_output "0"
}

@test "the panels show the SLI, the budget left and the burn rate" {
    spec <<'EOF'
services:
  - name: checkout
    job: checkout
    runbook: https://runbooks.example.com/checkout
    slos:
      - { name: availability, objective: 98, window: 30d, sli: { type: availability, metric: http_requests_total } }
EOF
    gen > /dev/null
    # Every query of the dashboard as a recording rule: dash:TYPE:REFID.
    jq '{groups: [{name: "dashboard", rules: [.panels[] | .type as $type | .targets[] | {record: "dash:\($type):\(.refId)", expr: "round((\(.expr)) * 1e6)"}]}]}' \
        "$out/dashboards/checkout.json" | yq -P > "$out/dash.rules.yml"
    run promtool check rules "$out/dash.rules.yml"
    assert_success
    RULE_FILES="rules/slos.rules.yml, dash.rules.yml" run promtest <<'EOF'
  - interval: 1m
    input_series:
      - series: http_requests_total{job="checkout",code="500"}
        values: 0+10x120
      - series: http_requests_total{job="checkout",code="200"}
        values: 0+990x120
    promql_expr_test:
      - expr: dash:stat:A
        eval_time: 100m
        exp_samples:
          - labels: 'dash:stat:A{service="checkout", slo="availability"}'
            value: 990000
      - expr: dash:gauge:A
        eval_time: 100m
        exp_samples:
          - labels: 'dash:gauge:A{service="checkout", slo="availability"}'
            value: 500000
      - expr: dash:timeseries:A
        eval_time: 100m
        exp_samples:
          - labels: 'dash:timeseries:A{service="checkout", slo="availability"}'
            value: 500000
      - expr: dash:timeseries:B
        eval_time: 100m
        exp_samples:
          - labels: 'dash:timeseries:B{service="checkout", slo="availability"}'
            value: 500000
EOF
    assert_success
}

@test "the committed dashboards are what slos.yaml generates today" {
    slogen generate "$repo/slos.yaml" "$out" > /dev/null
    for name in checkout search; do
        [ -f "$repo/dashboards/$name.json" ] || { echo "todo: generate and commit dashboards/$name.json"; return 1; }
        diff "$out/dashboards/$name.json" "$repo/dashboards/$name.json" \
            || { echo "dashboards/$name.json is stale: run bash scripts/slogen.sh generate slos.yaml ."; return 1; }
    done
}
