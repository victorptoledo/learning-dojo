# Helpers for DevOps kata tests: `load /opt/dojo/devops` in a .bats file.
# check.sh runs the tests from the kata copy ($KATA), so paths are relative to the kata.
bats_load_library bats-support
bats_load_library bats-assert

KATA=${KATA:-/target/kata}
cd "$KATA" || exit 1

# yaml FILE FILTER [jq args...]: a jq filter over a YAML file (read with yq), raw output.
# The workflow policies in policies.jq are available to the filter.
yaml() {
    local file=$1 filter=$2
    shift 2
    if [ ! -f "$file" ]; then
        echo "$file does not exist" >&2
        return 1
    fi
    yq eval -o=json -I=0 '.' "$file" | jq -L /opt/dojo -r "$@" "include \"policies\"; $filter"
}

# wf NAME FILTER [jq args...]: `yaml` over .github/workflows/NAME.
wf() {
    local name=$1
    shift
    yaml ".github/workflows/$name" "$@"
}

# policy NAME POLICY: runs a workflow policy from policies.jq over .github/workflows/NAME;
# prints the violations and fails when there is any.
policy() {
    local violations
    violations=$(wf "$1" "$2 | .[]") || return 1
    if [ -n "$violations" ]; then
        printf '%s\n' "$violations"
        return 1
    fi
}

# step_index NAME JOB PATTERN: the index of the first step of JOB whose `uses` or `run`
# matches the regular expression PATTERN, or -1.
step_index() {
    wf "$1" '[.jobs[$job].steps // [] | to_entries[] | select(((.value.uses // "") + "\n" + (.value.run // "")) | test($re)) | .key] | first // -1' \
        --arg job "$2" --arg re "$3"
}

# tf_outputs DIR: the module's declared outputs, one name per line (values are checked
# with `terraform test` files instead, since there is no apply).
tf_outputs() {
    grep -ho '^output "[^"]*"' "${1:-.}"/*.tf | sed 's/^output "\(.*\)"/\1/' | sort
}

# tf_variables DIR: the module's declared variables, one name per line.
tf_variables() {
    grep -ho '^variable "[^"]*"' "${1:-.}"/*.tf | sed 's/^variable "\(.*\)"/\1/' | sort
}

# rule FILE NAME FILTER: a jq filter over the rule (alert or record) called NAME.
rule() {
    local found
    found=$(yaml "$1" '[.groups[].rules[] | select(.alert == $name or .record == $name)] | first // empty' -c --arg name "$2")
    if [ -z "$found" ]; then
        echo "no rule named $2 in $1" >&2
        return 1
    fi
    jq -r "$3" <<< "$found"
}
