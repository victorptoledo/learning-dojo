#!/usr/bin/env bash
# Usage: promote.sh STORE VERSION ENV [--allow-downgrade] - points the channel ENV
# (staging or production) at the verified release STORE/releases/VERSION, by its digest.
set -euo pipefail

usage() {
    echo "usage: promote.sh STORE VERSION staging|production [--allow-downgrade]" >&2
    exit 2
}

die() {
    echo "promote: $*"
    exit 1
}

allow_downgrade=0
args=()
for arg in "$@"; do
    case "$arg" in
        --allow-downgrade) allow_downgrade=1 ;;
        -*) usage ;;
        *) args+=("$arg") ;;
    esac
done
[ "${#args[@]}" -eq 3 ] || usage
store=${args[0]}
version=${args[1]}
env=${args[2]}
case "$env" in
    staging | production) ;;
    *) usage ;;
esac

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
release="$store/releases/$version"
channels="$store/channels"

[ -d "$release" ] || die "no release $version in $store"
if ! report=$(bash "$here/verify.sh" "$release"); then
    echo "$report"
    die "$version does not verify"
fi
[ "$(jq -r '.version' "$release/manifest.json")" = "$version" ] \
    || die "$version: its manifest is for $(jq -r '.version' "$release/manifest.json")"
digest="sha256:$(sha256sum "$release/manifest.json" | cut -d' ' -f1)"

# semver_cmp A B: prints -1, 0 or 1, with the precedence rules of semver.org.
semver_cmp() {
    local a=$1 b=$2 a_core b_core a_pre='' b_pre='' i
    a_core=${a%%-*}
    b_core=${b%%-*}
    [ "$a" = "$a_core" ] || a_pre=${a#*-}
    [ "$b" = "$b_core" ] || b_pre=${b#*-}
    local -a x y
    IFS=. read -r -a x <<< "$a_core"
    IFS=. read -r -a y <<< "$b_core"
    for i in 0 1 2; do
        if ((10#${x[i]} != 10#${y[i]})); then
            ((10#${x[i]} < 10#${y[i]})) && echo -1 || echo 1
            return
        fi
    done
    # A pre-release comes before the release itself.
    if [ -z "$a_pre" ] || [ -z "$b_pre" ]; then
        if [ -z "$a_pre" ] && [ -z "$b_pre" ]; then echo 0
        elif [ -z "$a_pre" ]; then echo 1
        else echo -1
        fi
        return
    fi
    IFS=. read -r -a x <<< "$a_pre"
    IFS=. read -r -a y <<< "$b_pre"
    for ((i = 0; i < ${#x[@]} || i < ${#y[@]}; i++)); do
        if [ "$i" -ge "${#x[@]}" ]; then echo -1; return; fi
        if [ "$i" -ge "${#y[@]}" ]; then echo 1; return; fi
        local p=${x[i]} q=${y[i]}
        [ "$p" = "$q" ] && continue
        if [[ $p =~ ^[0-9]+$ && $q =~ ^[0-9]+$ ]]; then
            ((10#$p < 10#$q)) && echo -1 || echo 1
        elif [[ $p =~ ^[0-9]+$ ]]; then echo -1
        elif [[ $q =~ ^[0-9]+$ ]]; then echo 1
        elif [[ $p < $q ]]; then echo -1
        else echo 1
        fi
        return
    done
    echo 0
}

if [ "$env" = production ]; then
    [[ $version != *-* ]] || die "pre-releases never reach production"
    [ -f "$channels/staging.json" ] && [ "$(jq -r '.version' "$channels/staging.json")" = "$version" ] \
        || die "$version is not in staging"
    [ "$(jq -r '.digest' "$channels/staging.json")" = "$digest" ] \
        || die "$version changed since it reached staging"
fi

previous=null
if [ -f "$channels/$env.json" ]; then
    current=$(jq -r '.version' "$channels/$env.json")
    if [ "$current" = "$version" ] && [ "$(jq -r '.digest' "$channels/$env.json")" = "$digest" ]; then
        echo "promote: $env already runs $version"
        exit 0
    fi
    if [ "$(semver_cmp "$current" "$version")" = 1 ] && [ "$allow_downgrade" -eq 0 ]; then
        die "$env runs $current, newer than $version (use --allow-downgrade)"
    fi
    previous=$(jq -c '.version' "$channels/$env.json")
fi

mkdir -p "$channels"
tmp=$(mktemp "$channels/.$env.XXXXXX")
jq -n --arg version "$version" --arg digest "$digest" --argjson previous "$previous" \
    '{version: $version, digest: $digest, previous: $previous}' > "$tmp"
mv "$tmp" "$channels/$env.json"
echo "$env $version $digest" >> "$channels/history.log"
echo "promote: $env now runs $version ($digest)"
