#!/usr/bin/env bash
# Usage: rollout.sh COMMAND FILE - a canary rollout controller.
#   validate FILE  checks the rollout spec
#   plan FILE      prints the steps it would take
#   run FILE       moves the traffic to the canary, step by step
set -euo pipefail

usage() {
    echo "usage: rollout.sh validate|plan|run FILE" >&2
    exit 2
}

die() {
    echo "rollout: $*" >&2
    exit 1
}

# Every problem of the spec, one line each ("field: problem").
read -r -d '' problems_of <<'JQ' || true
def k8s_name: type == "string" and length <= 63 and test("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");
def duration: type == "string" and test("^[0-9]+[smh]$");
def seconds: capture("^(?<n>[0-9]+)(?<u>[smh])$") | (.n | tonumber) * { s: 1, m: 60, h: 3600 }[.u];
def interval_ok: duration and seconds > 0;
def int_at_least($lo): type == "number" and . == floor and . >= $lo;
. as $root
| (["service", "namespace", "route", "stable", "canary", "deployment"][] as $field
    | if $root[$field] == null then "\($field): is required"
      elif ($root[$field] | k8s_name) then empty
      else "\($field): must be a Kubernetes name" end),
  (if (.steps | type) != "array" or (.steps | length) == 0 then "steps: at least one step is required"
   else
     (.steps | to_entries[] | .key as $i | .value as $step
       | (if ($step.weight | int_at_least(1)) and $step.weight <= 100 | not then "steps[\($i)].weight: must be an integer from 1 to 100"
          elif $i > 0 and ($root.steps[$i - 1].weight | type) == "number" and $step.weight <= $root.steps[$i - 1].weight
          then "steps[\($i)].weight: must be greater than the previous step"
          else empty end),
         (if $step.pause == null then empty
          elif ($step.pause | duration | not) then "steps[\($i)].pause: must be a duration like 30s, 5m or 1h"
          elif ($root.analysis.interval | interval_ok) and (($step.pause | seconds) % ($root.analysis.interval | seconds)) != 0
          then "steps[\($i)].pause: must be a multiple of analysis.interval"
          else empty end)),
     (if .steps[-1].weight != 100 then "steps: the last step must reach weight 100" else empty end)
   end),
  (if (.analysis | type) != "object" then "analysis: is required"
   else
     (if .analysis.interval | interval_ok then empty else "analysis.interval: must be a duration like 30s, 5m or 1h" end),
     (if (.analysis.query | type) == "string" and (.analysis.query | length) > 0 then empty else "analysis.query: is required" end),
     (if (.analysis.max | type) == "number" and .analysis.max >= 0 and .analysis.max <= 1 then empty
      else "analysis.max: must be a number from 0 to 1" end),
     (if .analysis.failure_limit | int_at_least(1) then empty else "analysis.failure_limit: must be an integer of at least 1" end),
     (if .analysis.inconclusive_limit == null or (.analysis.inconclusive_limit | int_at_least(1)) then empty
      else "analysis.inconclusive_limit: must be an integer of at least 1" end)
   end)
JQ

# The plan, one line per step, from a valid spec.
read -r -d '' plan_of <<'JQ' || true
def seconds: capture("^(?<n>[0-9]+)(?<u>[smh])$") | (.n | tonumber) * { s: 1, m: 60, h: 3600 }[.u];
(.analysis.interval | seconds) as $every
| (.steps | length) as $n
| "\(.service): \($n) steps, analysis every \($every)s (max error ratio \(.analysis.max), \(.analysis.failure_limit) failures roll back)",
  (.steps | to_entries[] | .key as $i | .value as $step
    | "step \($i + 1)/\($n): weight \($step.weight)"
      + (if $step.pause then
           ($step.pause | seconds) as $pause | ($pause / $every) as $checks
           | ", pause \($pause)s, \($checks) check\(if $checks == 1 then "" else "s" end)"
         else "" end))
JQ

# load FILE: the spec as JSON in $spec, or every problem on stderr and exit 1.
load() {
    local file=$1 problems
    [ -f "$file" ] || { echo "rollout: no such file: $file" >&2; exit 1; }
    spec=$(yq -o=json -I=0 '.' "$file" 2> /dev/null) || { echo "rollout: $file is not YAML" >&2; exit 1; }
    problems=$(jq -r "$problems_of" <<< "$spec")
    if [ -n "$problems" ]; then
        while IFS= read -r line; do
            echo "rollout: $line" >&2
        done <<< "$problems"
        exit 1
    fi
}

# field NAME: one top-level field of the spec.
field() {
    jq -r --arg name "$1" '.[$name]' <<< "$spec"
}

# seconds DURATION: 30s, 5m or 1h in seconds.
seconds() {
    local n=${1%[smh]}
    case "$1" in
        *s) echo "$n" ;;
        *m) echo $((n * 60)) ;;
        *h) echo $((n * 3600)) ;;
    esac
}

kube() {
    kubectl -n "$namespace" "$@"
}

# set_weight W: W% to the canary and the rest to stable, in the rule of the route that
# has both backends; the other rules (a header for testers, say) are left alone.
set_weight() {
    local weight=$1 route found rule stable_ref canary_ref patch
    route=$(kube get httproute "$route_name" -o json) || die "cannot read httproute $route_name"
    found=$(jq -r --arg s "$stable" --arg c "$canary" '
        [.spec.rules // [] | to_entries[] | .key as $rule | .value.backendRefs // [] | map(.name)
         | { rule: $rule, s: index($s), c: index($c) } | select(.s != null and .c != null)]
        | first // empty | "\(.rule) \(.s) \(.c)"' <<< "$route")
    [ -n "$found" ] || die "httproute $route_name has no rule with both $stable and $canary"
    read -r rule stable_ref canary_ref <<< "$found"
    patch=$(jq -cn --argjson rule "$rule" --argjson s "$stable_ref" --argjson c "$canary_ref" --argjson w "$weight" '[
        { op: "add", path: "/spec/rules/\($rule)/backendRefs/\($s)/weight", value: (100 - $w) },
        { op: "add", path: "/spec/rules/\($rule)/backendRefs/\($c)/weight", value: $w } ]')
    kube patch httproute "$route_name" --type=json -p "$patch" > /dev/null || die "cannot patch httproute $route_name"
}

# pause STEP: waits the step's pause, one analysis interval at a time.
pause() {
    local duration checks k
    duration=$(jq -r --argjson i "$1" '.steps[$i].pause // "0s"' <<< "$spec")
    checks=$(($(seconds "$duration") / interval))
    for ((k = 1; k <= checks; k++)); do
        sleep "$interval"
    done
}

run_rollout() {
    local steps i weight
    service=$(field service)
    namespace=$(field namespace)
    route_name=$(field route)
    stable=$(field stable)
    canary=$(field canary)
    deployment=$(field deployment)
    interval=$(seconds "$(jq -r '.analysis.interval' <<< "$spec")")
    steps=$(jq '.steps | length' <<< "$spec")

    kube scale deployment "$deployment" --replicas=1 > /dev/null || die "cannot scale deployment $deployment"
    for ((i = 0; i < steps; i++)); do
        weight=$(jq -r --argjson i "$i" '.steps[$i].weight' <<< "$spec")
        echo "step $((i + 1))/$steps: weight $weight"
        set_weight "$weight"
        pause "$i"
    done
    echo "rollout: $service promoted"
}

[ "$#" -eq 2 ] || usage
case "$1" in
    run)
        load "$2"
        run_rollout
        ;;
    validate)
        load "$2"
        echo "rollout: $2 is valid"
        ;;
    plan)
        load "$2"
        jq -r "$plan_of" <<< "$spec"
        ;;
    *) usage ;;
esac
