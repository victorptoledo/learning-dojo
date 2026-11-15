#!/bin/bash
# Checks a Kubernetes kata in /work, offline and with no cluster:
#   1. renders every target named in tests/render.txt ("name: command", one per line;
#      without the file, every .yaml file at the top of the kata is one target "manifests"),
#   2. validates each rendered target with kubeconform against the baked-in schemas,
#   3. runs the visible and grading bats tests (staged in /work/tests) over the rendered
#      objects, which they read with the helpers in /opt/dojo/k8s.bash.
# Prints the result in `cargo test` format.
set -u

out=/target
rm -rf "${out:?}"/*
mkdir -p "$out/rendered"

# Everything runs on a copy. Windows editors may save CRLF line ends, which YAML block
# scalars, Helm templates and bash would all keep as part of the text.
cp -r /work "$out/kata"
find "$out/kata" -type f \( -name '*.yaml' -o -name '*.yml' -o -name '*.tpl' -o -name '*.txt' \
    -o -name '*.bats' -o -name '*.json' -o -name '*.properties' -o -name '*.env' \) \
    -exec sed -i 's/\r$//' {} +
cd "$out/kata" || exit 1

# `manifests DIR`: the .yaml files at the top of DIR, in name order, as one YAML stream.
manifests() {
    local dir=${1:-.} file found=0
    for file in "$dir"/*.yaml; do
        [ -f "$file" ] || continue
        found=1
        printf -- '---\n# Source: %s\n' "${file#./}"
        yq eval '.' "$file" || return 1
    done
    [ "$found" -eq 1 ] || { echo "no .yaml files in $dir" >&2; return 1; }
}
export -f manifests

if [ -f tests/render.txt ]; then
    targets=$(grep -v -e '^[[:space:]]*#' -e '^[[:space:]]*$' tests/render.txt)
else
    targets="manifests: manifests ."
fi

# A target that does not render (bad YAML, a broken overlay, a template error) is the
# manifest's "does not compile".
names=()
while IFS= read -r line; do
    name=${line%%:*}
    command=${line#*:}
    command=${command# }
    names+=("$name")
    if ! bash -c "$command" > "$out/rendered/$name.yaml" 2> "$out/render.log"; then
        echo "error: \`$command\` failed:"
        cat "$out/render.log"
        exit 1
    fi
    # The same objects as one JSON array, for the tests and the policies.
    if ! yq eval-all -o=json -I=0 '[. | select(. != null and . != "")]' \
        "$out/rendered/$name.yaml" > "$out/rendered/$name.json" 2> "$out/render.log"; then
        echo "error: the output of \`$command\` is not valid YAML:"
        cat "$out/render.log"
        exit 1
    fi
done <<< "$targets"

version=$(cat /opt/dojo/schemas/kubernetes-version)
: > "$out/kubeconform.log"
for name in "${names[@]}"; do
    count=$(jq 'length' "$out/rendered/$name.json")
    if [ "$count" -eq 0 ]; then
        { echo "KC fail $name"; echo "KC msg no Kubernetes objects were rendered"; } >> "$out/kubeconform.log"
        continue
    fi
    if kubeconform -strict -summary -output text \
        -kubernetes-version "$version" \
        -schema-location '/opt/dojo/schemas/k8s/{{.ResourceKind}}{{.KindSuffix}}.json' \
        -schema-location '/opt/dojo/schemas/crds/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json' \
        "$out/rendered/$name.yaml" > "$out/kc.one" 2>&1; then
        echo "KC ok $name" >> "$out/kubeconform.log"
    else
        echo "KC fail $name" >> "$out/kubeconform.log"
        sed -e "s#$out/rendered/##" -e 's/^/KC msg /' "$out/kc.one" >> "$out/kubeconform.log"
    fi
done

RENDERED="$out/rendered" bats --tap --recursive tests > "$out/bats.tap" 2>&1

awk -f /opt/dojo/tap2cargo.awk "$out/bats.tap" "$out/kubeconform.log"
status=$?
if [ "$status" -eq 2 ]; then
    echo "error: no tests ran"
    head -40 "$out/bats.tap"
fi
exit "$status"
