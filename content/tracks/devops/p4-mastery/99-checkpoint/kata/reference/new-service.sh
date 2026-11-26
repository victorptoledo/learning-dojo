#!/usr/bin/env bash
# The golden path: creates the skeleton of a new service with everything it needs to run in
# production from day one (CI/CD, SLO alerts, backup policy, runbook and catalog entry).
#
#   ./new-service.sh --name NAME --team TEAM --tier critical|standard [--out DIR]
#
# Exit codes: 0 created, 1 the service already exists, 2 bad input. On any error nothing is
# left behind: the tree is built in a hidden folder next to the target and moved in place.
set -euo pipefail

templates="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/templates"

usage() {
    echo "usage: new-service.sh --name NAME --team TEAM --tier critical|standard [--out DIR]" >&2
    exit 2
}

fail() {
    echo "new-service: $1" >&2
    exit "${2:-2}"
}

name=""
team=""
tier=""
out="."
while [ $# -gt 0 ]; do
    case $1 in
        --name | --team | --tier | --out)
            [ $# -ge 2 ] || usage
            case $1 in
                --name) name=$2 ;;
                --team) team=$2 ;;
                --tier) tier=$2 ;;
                --out) out=$2 ;;
            esac
            shift 2
            ;;
        -h | --help) usage ;;
        *) fail "unknown option $1" ;;
    esac
done

# ---- validate everything before writing anything -------------------------------------
[[ $name =~ ^[a-z][a-z0-9-]{1,28}[a-z0-9]$ ]] \
    || fail "name must be 3 to 30 lowercase letters, digits and dashes, start with a letter and not end with a dash"
[[ $team =~ ^[a-z][a-z0-9-]*$ ]] || fail "team must be lowercase letters, digits and dashes"
case $tier in
    critical) slo=99.9 budget=0.001 rpo=15 rto=120 ;;
    standard) slo=99.5 budget=0.005 rpo=1440 rto=480 ;;
    *) fail "tier must be critical or standard" ;;
esac
[ -d "$out" ] || fail "the output folder $out does not exist"
target="$out/$name"
[ ! -e "$target" ] || fail "$target already exists; the golden path never overwrites a service" 1

# duration MINUTES: "15 minutes", "1 hour", "8 hours".
duration() {
    if (($1 % 60 != 0)); then
        echo "$1 minutes"
    elif (($1 == 60)); then
        echo "1 hour"
    else
        echo "$(($1 / 60)) hours"
    fi
}

# render TEMPLATE DEST: fills the {{placeholders}} (the inputs are validated, so they are
# safe in a sed replacement).
render() {
    sed -e "s|{{name}}|$name|g" \
        -e "s|{{team}}|$team|g" \
        -e "s|{{tier}}|$tier|g" \
        -e "s|{{slo}}|$slo|g" \
        -e "s|{{budget}}|$budget|g" \
        -e "s|{{rpo}}|$(duration "$rpo")|g" \
        -e "s|{{rto}}|$(duration "$rto")|g" \
        "$templates/$1" > "$2"
}

# ---- build the tree aside, then move it in place --------------------------------------
staging=$(mktemp -d "$out/.$name.XXXXXX")
trap 'rm -rf "$staging"' EXIT
chmod 755 "$staging"
mkdir -p "$staging/.github/workflows" "$staging/slo" "$staging/infra"

render ci.yml.tpl "$staging/.github/workflows/ci.yml"
render slo.rules.yml.tpl "$staging/slo/$name.rules.yml"
render RUNBOOK.md.tpl "$staging/RUNBOOK.md"
render catalog-info.yaml.tpl "$staging/catalog-info.yaml"
cp "$templates/infra/main.tf" "$staging/infra/main.tf"
printf 'service = "%s"\nteam    = "%s"\ntier    = "%s"\n' "$name" "$team" "$tier" > "$staging/infra/terraform.tfvars"

mv "$staging" "$target"
trap - EXIT
echo "created $target"
