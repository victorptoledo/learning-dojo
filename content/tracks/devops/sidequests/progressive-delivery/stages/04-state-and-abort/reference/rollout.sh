#!/usr/bin/env bash
# Usage: rollout.sh COMMAND FILE - a canary rollout controller.
#   validate FILE  checks the rollout spec
#   plan FILE      prints the steps it would take
#   run [--restart] FILE  moves the traffic to the canary, step by step, rolls back when
#                  the analysis says the canary is bad, and resumes after a crash
#   abort FILE     rolls a running (or crashed) rollout back
#   status FILE    where the rollout of the service is
set -euo pipefail

usage() {
    echo "usage: rollout.sh validate|plan|run [--restart]|abort|status FILE" >&2
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

# measure: asks promq for the canary's error ratio and prints it. Returns 0 when it is
# within the maximum, 1 when it is above, 2 when there is no usable answer.
measure() {
    local value
    value=$(promq "$query" 2> /dev/null) || return 2
    value=$(tr -d '[:space:]' <<< "$value")
    [[ $value =~ ^[0-9]+(\.[0-9]+)?([eE][-+]?[0-9]+)?$ ]] || return 2
    echo "$value"
    if awk -v v="$value" -v m="$max" 'BEGIN { exit !(v + 0 > m + 0) }'; then
        return 1
    fi
}

# ---- state: where a rollout is, in $ROLLOUT_STATE_DIR (default .rollout) ----------------

# save STATUS STEP [REASON]: the state file, replaced whole.
save() {
    local tmp
    tmp=$(mktemp "$state_dir/.$service.XXXXXX")
    jq -n --arg status "$1" --argjson step "$2" --argjson steps "$steps" --arg reason "${3:-}" \
        --argjson weight "$(jq --argjson i "$(($2 - 1))" '.steps[$i].weight' <<< "$spec")" \
        '{status: $status, step: $step, steps: $steps, weight: $weight, reason: $reason}' > "$tmp"
    mv "$tmp" "$state_file"
}

# state FIELD: one field of the state file (empty without one).
state() {
    [ -f "$state_file" ] || return 0
    jq -r --arg f "$1" '.[$f] // empty' "$state_file"
}

# held: whether a run of this service holds the lock now.
held() {
    ! flock -n "$lock_file" true
}

# rollback STEP REASON [STATUS]: all the traffic back to stable, the canary scaled to zero.
rollback() {
    set_weight 0
    kube scale deployment "$deployment" --replicas=0 > /dev/null || die "cannot scale deployment $deployment"
    if [ "${3:-rolled-back}" = aborted ]; then
        save aborted "$(($1 + 1))" "$2"
        echo "rollout: $service aborted at step $(($1 + 1))"
    else
        save rolled-back "$(($1 + 1))" "$2"
        echo "rollout: $service rolled back at step $(($1 + 1)): $2"
    fi
}

# pause STEP: waits the step's pause, one analysis interval at a time, measuring the
# canary after each. Failed and inconclusive checks are counted in a row, over the whole
# rollout: a good check resets both counts, a failed one resets the inconclusive count.
pause() {
    local duration checks k value status
    duration=$(jq -r --argjson i "$1" '.steps[$i].pause // "0s"' <<< "$spec")
    checks=$(($(seconds "$duration") / interval))
    for ((k = 1; k <= checks; k++)); do
        sleep "$interval"
        if [ -e "$abort_file" ]; then
            rm -f "$abort_file"
            rollback "$1" "aborted" aborted
            exit 3
        fi
        status=0
        value=$(measure) || status=$?
        case "$status" in
            0)
                echo "  check $k/$checks: $value ok"
                failures=0
                inconclusive=0
                ;;
            1)
                echo "  check $k/$checks: $value failed"
                failures=$((failures + 1))
                inconclusive=0
                if [ "$failures" -ge "$failure_limit" ]; then
                    rollback "$1" "error ratio $value above $max"
                    exit 3
                fi
                ;;
            *)
                echo "  check $k/$checks: no data"
                inconclusive=$((inconclusive + 1))
                if [ "$inconclusive" -ge "$inconclusive_limit" ]; then
                    rollback "$1" "no data from the analysis"
                    exit 3
                fi
                ;;
        esac
    done
}

# setup: the fields of the spec and the state files of its service.
setup() {
    service=$(field service)
    namespace=$(field namespace)
    route_name=$(field route)
    stable=$(field stable)
    canary=$(field canary)
    deployment=$(field deployment)
    interval=$(seconds "$(jq -r '.analysis.interval' <<< "$spec")")
    steps=$(jq '.steps | length' <<< "$spec")
    query=$(jq -r '.analysis.query' <<< "$spec")
    max=$(jq -r '.analysis.max' <<< "$spec")
    failure_limit=$(jq -r '.analysis.failure_limit' <<< "$spec")
    inconclusive_limit=$(jq -r '.analysis.inconclusive_limit // 3' <<< "$spec")
    state_dir=${ROLLOUT_STATE_DIR:-.rollout}
    mkdir -p "$state_dir"
    state_file="$state_dir/$service.state"
    lock_file="$state_dir/$service.lock"
    abort_file="$state_dir/$service.abort"
}

run_rollout() {
    local restart=$1 start=0 i weight status
    # One rollout per service at a time: the lock lives as long as this process.
    exec 9> "$lock_file"
    flock -n 9 || { echo "rollout: $service is already running" >&2; exit 4; }
    rm -f "$abort_file"

    status=$(state status)
    if [ "$restart" -eq 0 ] && [ -n "$status" ]; then
        if [ "$status" = running ]; then
            start=$(($(state step) - 1))
            echo "rollout: resuming $service at step $((start + 1))/$steps"
        else
            echo "rollout: $service is already ${status/-/ }; use run --restart to start again"
            exit 0
        fi
    fi

    failures=0
    inconclusive=0
    kube scale deployment "$deployment" --replicas=1 > /dev/null || die "cannot scale deployment $deployment"
    for ((i = start; i < steps; i++)); do
        weight=$(jq -r --argjson i "$i" '.steps[$i].weight' <<< "$spec")
        save running "$((i + 1))"
        echo "step $((i + 1))/$steps: weight $weight"
        set_weight "$weight"
        pause "$i"
    done
    save promoted "$steps"
    echo "rollout: $service promoted"
}

abort_rollout() {
    if held; then
        : > "$abort_file"
        echo "rollout: abort requested for $service"
        return
    fi
    exec 9> "$lock_file"
    flock -n 9 || { echo "rollout: $service is already running" >&2; exit 4; }
    if [ "$(state status)" != running ]; then
        echo "rollout: $service is not running" >&2
        exit 1
    fi
    # The run that was here died: undo what it did.
    rollback "$(($(state step) - 1))" "aborted" aborted
}

show_status() {
    case "$(state status)" in
        "") echo "$service: not started" ;;
        running)
            if held; then
                echo "$service: running, step $(state step)/$(state steps) (weight $(state weight))"
            else
                echo "$service: interrupted at step $(state step)/$(state steps) (weight $(state weight))"
            fi
            ;;
        promoted) echo "$service: promoted" ;;
        rolled-back) echo "$service: rolled back at step $(state step) ($(state reason))" ;;
        aborted) echo "$service: aborted at step $(state step)" ;;
    esac
}

command=${1:-}
restart=0
if [ "$command" = run ] && [ "${2:-}" = --restart ]; then
    restart=1
    set -- "$1" "${@:3}"
fi
[ "$#" -eq 2 ] || usage
case "$command" in
    run | abort | status)
        load "$2"
        setup
        case "$command" in
            run) run_rollout "$restart" ;;
            abort) abort_rollout ;;
            status) show_status ;;
        esac
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
