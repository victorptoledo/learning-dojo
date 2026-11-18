#!/usr/bin/env bash
# Usage: dupes.sh scan|report|link [options] DIR - finds files with the same content.
set -uo pipefail
export LC_ALL=C

usage() {
    echo "usage: dupes.sh scan|report|link [options] DIR" >&2
    exit 2
}

# SIZE<TAB>INODE<TAB>PATH for every non-empty regular file whose size another file shares.
# Files with a size of their own are never opened.
same_size_files() {
    find "$1" -type f -size +0 -printf '%s\t%i\t%P\n' |
        awk -F'\t' '
            { count[$1]++; line[++n] = $0; size[n] = $1 }
            END { for (i = 1; i <= n; i++) if (count[size[i]] > 1) print line[i] }'
}

# HASH<TAB>SIZE<TAB>INODE<TAB>PATH, sorted by hash and then path.
hashed() {
    local dir=$1 size inode path sum
    while IFS=$'\t' read -r size inode path; do
        sum=$(sha256sum < "$dir/$path") || continue
        printf '%s\t%s\t%s\t%s\n' "${sum%% *}" "$size" "$inode" "$path"
    done | sort -t $'\t' -k1,1 -k4
}

# FIRST<TAB>SIZE<TAB>INODE<TAB>PATH for each file of each group of two or more, where FIRST
# names the group (its first path); sorted by group and then path.
collect() {
    same_size_files "$1" | hashed "$1" |
        awk -F'\t' '
            function flush(   i) { if (n > 1) for (i = 1; i <= n; i++) print first "\t" rec[i]; n = 0 }
            $1 != hash { flush(); hash = $1; first = $4 }
            { rec[++n] = $2 "\t" $3 "\t" $4 }
            END { flush() }' |
        sort -t $'\t' -k1,1 -k4
}

print_scan() {
    awk -F'\t' 'NR > 1 && $1 != prev { print "" } { prev = $1; print $4 }'
}

command=${1:-}
[ "$#" -gt 0 ] && shift
case $command in
    scan) ;;
    *) usage ;;
esac
[ "$#" -eq 1 ] || usage
dir=$1
if [ ! -d "$dir" ]; then
    echo "dupes.sh: not a directory: $dir" >&2
    exit 2
fi

collect "$dir" | print_scan
