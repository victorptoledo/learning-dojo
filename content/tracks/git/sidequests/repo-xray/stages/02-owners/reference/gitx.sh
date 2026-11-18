#!/usr/bin/env bash
# Usage: gitx.sh authors|owners|hotspots REPO [ARGS] - reads a repository's history.
set -euo pipefail
export LC_ALL=C
TAB=$(printf '\t')

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
        sort -t "$TAB" -k1,1nr -k4,4
}

# owners PATH: lines per author in the text files under PATH at HEAD, by git blame.
cmd_owners() {
    local path=$1 empty file files=0 ignore=()
    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    : > "$tmp/files"
    if has_commits; then
        # Text files only: numstat against the empty tree marks binary files with "-".
        empty=$(git -C "$repo" hash-object -t tree /dev/null)
        git -C "$repo" diff -z --numstat "$empty" HEAD -- "$path" |
            awk -v RS='\0' -F '\t' 'NF >= 3 && $1 != "-" { printf "%s%c", $3, 0 }' > "$tmp/files"
        if git -C "$repo" cat-file -e HEAD:.git-blame-ignore-revs 2> /dev/null; then
            git -C "$repo" show HEAD:.git-blame-ignore-revs > "$tmp/ignore"
            ignore=(--ignore-revs-file "$tmp/ignore")
        fi
    fi
    if [ ! -s "$tmp/files" ]; then
        echo "gitx.sh: no files to blame under $path" >&2
        exit 1
    fi
    while IFS= read -r -d '' file; do
        files=$((files + 1))
        git -C "$repo" blame --line-porcelain "${ignore[@]}" HEAD -- "$file"
    done < "$tmp/files" > "$tmp/blame"
    # Each line of the file is one tab-prefixed line, after its author header.
    awk '
        /^author / { author = substr($0, 8); next }
        /^\t/ { count[author]++; total++ }
        END {
            for (a in count) printf "%d\t%d%%\t%s\n", count[a], int((200 * count[a] + total) / (2 * total)), a
        }' "$tmp/blame" | sort -t "$TAB" -k1,1nr -k3,3 > "$tmp/owners"
    cat "$tmp/owners"
    awk -F '\t' -v files="$files" '
        { total += $1; lines[NR] = $1 }
        END {
            printf "total: %d lines in %d files\n", total, files
            factor = 0
            for (i = 1; i <= NR && 2 * sum <= total; i++) { sum += lines[i]; factor = i }
            printf "bus factor: %d\n", factor
        }' "$tmp/owners"
}

[ "$#" -ge 2 ] || usage
command=$1
repo=$2
shift 2
case "$command" in
    authors) [ "$#" -eq 0 ] || usage ;;
    owners) [ "$#" -eq 1 ] || usage ;;
    *) usage ;;
esac
if [ ! -d "$repo" ] || ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "gitx.sh: not a git repository: $repo" >&2
    exit 2
fi
"cmd_$command" "$@"
