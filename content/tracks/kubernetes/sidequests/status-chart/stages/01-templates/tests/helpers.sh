# Shared by the tests of every stage: `source "$BATS_TEST_DIRNAME/helpers.sh"` after
# `load /opt/dojo/k8s`. check.sh renders the targets of tests/render.txt; other value sets
# are rendered here, into the same folder, so the k8s helpers read them too.

KATA_DIR="$BATS_TEST_DIRNAME/.."

# helm_render NAME ARGS...: `helm template ARGS...` (run in the kata folder) as the target
# NAME. A render that fails leaves an empty target and its error in $RENDERED/NAME.err.
helm_render() {
    local name=$1
    shift
    rm -f "$RENDERED/$name.err"
    if ! (cd "$KATA_DIR" && helm template "$@") > "$RENDERED/$name.yaml" 2> "$RENDERED/$name.log"; then
        { echo "\`helm template $*\` failed:"; cat "$RENDERED/$name.log"; } > "$RENDERED/$name.err"
        : > "$RENDERED/$name.yaml"
    fi
    yq eval-all -o=json -I=0 '[. | select(. != null and . != "")]' "$RENDERED/$name.yaml" \
        > "$RENDERED/$name.json" 2> /dev/null || echo '[]' > "$RENDERED/$name.json"
}

# valid NAME: the target rendered, has objects, and every object passes kubeconform.
valid() {
    local name=$1
    if [ -f "$RENDERED/$name.err" ]; then
        cat "$RENDERED/$name.err"
        return 1
    fi
    if [ "$(jq length "$RENDERED/$name.json")" -eq 0 ]; then
        echo "the $name render has no Kubernetes objects"
        return 1
    fi
    kubeconform -strict -summary -output text \
        -kubernetes-version "$(cat /opt/dojo/schemas/kubernetes-version)" \
        -schema-location '/opt/dojo/schemas/k8s/{{.ResourceKind}}{{.KindSuffix}}.json' \
        "$RENDERED/$name.yaml"
}

# rejects ARGS...: `helm template ARGS...` must fail; prints its error for the message check.
rejects() {
    if (cd "$KATA_DIR" && helm template "$@") > /dev/null 2> "$BATS_TEST_TMPDIR/rejects.log"; then
        echo "\`helm template $*\` rendered, but these values must be rejected"
        return 1
    fi
    cat "$BATS_TEST_TMPDIR/rejects.log"
}

# container TARGET NAME FILTER: a jq filter on the statuspage container of the Deployment NAME.
container() {
    get "$1" Deployment "$2" ".spec.template.spec.containers[] | select(.name == \"statuspage\") | $3"
}
