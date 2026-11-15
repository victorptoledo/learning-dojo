# Helpers for Kubernetes kata tests: `load /opt/dojo/k8s` in a .bats file.
# check.sh rendered every target to $RENDERED/<target>.yaml and, as one JSON array of
# objects, to $RENDERED/<target>.json. The helpers read the JSON with jq.
bats_load_library bats-support
bats_load_library bats-assert

RENDERED=${RENDERED:-/target/rendered}

# k8s TARGET FILTER [jq args...]: runs a jq filter over the array of objects of TARGET.
k8s() {
    local target=$1 filter=$2
    shift 2
    jq -L /opt/dojo -r "$@" "include \"policies\"; $filter" "$RENDERED/$target.json"
}

# obj TARGET KIND NAME: the object as JSON; fails when the target has no such object.
obj() {
    local found
    found=$(k8s "$1" '.[] | select(.kind == $kind and .metadata.name == $name)' \
        --arg kind "$2" --arg name "$3" -c)
    if [ -z "$found" ]; then
        echo "no $2 named $3 in the $1 manifests" >&2
        return 1
    fi
    printf '%s\n' "$found"
}

# get TARGET KIND NAME FILTER [jq args...]: a jq filter applied to one object, raw output.
get() {
    local object filter=$4
    object=$(obj "$1" "$2" "$3") || return 1
    shift 4
    jq -L /opt/dojo -r "$@" "include \"policies\"; $filter" <<< "$object"
}

# names TARGET KIND: the names of every object of KIND, one per line, sorted.
names() {
    k8s "$1" '[.[] | select(.kind == $kind) | .metadata.name] | sort | .[]' --arg kind "$2"
}

# count TARGET KIND: how many objects of KIND the target has.
count() {
    k8s "$1" '[.[] | select(.kind == $kind)] | length' --arg kind "$2"
}

# policy TARGET NAME: runs a policy from policies.jq; prints the violations and fails
# when there is any.
policy() {
    local violations
    violations=$(k8s "$1" "$2 | .[]") || return 1
    if [ -n "$violations" ]; then
        printf '%s\n' "$violations"
        return 1
    fi
}
