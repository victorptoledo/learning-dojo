#!/usr/bin/env bash
# The golden path: creates the skeleton of a new service with everything it needs to run in
# production from day one (CI/CD, SLO alerts, backup policy, runbook and catalog entry).
#
#   ./new-service.sh --name NAME --team TEAM --tier critical|standard [--out DIR]
#
# TODO (see README.md): validate the input before writing anything, never overwrite an
# existing service, fill the tier's values, and leave nothing behind when it fails.
set -euo pipefail

templates="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/templates"

name=""
team=""
tier=""
out="."
while [ $# -gt 0 ]; do
    case $1 in
        --name) name=$2 ;;
        --team) team=$2 ;;
        --tier) tier=$2 ;;
        --out) out=$2 ;;
    esac
    shift 2
done

target="$out/$name"

# render TEMPLATE DEST: fills the {{placeholders}}.
render() {
    sed -e "s|{{name}}|$name|g" \
        -e "s|{{team}}|$team|g" \
        -e "s|{{tier}}|$tier|g" \
        "$templates/$1" > "$2"
}

mkdir -p "$target/.github/workflows" "$target/slo" "$target/infra"
render ci.yml.tpl "$target/.github/workflows/ci.yml"
render slo.rules.yml.tpl "$target/slo/$name.rules.yml"
render RUNBOOK.md.tpl "$target/RUNBOOK.md"
render catalog-info.yaml.tpl "$target/catalog-info.yaml"
cp "$templates/infra/main.tf" "$target/infra/main.tf"
echo "created $target"
