# Shared by the tests of every stage: `source "$BATS_TEST_DIRNAME/helpers.sh"` after
# `load /opt/dojo/k8s`. check.sh renders only the targets of tests/render.txt (the base);
# the overlays are rendered here, into the same folder, so the k8s helpers read them too.

KATA_DIR="$BATS_TEST_DIRNAME/.."

# render NAME DIR: `kustomize build DIR` as the target NAME ($RENDERED/NAME.yaml and .json).
# A build that fails leaves an empty target and its error in $RENDERED/NAME.err.
render() {
    local name=$1 dir=$2
    rm -f "$RENDERED/$name.err"
    if ! (cd "$KATA_DIR" && kustomize build "$dir") > "$RENDERED/$name.yaml" 2> "$RENDERED/$name.log"; then
        { echo "\`kustomize build $dir\` failed:"; cat "$RENDERED/$name.log"; } > "$RENDERED/$name.err"
        : > "$RENDERED/$name.yaml"
    fi
    yq eval-all -o=json -I=0 '[. | select(. != null and . != "")]' "$RENDERED/$name.yaml" \
        > "$RENDERED/$name.json" 2> /dev/null || echo '[]' > "$RENDERED/$name.json"
}

# valid NAME: the target built, has objects, and every object passes kubeconform.
valid() {
    local name=$1
    if [ -f "$RENDERED/$name.err" ]; then
        cat "$RENDERED/$name.err"
        return 1
    fi
    if [ "$(jq length "$RENDERED/$name.json")" -eq 0 ]; then
        echo "the $name build has no Kubernetes objects"
        return 1
    fi
    kubeconform -strict -summary -output text \
        -kubernetes-version "$(cat /opt/dojo/schemas/kubernetes-version)" \
        -schema-location '/opt/dojo/schemas/k8s/{{.ResourceKind}}{{.KindSuffix}}.json' \
        "$RENDERED/$name.yaml"
}

# generated TARGET KIND PREFIX: the one name of KIND that is PREFIX plus a Kustomize hash.
generated() {
    k8s "$1" '[.[] | select(.kind == $kind) | .metadata.name | select(test("^" + $prefix + "-[a-z0-9]{10}$"))] | if length == 1 then .[0] else "missing" end' \
        --arg kind "$2" --arg prefix "$3"
}

# env_of TARGET NAME: the env and envFrom of the invoices container, one "NAME=source" per line.
env_of() {
    get "$1" Deployment invoices '.spec.template.spec.containers[] | select(.name == "invoices")
      | ((.env // [])[] | "\(.name)=\(.value // (.valueFrom | tojson))"),
        ((.envFrom // [])[] | "envFrom=\(.configMapRef.name // .secretRef.name)")'
}
