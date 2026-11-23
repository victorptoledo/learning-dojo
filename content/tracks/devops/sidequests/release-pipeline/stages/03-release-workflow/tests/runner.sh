# A tiny GitHub Actions runner for the tests of stage 3 and later: it runs the `run:` steps
# of one job in order, in the repository copy, with the ${{ }} expressions filled in from
# a table the test gives, and a fake `gh` and `git` first in PATH. Sourced after helpers.sh.

fakebin="$BATS_TEST_TMPDIR/fakebin"
GH_LOG="$BATS_TEST_TMPDIR/gh.log"
GIT_LOG="$BATS_TEST_TMPDIR/git.log"
FAKE_ARTIFACTS="$BATS_TEST_TMPDIR/artifacts"
FAKE_RELEASES="$BATS_TEST_TMPDIR/releases"
FAKE_COMMIT_TIME=1700000123
mkdir -p "$fakebin" "$FAKE_ARTIFACTS" "$FAKE_RELEASES"
: > "$GH_LOG"
: > "$GIT_LOG"
export GH_LOG GIT_LOG FAKE_ARTIFACTS FAKE_RELEASES FAKE_COMMIT_TIME

# gh: records every call as a JSON array of its arguments; `run download` and
# `release download` copy what the test prepared; `release create` checks its files.
cat > "$fakebin/gh" <<'GH'
#!/usr/bin/env bash
jq -cn '$ARGS.positional' --args -- "$@" >> "$GH_LOG"
dir=.
pattern='*'
args=("$@")
for ((i = 0; i < ${#args[@]}; i++)); do
    case "${args[i]}" in
        --dir | -D) dir=${args[i + 1]} ;;
        --pattern | -p) pattern=${args[i + 1]} ;;
    esac
done
case "$1 $2" in
    "run download")
        found=0
        for artifact in "$FAKE_ARTIFACTS"/*/; do
            name=$(basename "$artifact")
            # shellcheck disable=SC2053
            [[ $name == $pattern ]] || continue
            mkdir -p "$dir/$name"
            cp -r "$artifact." "$dir/$name/"
            found=1
        done
        [ "$found" -eq 1 ] || { echo "gh: no artifact matches $pattern" >&2; exit 1; }
        ;;
    "release download")
        if [ ! -d "$FAKE_RELEASES/$3" ]; then
            echo "release not found" >&2
            exit 1
        fi
        mkdir -p "$dir"
        cp -r "$FAKE_RELEASES/$3/." "$dir/"
        ;;
    "release create")
        for arg in "${@:4}"; do
            case "$arg" in
                -*) break ;;
                *) [ -f "$arg" ] || { echo "gh: no such file: $arg" >&2; exit 1; } ;;
            esac
        done
        ;;
esac
exit 0
GH

# git: records every call; answers `log -1 --format=%ct`, `rev-parse HEAD` and `diff`.
cat > "$fakebin/git" <<'GIT'
#!/usr/bin/env bash
jq -cn '$ARGS.positional' --args -- "$@" >> "$GIT_LOG"
case "$*" in
    "log -1 --format=%ct"*) echo "$FAKE_COMMIT_TIME" ;;
    "rev-parse HEAD") echo "4f2a9c1e0b7d4f2a9c1e0b7d4f2a9c1e0b7d4f2a" ;;
    # Something is staged, unless the test says the tree is clean.
    "diff "*) exit "${FAKE_GIT_DIFF_STATUS:-1}" ;;
esac
exit 0
GIT
chmod +x "$fakebin/gh" "$fakebin/git"

# expressions EXPR=VALUE...: the table of ${{ }} values, as JSON.
expressions() {
    local table='{}' pair
    for pair in "$@"; do
        table=$(jq -c --arg k "${pair%%=*}" --arg v "${pair#*=}" '. + {($k): $v}' <<< "$table")
    done
    echo "$table"
}

# steps FILE JOB TABLE: one JSON object per `run:` step of JOB (name, run, env), with the
# workflow, job and step env merged and every ${{ }} replaced from TABLE.
steps() {
    wf "$1" '
        def resolve: gsub("\\$\\{\\{\\s*(?<e>.*?)\\s*\\}\\}"; ($table[.e] // ("<<unknown: " + .e + ">>")));
        (.env // {}) as $wenv
        | (.jobs[$job].env // {}) as $jenv
        | .jobs[$job].steps // [] | .[] | select(.run)
        | { name: (.name // .run), run: (.run | resolve),
            env: ($wenv + $jenv + (.env // {}) | map_values(tostring | resolve)) }' \
        -c --arg job "$2" --argjson table "$3"
}

# run_job FILE JOB EXPR=VALUE...: runs the `run:` steps of JOB in order, as GitHub's
# default bash does (-e, pipefail), and stops at the first one that fails.
run_job() {
    local file=$1 job=$2 table step name script status=0
    shift 2
    table=$(expressions "$@")
    local all
    all=$(steps "$file" "$job" "$table") || return 1
    [ -n "$all" ] || { echo "job $job has no run: step"; return 1; }
    while IFS= read -r step; do
        name=$(jq -r '.name' <<< "$step")
        if grep -q '<<unknown: ' <<< "$step"; then
            echo "step '$name': the test runner has no value for $(grep -o '<<unknown: [^>]*>>' <<< "$step" | head -1)"
            return 1
        fi
        script=$(jq -r '.run' <<< "$step")
        local env_args=()
        mapfile -t env_args < <(jq -r '.env | to_entries[] | "\(.key)=\(.value)"' <<< "$step")
        echo "::step $name"
        (cd "$repo" && env PATH="$fakebin:$PATH" GITHUB_RUN_ID=4242 GITHUB_REPOSITORY=acme/greet \
            GITHUB_WORKSPACE="$repo" "${env_args[@]}" \
            timeout 60 bash --noprofile --norc -eo pipefail -c "$script" 3>&-) || status=$?
        if [ "$status" -ne 0 ]; then
            echo "::failed $name ($status)"
            return "$status"
        fi
    done <<< "$all"
}

# upload FILE JOB EXPR=VALUE...: what the job's actions/upload-artifact step would keep:
# the files under its `path`, in $FAKE_ARTIFACTS/<its name>/.
upload() {
    local file=$1 job=$2 table spec name path
    shift 2
    table=$(expressions "$@")
    spec=$(wf "$file" '
        def resolve: gsub("\\$\\{\\{\\s*(?<e>.*?)\\s*\\}\\}"; ($table[.e] // ("<<unknown: " + .e + ">>")));
        [.jobs[$job].steps[] | select((.uses // "") | startswith("actions/upload-artifact@"))] | first // empty
        | "\(.with.name | resolve)\t\(.with.path | resolve)"' --arg job "$job" --argjson table "$table")
    [ -n "$spec" ] || { echo "job $job uploads no artifact"; return 1; }
    name=${spec%%$'\t'*}
    path=${spec#*$'\t'}
    path=${path%/}
    mkdir -p "$FAKE_ARTIFACTS/$name"
    cp -r "$repo/$path/." "$FAKE_ARTIFACTS/$name/"
}

# gh_calls VERB: the recorded `gh` calls starting with VERB (for example "release create").
gh_calls() {
    jq -c --arg verb "$1" 'select((.[0:($verb | split(" ") | length)] | join(" ")) == $verb)' "$GH_LOG"
}
