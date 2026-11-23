bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    cd "$BATS_TEST_TMPDIR"
}

# write NAME: a rule file in the test folder, from stdin.
write() {
    cat > "$BATS_TEST_TMPDIR/$1"
}

@test "the generated rules pass the lint" {
    run --separate-stderr slogen lint "$repo/rules/slos.rules.yml"
    assert_success
    count=$(yq -o=json '.' "$repo/rules/slos.rules.yml" | jq '[.groups[].rules[]] | length')
    assert_output "lint: ok ($count rules)"
}

@test "the lint reports every problem of an alert" {
    write bad.rules.yml <<'EOF'
groups:
  - name: app
    rules:
      - alert: HighLatency
        expr: histogram_quantile(0.99, sum by (le) (rate(http_request_duration_seconds_bucket[5m]))) > 1
        labels: { severity: critical }
      - alert: DiskFull
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes < 0.05
        for: 10m
        labels: { severity: page }
        annotations:
          summary: "{{ $labels.instance }} is almost full"
          runbook_url: http://runbooks.example.com/disk
      - alert: Fine
        expr: up == 0
        for: 5m
        labels: { severity: ticket }
        annotations: { summary: down, runbook_url: https://runbooks.example.com/up }
EOF
    run --separate-stderr slogen lint bad.rules.yml
    assert_failure 1
    assert_output "bad.rules.yml: HighLatency: no for
bad.rules.yml: HighLatency: severity must be page or ticket
bad.rules.yml: HighLatency: no summary
bad.rules.yml: HighLatency: no runbook_url
bad.rules.yml: DiskFull: no runbook_url
lint: failed"
}

@test "the lint checks record names and duplicates" {
    write records.rules.yml <<'EOF'
groups:
  - name: one
    rules:
      - record: job:http_requests:rate5m
        expr: sum by (job) (rate(http_requests_total[5m]))
      - record: http_requests_rate5m
        expr: sum(rate(http_requests_total[5m]))
      - record: instance:node_cpu:ratio
        expr: avg by (instance) (rate(node_cpu_seconds_total{mode!="idle"}[5m]))
        labels: { team: infra }
  - name: two
    rules:
      - record: job:http_requests:rate5m
        expr: sum by (job) (rate(http_requests_total[5m]))
      - record: instance:node_cpu:ratio
        expr: avg by (instance) (rate(node_cpu_seconds_total{mode!="idle"}[5m]))
        labels: { team: platform }
EOF
    run --separate-stderr slogen lint records.rules.yml
    assert_failure 1
    assert_output "records.rules.yml: http_requests_rate5m: record names are level:metric:operations
records.rules.yml: job:http_requests:rate5m: recorded twice with the same labels
lint: failed"
}

@test "the lint goes through every file and counts all their rules" {
    write a.rules.yml <<'EOF'
groups:
  - name: a
    rules:
      - record: job:up:sum
        expr: sum by (job) (up)
EOF
    write b.rules.yml <<'EOF'
groups:
  - name: b
    rules:
      - alert: Down
        expr: up == 0
        labels: { severity: ticket }
        annotations: { summary: down, runbook_url: https://runbooks.example.com/up }
EOF
    printf 'groups: [\n' > broken.rules.yml
    run --separate-stderr slogen lint a.rules.yml missing.rules.yml b.rules.yml broken.rules.yml
    assert_failure 1
    assert_output "missing.rules.yml: no such file
b.rules.yml: Down: no for
broken.rules.yml: not YAML
lint: failed"
    sed -i 's/labels:/for: 5m\n        labels:/' b.rules.yml
    run --separate-stderr slogen lint a.rules.yml b.rules.yml
    assert_success
    assert_output "lint: ok (2 rules)"
}

@test "lint needs at least one file" {
    run --separate-stderr slogen lint
    assert_failure 2
}

@test "prometheus.yml is valid and evaluates every 30 seconds" {
    [ -f "$repo/prometheus.yml" ] || { echo "todo: write prometheus.yml"; return 1; }
    run promtool check config --syntax-only "$repo/prometheus.yml"
    assert_success
    run yaml "$repo/prometheus.yml" '"\(.global.scrape_interval) \(.global.evaluation_interval)"'
    assert_output "30s 30s"
}

@test "prometheus.yml loads the generated rules" {
    run yaml "$repo/prometheus.yml" '.rule_files // [] | .[]'
    assert_success
    [ -n "$output" ] || { echo "todo: rule_files"; return 1; }
    globs=$output
    loaded=""
    while read -r glob; do
        loaded+=$(cd "$repo" && compgen -G "$glob" || true)$'\n'
    done <<< "$globs"
    grep -qx "rules/slos.rules.yml" <<< "$loaded" || { echo "rule_files do not load rules/slos.rules.yml: $globs"; return 1; }
    mkdir -p "$repo/rules"
    touch "$repo/rules/team.rules.yml"
    loaded=""
    while read -r glob; do
        loaded+=$(cd "$repo" && compgen -G "$glob" || true)$'\n'
    done <<< "$globs"
    grep -qx "rules/team.rules.yml" <<< "$loaded" || { echo "rule_files do not load other rules/*.rules.yml files: $globs"; return 1; }
}

@test "alerts go to the Alertmanager" {
    run yaml "$repo/prometheus.yml" '[.alerting.alertmanagers[].static_configs[].targets[]] | join(",")'
    assert_output "alertmanager:9093"
}

@test "the kubernetes-pods job discovers pods" {
    run yaml "$repo/prometheus.yml" '[.scrape_configs[] | .job_name] | join(",")'
    assert_output "kubernetes-pods"
    run yaml "$repo/prometheus.yml" '.scrape_configs[0].kubernetes_sd_configs | map(.role) | join(",")'
    assert_output "pod"
}

# relabel POD_JSON: the target's labels after the job's relabel_configs ("null": dropped),
# reduced to the labels a scrape keeps.
relabel() {
    local config
    config=$(yaml "$repo/prometheus.yml" '.scrape_configs[] | select(.job_name == "kubernetes-pods") | .relabel_configs // []' -c)
    jq -c -n --argjson config "$config" --argjson pod "$1" \
        '{config: $config, labels: ({job: "kubernetes-pods", __metrics_path__: "/metrics", __scheme__: "http"} + $pod)}' \
        | jq -c -f "$BATS_TEST_DIRNAME/relabel.jq" \
        | jq -c 'if . == null then null else with_entries(select(.key | startswith("__meta_") | not)) end'
}

@test "an annotated pod is scraped on its port and path, as its app" {
    run relabel '{"__address__": "10.0.0.5:80",
        "__meta_kubernetes_pod_annotation_prometheus_io_scrape": "true",
        "__meta_kubernetes_pod_annotation_prometheus_io_port": "8080",
        "__meta_kubernetes_pod_annotation_prometheus_io_path": "/internal/metrics",
        "__meta_kubernetes_pod_label_app": "checkout",
        "__meta_kubernetes_namespace": "shop",
        "__meta_kubernetes_pod_name": "checkout-7d9f"}'
    assert_success
    run jq -r '"\(.__address__) \(.__metrics_path__) \(.job) \(.namespace) \(.pod)"' <<< "$output"
    assert_output "10.0.0.5:8080 /internal/metrics checkout shop checkout-7d9f"
}

@test "without a port or a path annotation, the defaults stay" {
    run relabel '{"__address__": "10.0.0.7",
        "__meta_kubernetes_pod_annotation_prometheus_io_scrape": "true",
        "__meta_kubernetes_pod_label_app": "search",
        "__meta_kubernetes_namespace": "shop",
        "__meta_kubernetes_pod_name": "search-1"}'
    assert_success
    run jq -r '"\(.__address__) \(.__metrics_path__) \(.job)"' <<< "$output"
    assert_output "10.0.0.7 /metrics search"
}

@test "a port annotation is added to an address without one" {
    run relabel '{"__address__": "10.0.0.8",
        "__meta_kubernetes_pod_annotation_prometheus_io_scrape": "true",
        "__meta_kubernetes_pod_annotation_prometheus_io_port": "9100",
        "__meta_kubernetes_pod_label_app": "node"}'
    run jq -r '.__address__' <<< "$output"
    assert_output "10.0.0.8:9100"
}

@test "pods that do not ask to be scraped are dropped" {
    run relabel '{"__address__": "10.0.0.9:80", "__meta_kubernetes_pod_label_app": "batch"}'
    assert_output "null"
    run relabel '{"__address__": "10.0.0.9:80", "__meta_kubernetes_pod_annotation_prometheus_io_scrape": "false", "__meta_kubernetes_pod_label_app": "batch"}'
    assert_output "null"
}
