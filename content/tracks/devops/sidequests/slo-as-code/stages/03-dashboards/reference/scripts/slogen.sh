#!/usr/bin/env bash
# Usage: slogen.sh generate SLOS OUTDIR - Prometheus rules (and more) from an SLO file.
set -euo pipefail

usage() {
    echo "usage: slogen.sh generate SLOS OUTDIR" >&2
    exit 2
}

# The windows every SLI is recorded over: the burn-rate alerts need all of them.
windows='["5m","30m","1h","2h","6h","1d","3d"]'

# Every problem of the spec, one line each ("service/slo: problem").
read -r -d '' validate <<'JQ' || true
def name_ok: type == "string" and test("^[a-z][a-z0-9-]*$");
if (.services | type) != "array" or (.services | length) == 0 then "no services"
else
  .services | to_entries[] | .value as $s | ($s.name // "service \(.key + 1)" | tostring) as $sn
  | (if ($s.name | name_ok) then empty else "\($sn): the service name must match [a-z][a-z0-9-]*" end),
    (if ($s.job | type) == "string" and ($s.job | length) > 0 then empty else "\($sn): no job" end),
    (if ($s.runbook | type) == "string" and ($s.runbook | startswith("https://")) then empty else "\($sn): runbook must be an https:// URL" end),
    (if ($s.slos | type) == "array" and ($s.slos | length) > 0 then empty else "\($sn): no slos" end),
    ($s.slos // [] | to_entries[] | .value as $o | "\($sn)/\($o.name // "slo \(.key + 1)" | tostring)" as $on
      | (if ($o.name | name_ok) then empty else "\($on): the slo name must match [a-z][a-z0-9-]*" end),
        (if ($o.objective | type) == "number" and $o.objective > 0 and $o.objective < 100 then empty
         else "\($on): objective must be a number between 0 and 100" end),
        (if ["7d", "28d", "30d"] | index([$o.window]) then empty else "\($on): window must be 7d, 28d or 30d" end),
        (if ($o.sli.metric | type) == "string" and ($o.sli.metric | length) > 0 then empty else "\($on): sli.metric is missing" end),
        (if $o.sli.type == "availability" then empty
         elif $o.sli.type == "latency" then
           (if ($o.sli.threshold | type) == "number" and $o.sli.threshold > 0 then empty
            else "\($on): a latency sli needs a threshold in seconds" end)
         else "\($on): sli.type must be availability or latency" end))
end
JQ

# The rules, as JSON: one group per service.
read -r -d '' rules <<'JQ' || true
def selector($s): "job=\"\($s.job)\"";
def error_ratio($s; $o; $w):
  if $o.sli.type == "availability" then
    "sum(rate(\($o.sli.metric){\(selector($s)),code=~\"5..\"}[\($w)]))"
    + " / sum(rate(\($o.sli.metric){\(selector($s))}[\($w)]))"
  else
    "1 - sum(rate(\($o.sli.metric)_bucket{\(selector($s)),le=\"\($o.sli.threshold | tostring)\"}[\($w)]))"
    + " / sum(rate(\($o.sli.metric)_count{\(selector($s))}[\($w)]))"
  end;
# 1 - objective, computed so that 99.9 gives 0.001 and not 0.000999...
def budget($o): ((100 - $o.objective) * 1e6 | round) / 1e8;
def hours: { "1h": 1, "6h": 6, "1d": 24, "3d": 72, "7d": 168, "28d": 672, "30d": 720 }[.];
# The SRE workbook's alerts: page when 2% of the budget burns in an hour or 5% in six
# hours, open a ticket when 10% burns in a day or in three days. Each also needs a short
# window above the threshold, so an alert stops soon after the burning does.
def burns: [
  { severity: "page", long: "1h", short: "5m", spend: 0.02, for: "2m" },
  { severity: "page", long: "6h", short: "30m", spend: 0.05, for: "15m" },
  { severity: "ticket", long: "1d", short: "2h", spend: 0.1, for: "15m" },
  { severity: "ticket", long: "3d", short: "6h", spend: 0.1, for: "15m" } ];
# The burn rate that spends `spend` of the budget of the SLO's window in `long`.
def factor($o; $b): ($b.spend * ($o.window | hours) / ($b.long | hours) * 1e4 | round) / 1e4;
def series($s; $o; $w): "slo:sli_error:ratio_rate\($w){service=\"\($s.name)\",slo=\"\($o.name)\"}";
def alert($s; $o; $b): "(\(factor($o; $b)) * \(budget($o)))" as $threshold | {
  alert: "SLOErrorBudgetBurn",
  expr: "\(series($s; $o; $b.long)) > \($threshold)\nand\n\(series($s; $o; $b.short)) > \($threshold)\n",
  for: $b.for,
  labels: { service: $s.name, slo: $o.name, severity: $b.severity, long_window: $b.long },
  annotations: {
    summary: "{{ $labels.service }} is burning the {{ $labels.slo }} error budget",
    runbook_url: $s.runbook } };
{ groups: [ .services[] as $s | {
    name: "slo-\($s.name)",
    rules: [ $s.slos[] as $o | { service: $s.name, slo: $o.name } as $labels
      | (($windows + ([$o.window] - $windows))[] as $w | { record: "slo:sli_error:ratio_rate\($w)", expr: error_ratio($s; $o; $w), labels: $labels }),
        { record: "slo:error_budget:ratio", expr: "vector(\(budget($o)))", labels: $labels } ]
  }, {
    name: "slo-\($s.name)-alerts",
    rules: [ $s.slos[] as $o | burns[] as $b | alert($s; $o; $b) ]
  } ] }
JQ

# The Grafana dashboard of the service $name, on the recorded series only.
read -r -d '' dashboard <<'JQ' || true
{ type: "prometheus", uid: "${datasource}" } as $ds
| (.services[] | select(.name == $name)) as $s
| def target($ref; $legend; $expr): { refId: $ref, datasource: $ds, legendFormat: $legend, expr: $expr };
{
  uid: "slo-\($s.name)",
  title: "\($s.name) SLOs",
  tags: ["slo", "generated"],
  schemaVersion: 39,
  editable: false,
  time: { from: "now-7d", to: "now" },
  templating: { list: [ { name: "datasource", label: "Data source", type: "datasource", query: "prometheus" } ] },
  panels: [ $s.slos | to_entries[] | .key as $i | .value as $o
    | "{service=\"\($s.name)\",slo=\"\($o.name)\"}" as $sel
    | "slo:sli_error:ratio_rate\($o.window)\($sel)" as $ratio
    | "slo:error_budget:ratio\($sel)" as $budget
    | { id: ($i * 3 + 1), type: "stat", title: "\($o.name): SLI over \($o.window)",
        gridPos: { x: 0, y: ($i * 8), w: 6, h: 8 }, datasource: $ds,
        fieldConfig: { defaults: { unit: "percentunit", decimals: 3 } },
        targets: [ target("A"; "SLI"; "1 - \($ratio)") ] },
      { id: ($i * 3 + 2), type: "gauge", title: "\($o.name): error budget left",
        gridPos: { x: 6, y: ($i * 8), w: 6, h: 8 }, datasource: $ds,
        fieldConfig: { defaults: { unit: "percentunit", min: 0, max: 1 } },
        targets: [ target("A"; "budget left"; "1 - \($ratio) / \($budget)") ] },
      { id: ($i * 3 + 3), type: "timeseries", title: "\($o.name): burn rate",
        gridPos: { x: 12, y: ($i * 8), w: 12, h: 8 }, datasource: $ds,
        targets: [ target("A"; "1h"; "slo:sli_error:ratio_rate1h\($sel) / \($budget)"),
                   target("B"; "6h"; "slo:sli_error:ratio_rate6h\($sel) / \($budget)") ] } ]
}
JQ

[ "$#" -eq 3 ] && [ "$1" = generate ] || usage
file=$2
out=$3
[ -f "$file" ] || { echo "slogen: no such file: $file" >&2; exit 1; }
spec=$(yq -o=json -I=0 '.' "$file" 2> /dev/null) || { echo "slogen: $file is not YAML" >&2; exit 1; }

problems=$(jq -r "$validate" <<< "$spec")
if [ -n "$problems" ]; then
    sed 's/^/slogen: /' <<< "$problems" >&2
    exit 1
fi

mkdir -p "$out/rules"
json=$(jq -c --argjson windows "$windows" "$rules" <<< "$spec")
{
    echo "# Generated by scripts/slogen.sh from $(basename "$file"): do not edit."
    yq -P -o=yaml '.' <<< "$json"
} > "$out/rules/slos.rules.yml.tmp"
mv "$out/rules/slos.rules.yml.tmp" "$out/rules/slos.rules.yml"
echo "slogen: wrote rules/slos.rules.yml ($(jq '[.groups[].rules[]] | length' <<< "$json") rules)"

# One dashboard per service; a dashboard of a service that is gone is removed.
mkdir -p "$out/dashboards"
mapfile -t services < <(jq -r '.services[].name' <<< "$spec")
for old in "$out"/dashboards/*.json; do
    [ -e "$old" ] || continue
    name=$(basename "$old" .json)
    printf '%s\n' "${services[@]}" | grep -qxF -- "$name" || rm -f "$old"
done
for name in "${services[@]}"; do
    jq --indent 2 --arg name "$name" "$dashboard" <<< "$spec" > "$out/dashboards/$name.json.tmp"
    mv "$out/dashboards/$name.json.tmp" "$out/dashboards/$name.json"
    echo "slogen: wrote dashboards/$name.json"
done
