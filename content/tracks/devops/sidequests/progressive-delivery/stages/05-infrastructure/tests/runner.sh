# A tiny GitHub Actions runner (sourced after helpers.sh and fakes.sh): it runs the `run:`
# steps of a job in order in the repository copy, with the ${{ }} expressions filled in
# from a table the test gives, $RUNNER_TEMP, and $GITHUB_ENV carried from step to step.
# `uses:` steps are skipped (the tools are already in the image); so are steps with an
# `if:`, which run_step runs on purpose.

runner_temp="$BATS_TEST_TMPDIR/runner-temp"
github_env="$BATS_TEST_TMPDIR/github-env"
mkdir -p "$runner_temp"
: > "$github_env"

# steps FILE JOB TABLE: the run steps of JOB as JSON lines (name, if, run, env), with every
# ${{ }} replaced from TABLE (JSON).
steps() {
    wf "$1" '
        def resolve: gsub("\\$\\{\\{\\s*(?<e>.*?)\\s*\\}\\}"; ($table[.e] // ("<<unknown: " + .e + ">>")));
        (.env // {}) as $wenv
        | (.jobs[$job].env // {}) as $jenv
        | .jobs[$job].steps // [] | .[] | select(.run)
        | { name: (.name // .run), if: (.if // ""), run: (.run | resolve),
            env: ($wenv + $jenv + (.env // {}) | map_values(tostring | resolve)) }' \
        -c --arg job "$2" --argjson table "$3"
}

expressions() {
    local table='{}' pair
    for pair in "$@"; do
        table=$(jq -c --arg k "${pair%%=*}" --arg v "${pair#*=}" '. + {($k): $v}' <<< "$table")
    done
    echo "$table"
}

# one_step STEP_JSON: runs one step, as GitHub's bash does (-e, pipefail).
one_step() {
    local step=$1 name script status=0
    name=$(jq -r '.name' <<< "$step")
    if grep -q '<<unknown: ' <<< "$step"; then
        echo "step '$name': the test runner has no value for $(grep -o '<<unknown: [^>]*>>' <<< "$step" | head -1)"
        return 1
    fi
    script=$(jq -r '.run' <<< "$step")
    local env_args=() carried=()
    mapfile -t env_args < <(jq -r '.env | to_entries[] | "\(.key)=\(.value)"' <<< "$step")
    mapfile -t carried < "$github_env"
    echo "::step $name"
    (cd "$repo" && env "${carried[@]}" GITHUB_ENV="$github_env" RUNNER_TEMP="$runner_temp" \
        GITHUB_WORKSPACE="$repo" "${env_args[@]}" \
        timeout 120 bash --noprofile --norc -eo pipefail -c "$script" 3>&-) || status=$?
    [ "$status" -eq 0 ] || echo "::failed $name ($status)"
    return "$status"
}

# run_job FILE JOB EXPR=VALUE...: every run step of JOB without an `if:`, in order,
# stopping at the first that fails (with its status).
run_job() {
    local file=$1 job=$2 all step
    shift 2
    all=$(steps "$file" "$job" "$(expressions "$@")") || return 1
    [ -n "$all" ] || { echo "job $job has no run: step"; return 1; }
    while IFS= read -r step; do
        [ -z "$(jq -r '.if' <<< "$step")" ] || continue
        one_step "$step" || return $?
    done <<< "$all"
}

# run_step FILE JOB PATTERN EXPR=VALUE...: the first run step of JOB whose script matches
# PATTERN, whatever its `if:`.
run_step() {
    local file=$1 job=$2 pattern=$3 step
    shift 3
    step=$(steps "$file" "$job" "$(expressions "$@")" | jq -c --arg re "$pattern" 'select(.run | test($re))' | head -1)
    [ -n "$step" ] || { echo "job $job has no step that runs $pattern"; return 1; }
    one_step "$step"
}
