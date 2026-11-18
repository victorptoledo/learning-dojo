#!/usr/bin/env bash
# Usage: gitx.sh authors|owners|hotspots REPO [ARGS] - reads a repository's history.
set -euo pipefail
export LC_ALL=C

usage() {
    echo "usage: gitx.sh authors|owners|hotspots REPO [ARGS]" >&2
    exit 2
}

# has_commits: true when HEAD points at a commit (a new repository has none yet).
has_commits() {
    git -C "$repo" rev-parse --verify --quiet 'HEAD^{commit}' > /dev/null
}

# authors: commits, lines added and removed per (mailmapped) author, merges left out.
cmd_authors() {
    has_commits || return 0
    git -C "$repo" log --no-merges --format='@%aN' --numstat HEAD |
        awk -F '\t' '
            /^@/ { name = substr($0, 2); commits[name]++; next }
            NF >= 3 {
                if ($1 != "-") added[name] += $1
                if ($2 != "-") removed[name] += $2
            }
            END {
                for (n in commits) printf "%d\t+%d\t-%d\t%s\n", commits[n], added[n], removed[n], n
            }' |
        sort -t "$(printf '\t')" -k1,1nr -k4,4
}

[ "$#" -ge 2 ] || usage
command=$1
repo=$2
shift 2
case "$command" in
    authors) [ "$#" -eq 0 ] || usage ;;
    *) usage ;;
esac
if [ ! -d "$repo" ] || ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "gitx.sh: not a git repository: $repo" >&2
    exit 2
fi
"cmd_$command" "$@"
