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

# The fast-import stream filter. Reads $tmp/purge (one path per line) and $tmp/blobs ("OLD NEW"
# blob ids). Drops the file lines of purged paths, swaps blob ids, drops the commits left
# without changes and points everything that named them at their parent.
write_filter() {
    cat > "$tmp/filter.awk" << 'AWK'
BEGIN {
    while ((getline line < (tmp "/purge")) > 0) purge[line] = 1
    while ((getline line < (tmp "/blobs")) > 0) { split(line, b, " "); blob[b[1]] = b[2] }
}
# next_line: the next line of the stream, or the rest of a line a message ended in.
function next_line() {
    if (pending != "") { line = pending; pending = ""; return 1 }
    return (getline line) > 0
}
# read_data: the line "data N" was read; returns the N bytes that follow.
function read_data(header,    n, need, text) {
    n = substr(header, 6) + 0
    need = n; text = ""
    while (need > 0 && next_line()) {
        if (length(line) + 1 <= need) { text = text line "\n"; need -= length(line) + 1 }
        else { text = text substr(line, 1, need); pending = substr(line, need + 1); need = 0 }
    }
    # A message without a final newline gets one: fast-import allows one LF after the data.
    return header "\n" text (text ~ /\n$/ ? "" : "\n")
}
function purged(path,    p) {
    if (path ~ /^".*"$/) path = substr(path, 2, length(path) - 2)
    for (p in purge) if (path == p || index(path, p "/") == 1) return 1
    return 0
}
# parent: a mark after the commits it named were dropped ("" when there is none).
function parent(ref) {
    while (ref in alias) ref = alias[ref]
    return ref
}
function flush_commit(    i, from, merges, m) {
    from = (commit_from == "") ? "" : parent(commit_from)
    if (ops == 0 && changes > 0 && nmerges == 0) {
        alias[commit_mark] = from
        dropped++
        if (from != "") printf "reset %s\nfrom %s\n\n", commit_ref, from
    } else {
        printf "%s", commit_head
        if (from != "") printf "from %s\n", from
        merges = ""
        for (i = 1; i <= nmerges; i++) {
            m = parent(commit_merge[i])
            if (m != "" && m != from) printf "merge %s\n", m
        }
        printf "%s\n", commit_ops
    }
    in_commit = 0
}
{ pending = ""; line = $0 }
{
    do {
        if (in_commit) {
            if (line == "") { flush_commit(); continue }
            if (line ~ /^from /) { commit_from = substr(line, 6); continue }
            if (line ~ /^merge /) { commit_merge[++nmerges] = substr(line, 7); continue }
            if (line ~ /^M /) {
                changes++
                split(line, f, " ")
                path = substr(line, length(f[1] f[2] f[3]) + 4)
                if (purged(path)) continue
                if (f[3] in blob) f[3] = blob[f[3]]
                commit_ops = commit_ops "M " f[2] " " f[3] " " path "\n"; ops++
                continue
            }
            if (line ~ /^D /) {
                changes++
                if (purged(substr(line, 3))) continue
                commit_ops = commit_ops line "\n"; ops++
                continue
            }
            if (line ~ /^data /) { commit_head = commit_head read_data(line); continue }
            commit_head = commit_head line "\n"
            if (line ~ /^mark /) commit_mark = substr(line, 6)
            continue
        }
        if (line ~ /^commit /) {
            in_commit = 1; commit_ref = substr(line, 8); commit_head = line "\n"; commit_mark = ""
            commit_from = ""; nmerges = 0; commit_ops = ""; ops = 0; changes = 0
            continue
        }
        if (line ~ /^from /) { ref = parent(substr(line, 6)); if (ref != "") print "from " ref; continue }
        if (line ~ /^data /) { printf "%s", read_data(line); continue }
        print line
    } while (next_line())
}
END { if (in_commit) flush_commit() }
AWK
}

# preflight: refuses to rewrite a repository without commits, with uncommitted changes or
# with a backup of an earlier rewrite.
preflight() {
    git -C "$repo" rev-parse --verify --quiet 'HEAD^{commit}' > /dev/null || fail "no commits"
    [ -z "$(git -C "$repo" status --porcelain --untracked-files=no)" ] || fail "uncommitted changes"
    [ -z "$(git -C "$repo" for-each-ref --count=1 refs/scrub-backup)" ] ||
        fail "a backup exists: run undo or expire first"
}

# rewrite: backs up every branch and tag, runs them through the filter, writes the commit map
# and prints what changed.
rewrite() {
    touch "$tmp/purge" "$tmp/blobs"
    write_filter
    git -C "$repo" for-each-ref --format='create refs/scrub-backup/%(refname:lstrip=1) %(objectname)' \
        refs/heads refs/tags | git -C "$repo" update-ref --stdin
    git -C "$repo" fast-export --branches --tags --no-data --signed-tags=strip \
        --tag-of-filtered-object=rewrite --export-marks="$tmp/old-marks" > "$tmp/stream"
    awk -v tmp="$tmp" -f "$tmp/filter.awk" "$tmp/stream" > "$tmp/filtered"
    git -C "$repo" fast-import --quiet --force --export-marks="$tmp/new-marks" < "$tmp/filtered"
    git -C "$repo" reset -q --hard
    mkdir -p "$gitdir/scrub"
    awk -v zeros=0000000000000000000000000000000000000000 '
        FILENAME == ARGV[1] { old[$1] = $2; next }
        { new[$1] = $2 }
        END { for (m in old) print old[m], (m in new) ? new[m] : zeros }' \
        "$tmp/old-marks" "$tmp/new-marks" | sort > "$gitdir/scrub/commit-map"
    awk '
        $2 ~ /^0+$/ { dropped++; next }
        $1 != $2 { rewrote++ }
        END { printf "rewrote %d commits, dropped %d\n", rewrote, dropped }' "$gitdir/scrub/commit-map"
}

# purge PATH...: removes the paths from every commit of every branch and tag.
cmd_purge() {
    [ "$#" -ge 1 ] || usage
    preflight
    printf '%s\n' "$@" > "$tmp/purge"
    rewrite
}

# redact FILE: replaces the strings listed in FILE in every version of every file.
cmd_redact() {
    [ "$#" -eq 1 ] || usage
    local list=$1 blob new count=0
    { tr -d '\r' < "$list" | grep -v '^$' > "$tmp/strings"; } 2> /dev/null || true
    [ -s "$tmp/strings" ] || fail "no strings in $list"
    preflight
    versions > "$tmp/versions"
    : > "$tmp/blobs"
    while IFS= read -r blob; do
        git -C "$repo" cat-file blob "$blob" > "$tmp/blob"
        grep -qaF -f "$tmp/strings" "$tmp/blob" || continue
        # Literal replacement on raw bytes: \Q...\E quotes every special character.
        perl -e '
            open(my $list, "<", $ARGV[0]) or die;
            my @strings = map { chomp; $_ } <$list>;
            local $/;
            binmode STDIN; binmode STDOUT;
            my $data = <STDIN>;
            $data =~ s/\Q$_\E/***REMOVED***/g for @strings;
            print $data;' "$tmp/strings" < "$tmp/blob" > "$tmp/redacted"
        new=$(git -C "$repo" hash-object -w "$tmp/redacted")
        echo "$blob $new" >> "$tmp/blobs"
        count=$((count + 1))
    done < <(cut -f 1 "$tmp/versions" | sort -u)
    echo "redacted $count file versions"
    if [ "$count" -eq 0 ]; then
        echo "rewrote 0 commits, dropped 0"
        return
    fi
    rewrite
}

# undo: every branch and tag back where the backup says, and the working tree with them.
cmd_undo() {
    [ "$#" -eq 0 ] || usage
    [ -n "$(git -C "$repo" for-each-ref --count=1 refs/scrub-backup)" ] || fail "nothing to undo"
    [ -z "$(git -C "$repo" status --porcelain --untracked-files=no)" ] || fail "uncommitted changes"
    git -C "$repo" for-each-ref --format='%(refname:lstrip=2) %(objectname)' refs/scrub-backup > "$tmp/backup"
    awk '{ print "update refs/" $1 " " $2; print "delete refs/scrub-backup/" $1 }' "$tmp/backup" |
        git -C "$repo" update-ref --stdin
    rm -rf "$gitdir/scrub"
    git -C "$repo" reset -q --hard
    echo "restored $(wc -l < "$tmp/backup") refs"
}

# expire: drops the backup, the reflogs and ORIG_HEAD, then prunes what nothing reaches.
cmd_expire() {
    [ "$#" -eq 0 ] || usage
    git -C "$repo" for-each-ref --format='delete %(refname)' refs/scrub-backup |
        git -C "$repo" update-ref --stdin
    rm -rf "$gitdir/scrub"
    git -C "$repo" update-ref -d ORIG_HEAD 2> /dev/null || true
    git -C "$repo" reflog expire --expire=now --expire-unreachable=now --all
    git -C "$repo" gc --quiet --prune=now
    echo "expired the backup and the reflogs"
}

[ "$#" -ge 2 ] || usage
command=$1
repo=$2
shift 2
case "$command" in
    big | secrets | purge | redact | undo | expire) ;;
    *) usage ;;
esac
if [ ! -d "$repo" ] || ! git -C "$repo" rev-parse --git-dir > /dev/null 2>&1; then
    echo "scrub.sh: not a git repository: $repo" >&2
    exit 2
fi
gitdir=$(git -C "$repo" rev-parse --absolute-git-dir)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
"cmd_$command" "$@"
