#!/usr/bin/env bash
# Usage: scrub.sh big|secrets|purge|redact|undo|expire REPO [ARGS] - rewrites history safely.
set -euo pipefail
export LC_ALL=C
TAB=$'\t'

usage() {
    echo "usage: scrub.sh big|secrets|purge|redact|undo|expire REPO [ARGS]" >&2
    exit 2
}

fail() {
    echo "scrub.sh: $*" >&2
    exit 1
}

has_commits() {
    git -C "$repo" rev-parse --verify --quiet 'HEAD^{commit}' > /dev/null ||
        [ -n "$(git -C "$repo" for-each-ref --count=1 refs/heads refs/tags)" ]
}

# versions: every (blob, path) pair ever committed on a branch or tag, as "BLOB<TAB>PATH<TAB>WHERE"
# lines, where WHERE says whether that blob is at that path in HEAD.
versions() {
    {
        if git -C "$repo" rev-parse --verify --quiet 'HEAD^{commit}' > /dev/null; then
            git -C "$repo" ls-tree -r -z HEAD |
                awk -v RS='\0' '{ split($0, f, "\t"); split(f[1], m, " "); printf "H\t%s\t%s\n", m[3], f[2] }'
        fi
        # With -z, each change is ":modes ids status" then the path, each ending in NUL.
        git -C "$repo" log --branches --tags --raw -z --no-abbrev --no-renames -m --format= |
            awk -v RS='\0' '
                { sub(/^\n+/, "") }
                /^:/ { split($0, f, " "); blob = f[4]; status = f[5]; next }
                blob != "" {
                    if (status != "D") printf "V\t%s\t%s\n", blob, $0
                    blob = ""
                }'
    } | awk -F '\t' '
        $1 == "H" { head[$2 "\t" $3] = 1; next }
        !(($2 "\t" $3) in seen) {
            seen[$2 "\t" $3] = 1
            printf "%s\t%s\t%s\n", $2, $3, (($2 "\t" $3) in head) ? "HEAD" : "history"
        }'
}

# big [--min-size BYTES]: versions of files of at least BYTES bytes.
cmd_big() {
    local min=1048576
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --min-size) [ "$#" -ge 2 ] && [[ $2 =~ ^[0-9]+$ ]] || usage; min=$2; shift 2 ;;
            *) usage ;;
        esac
    done
    has_commits || return 0
    versions > "$tmp/versions"
    cut -f 1 "$tmp/versions" | sort -u |
        git -C "$repo" cat-file --batch-check='%(objectname) %(objectsize)' > "$tmp/sizes"
    awk -F '\t' -v min="$min" '
        FILENAME == ARGV[1] { split($0, f, " "); size[f[1]] = f[2]; next }
        size[$1] >= min { printf "%d\t%s\t%s\n", size[$1], $2, $3 }' \
        "$tmp/sizes" "$tmp/versions" |
        sort -t "$TAB" -k1,1nr -k2,2 -k3,3
}

# secrets: the kinds of secrets in each version, exit 1 when any is found.
cmd_secrets() {
    [ "$#" -eq 0 ] || usage
    has_commits || return 0
    versions > "$tmp/versions"
    local blob kinds
    : > "$tmp/found"
    while IFS= read -r blob; do
        git -C "$repo" cat-file blob "$blob" > "$tmp/blob"
        kinds=""
        grep -qaE 'AKIA[0-9A-Z]{16}' "$tmp/blob" && kinds+="aws-access-key "
        grep -qaE 'ghp_[A-Za-z0-9]{36}' "$tmp/blob" && kinds+="github-token "
        grep -qaE -- '-----BEGIN ([A-Z0-9]+ )*PRIVATE KEY-----' "$tmp/blob" && kinds+="private-key "
        [ -z "$kinds" ] || printf '%s\t%s\n' "$blob" "$kinds" >> "$tmp/found"
    done < <(cut -f 1 "$tmp/versions" | sort -u)
    awk -F '\t' '
        FILENAME == ARGV[1] { kinds[$1] = $2; next }
        $1 in kinds {
            n = split(kinds[$1], k, " ")
            for (i = 1; i <= n; i++) printf "%s\t%s\t%s\n", k[i], $2, $3
        }' "$tmp/found" "$tmp/versions" |
        sort -u -t "$TAB" -k2,2 -k1,1 -k3,3 > "$tmp/report"
    cat "$tmp/report"
    [ ! -s "$tmp/report" ]
}

[ "$#" -ge 2 ] || usage
command=$1
repo=$2
shift 2
case "$command" in
    big | secrets) ;;
    *) usage ;;
esac
if [ ! -d "$repo" ] || ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "scrub.sh: not a git repository: $repo" >&2
    exit 2
fi
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
"cmd_$command" "$@"
