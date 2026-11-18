#!/usr/bin/env bash
# Usage: release.sh next|notes|tag REPO [OPTIONS] - releases from Conventional Commits.
set -euo pipefail
export LC_ALL=C
US=$'\x1f'
RS=$'\x1e'
SEMVER='^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
CONVENTIONAL='^([a-z]+)(\(([a-z0-9-]+)\))?(!)?: (.+)$'

usage() {
    echo "usage: release.sh next|notes|tag REPO [OPTIONS]" >&2
    exit 2
}

fail() {
    echo "release.sh: $*" >&2
    exit 1
}

# last_release: the highest vX.Y.Z tag reachable from HEAD, or nothing.
last_release() {
    git -C "$repo" tag --merged HEAD --list 'v*' |
        while IFS= read -r tag; do
            if [[ $tag =~ $SEMVER ]]; then echo "$tag"; fi
        done |
        sort -V | tail -n 1
}

# collect RANGE: one line per conventional commit of RANGE, oldest first, with the fields
# hash, type, scope, breaking (0 or 1), description and the breaking-change text, separated by
# the unit separator (0x1f). Merges and other subjects are skipped.
collect() {
    local hash subject body breaking text
    git -C "$repo" log --no-merges --reverse --format="%H$US%s$US%b$RS" "$1" |
        while IFS="$US" read -r -d "$RS" hash subject body; do
            hash=${hash#$'\n'}
            [[ $subject =~ $CONVENTIONAL ]] || continue
            breaking=0
            text=${BASH_REMATCH[5]}
            [ -n "${BASH_REMATCH[4]}" ] && breaking=1
            footer=$(grep -m 1 -E '^BREAKING[ -]CHANGE: ' <<< "$body" || true)
            if [ -n "$footer" ]; then
                breaking=1
                text=${footer#BREAKING?CHANGE: }
            fi
            printf '%s\n' "$hash$US${BASH_REMATCH[1]}$US${BASH_REMATCH[3]}$US$breaking$US${BASH_REMATCH[5]}$US$text"
        done
}

# next_version: sets $base (the last release, maybe empty) and $version (the next one), and
# saves the commits since $base in $tmp/commits. Fails when there is nothing to release.
next_version() {
    git -C "$repo" rev-parse --verify --quiet 'HEAD^{commit}' > /dev/null || fail "no commits"
    base=$(last_release)
    if [ -z "$base" ]; then
        collect HEAD > "$tmp/commits"
        version=v0.1.0
        return
    fi
    collect "$base..HEAD" > "$tmp/commits"
    local level
    level=$(awk -F "$US" '
        $4 == 1 { level = 3 }
        $2 == "feat" && level < 2 { level = 2 }
        ($2 == "fix" || $2 == "perf") && level < 1 { level = 1 }
        END { print level + 0 }' "$tmp/commits")
    [ "$level" -gt 0 ] || fail "nothing to release since $base"
    [[ $base =~ $SEMVER ]]
    local x=${BASH_REMATCH[1]} y=${BASH_REMATCH[2]} z=${BASH_REMATCH[3]}
    # Before 1.0.0 everything moves one place to the right.
    [ "$x" -eq 0 ] && [ "$level" -lt 3 ] && level=1
    [ "$x" -eq 0 ] && [ "$level" -eq 3 ] && level=2
    case "$level" in
        3) version="v$((x + 1)).0.0" ;;
        2) version="v$x.$((y + 1)).0" ;;
        1) version="v$x.$y.$((z + 1))" ;;
    esac
}

cmd_next() {
    next_version
    echo "$version"
}

[ "$#" -ge 2 ] || usage
command=$1
repo=$2
shift 2
case "$command" in
    next) [ "$#" -eq 0 ] || usage ;;
    *) usage ;;
esac
if [ ! -d "$repo" ] || ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "release.sh: not a git repository: $repo" >&2
    exit 2
fi
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
"cmd_$command" "$@"
